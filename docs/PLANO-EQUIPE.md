# Plano — Cadastro de equipe e permissões

Status: **em implementação**  
Escopo desta entrega: migração `V5`, cadastro de funcionários, perfis de acesso e filtro de mecânicos.  
Fora do escopo imediato: orçamentos, dashboard agregado, boxes, upload de fotos e telas de Agenda/Financeiro.

---

## 1. Objetivo

Permitir que o administrador (dono da oficina), já autenticado no sistema, cadastre os funcionários da oficina.

Regras de produto:

- o funcionário é criado dentro da oficina do administrador;
- funcionários que atuam como mecânicos aparecem em **“Mecânico responsável”** na criação/edição de OS;
- funcionários têm acesso operacional, mas **não** acessam Financeiro, Equipe, Configurações nem relatórios financeiros;
- o administrador continua com acesso total.

---

## 2. Modelo de dados

### 2.1 Perfil de acesso x função operacional

Para não misturar permissão com cargo exibido:

| Campo | Valores | Função |
| --- | --- | --- |
| `perfil` | `ADMIN`, `FUNCIONARIO` | Define o que o usuário pode acessar |
| `executa_os` | `true`, `false` | Define se aparece como mecânico responsável na OS |
| `cargo` | texto livre | Função exibida: Mecânico, Eletricista, Atendente etc. |

Na v1, novos usuários sempre nascem como `FUNCIONARIO`. O perfil `ADMIN` fica restrito ao dono da conta. Se no futuro houver gerente/atendente com menus diferentes, o enum pode evoluir em uma nova migração.

### 2.2 Migração V5

Arquivo: `src/main/resources/db/migration/V5__cadastro_equipe_e_permissoes.sql`

```sql
alter table public.usuarios
  add column if not exists perfil varchar(20) not null default 'FUNCIONARIO',
  add column if not exists executa_os boolean not null default true,
  add column if not exists deve_trocar_senha boolean not null default false;

update public.usuarios
   set perfil = 'ADMIN'
 where upper(btrim(cargo)) in ('ADMIN', 'DONO', 'PROPRIETARIO');

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'usuarios_perfil_chk'
       and conrelid = 'public.usuarios'::regclass
  ) then
    alter table public.usuarios
      add constraint usuarios_perfil_chk
      check (perfil in ('ADMIN', 'FUNCIONARIO'));
  end if;
end $$;

create index if not exists ix_usuarios_oficina_perfil_ativo
  on public.usuarios (oficina_id, perfil, ativo);

create index if not exists ix_usuarios_oficina_executa_os
  on public.usuarios (oficina_id, executa_os, ativo);
```

Observações:

- `executa_os` nasce `true` para preservar os usuários existentes no dropdown de mecânicos.
- O e-mail continua único globalmente (`ux_usuarios_email`). Um funcionário não pode participar de duas oficinas na v1.
- `deve_trocar_senha` prepara o fluxo de senha temporária.

---

## 3. Matriz de acesso

| Área | ADMIN | FUNCIONARIO |
| --- | --- | --- |
| Dashboard | completo | sem o card financeiro |
| Agenda | sim | sim |
| Clientes | sim | sim |
| Veículos | sim | sim |
| OS | sim | sim |
| Orçamentos | sim | sim (sem indicadores financeiros, se desejado) |
| Estoque | sim, com custo/margem | operacional; ocultar custo/margem se desejado |
| Financeiro | **sim** | **não** |
| Equipe / Configurações | **sim** | **não** |
| Relatórios financeiros | **sim** | **não** |

A restrição é aplicada no back-end. Esconder o item do menu não é suficiente.

---

## 4. Back-end

### 4.1 Endpoints

| Método | Rota | Autorização | Função |
| --- | --- | --- | --- |
| `GET` | `/usuarios?busca=&ativo=&executaOs=` | ADMIN | Lista a equipe da oficina |
| `POST` | `/usuarios` | ADMIN | Cria funcionário no Supabase Auth + `usuarios` |
| `GET` | `/usuarios/{id}` | ADMIN ou o próprio | Detalhe |
| `PUT` | `/usuarios/{id}` | ADMIN | Edita dados operacionais |
| `PATCH` | `/usuarios/{id}/status` | ADMIN | Ativa/desativa |
| `POST` | `/usuarios/{id}/reset-senha` | ADMIN | Gera senha temporária |
| `GET` | `/mecanicos` | Autenticado | Ativos com `executa_os = true` |

O `GET /auth/me` passa a devolver `perfil`, `executaOs` e `ativo`.

### 4.2 Criação no Supabase Auth

O cadastro de funcionário exige a chave de serviço do Supabase, que **nunca** vai para o front:

```properties
salvo.security.supabase.service-role-key=${SUPABASE_SERVICE_ROLE_KEY:}
```

Fluxo do `POST /usuarios`:

1. valida se quem chama é `ADMIN`;
2. valida e-mail único no banco;
3. cria o usuário no Supabase Auth via Admin API (`/admin/users`);
4. insere em `usuarios` com `oficina_id` do administrador e `auth_user_id`;
5. se o insert falhar, exclui o usuário do Supabase (compensação);
6. devolve o usuário criado e, quando aplicável, a senha temporária uma única vez.

Dois modos de primeiro acesso:

- **v1:** senha inicial definida/gerada pelo sistema, com `deve_trocar_senha = true`;
- **fase 2:** convite por e-mail via `/invite` do Supabase.

### 4.3 Autorização

- `@EnableMethodSecurity` no `SecurityConfig`.
- Bean `PermissaoService` (`@permissoes`) com `isAdmin`, `isFuncionario`, `isSelf`.
- `@PreAuthorize("@permissoes.isAdmin()")` em `UsuarioController`.
- `@PreAuthorize("@permissoes.isAdmin()")` em `FinanceiroController`.
- `OficinaContexto` rejeita usuário com `ativo = false`.
- Proteções no `UsuarioService`:
  - não desativar a si mesmo;
  - não desativar o último ADMIN ativo;
  - não alterar usuário de outra oficina;
  - não permitir que funcionário altere o próprio perfil.

### 4.4 Mecânicos

`GET /mecanicos` deixa de devolver todos os usuários ativos e passa a devolver apenas:

```text
oficina_id = oficina do usuário logado
ativo = true
executa_os = true
```

Também deixa de expor e-mail e comissão de todos os usuários; o payload passa a ser um `MecanicoDTO` enxuto.

---

## 5. Front-end

1. `AuthProvider` passa a mapear `perfil`, `executaOs` e `isAdmin`.
2. `navigation.js` ganha `adminOnly` em Financeiro, Configurações e Equipe.
3. `Aside` filtra itens administrativos.
4. Criar `RequireAdmin` para `/equipe`, `/financeiro` e `/configuracoes`.
5. Criar a tela `/equipe` com lista, busca, filtro, criação, edição, ativação/desativação e redefinição de senha.
6. Criar `api.usuarios`.
7. Após criar/editar, recarregar `/mecanicos` para o select da OS.
8. Corrigir `useOficinaState`: lista vazia da API não pode voltar para os mocks.
9. Esconder o card financeiro do Dashboard para funcionário.
10. Em modo demo, a tela de equipe fica somente leitura ou simulada.

---

## 6. Testes

Back-end:

- ADMIN cria funcionário e o Supabase é chamado;
- funcionário recebe 403 em `POST /usuarios` e em `/pagamentos`;
- usuário inativo recebe 403;
- e-mail duplicado retorna erro claro;
- falha no insert compensa excluindo o usuário do Auth;
- não é possível desativar o último ADMIN;
- `/mecanicos` filtra `executa_os = false`;
- lista vazia de mecânicos não cai no mock.

Front-end:

- `npm run lint` e `npm run build`;
- menu administrativo oculto para funcionário;
- acesso direto às rotas administrativas bloqueado;
- funcionário criado aparece no select de mecânico;
- funcionário desativado desaparece do select.

Testes não devem criar registros no banco de produção.

---

## 7. Ordem de deploy

1. Configurar `SUPABASE_SERVICE_ROLE_KEY` no Render.
2. Aplicar a V5 no Supabase.
3. Deploy do back-end.
4. Deploy do front-end.
5. Smoke test com ADMIN e FUNCIONARIO.

A V5 é compatível com o front antigo: os defaults preservam o comportamento atual.

---

## 8. Riscos e decisões

| Risco | Mitigação |
| --- | --- |
| Chave de serviço exposta | Manter apenas no back-end, como secret; nunca no front |
| Funcionário acessando Financeiro pela API | `@PreAuthorize` no controller + testes |
| E-mail já usado em outra oficina | Rejeitar com mensagem clara na v1 |
| Desativar o último ADMIN | Bloquear no service |
| Sessão de usuário desativado | `OficinaContexto` valida `ativo`; banimento no Supabase é opcional |
| Lista vazia de mecânicos cair no mock | Corrigir `useOficinaState` e aceitar `[]` |

---

## 9. Próximos passos

1. Implementar a V5 e o back-end de usuários.
2. Implementar a tela de Equipe e as guardas no front.
3. Integrar o dropdown de mecânicos e esconder o financeiro.
4. Atacar os mocks P1: boxes, vistoria, notificações e configurações da oficina.
