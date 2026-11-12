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

## 9. Acesso por troca de usuário, permissões e auditoria (v2)

Decisões confirmadas pelo dono da oficina:

1. **Administradores:** o dono pode criar outros administradores, mas só o próprio dono faz login direto no app. Administradores criados por ele entram pela troca de usuário.
2. **Login de funcionário:** funcionários não fazem login direto. Eles são acessados só pela troca de usuário, a partir da sessão do admin.
3. **Voltar para o admin:** exige a senha do admin, para impedir que um funcionário acesse a conta do admin indevidamente.
4. **Permissões por checkbox:** Clientes, Veículos, Ordens de serviço, Orçamentos, Estoque, Agenda e Financeiro. Equipe, Configurações e gestão de usuários são sempre exclusivos de administradores.
5. **Senha inicial:** o funcionário deve trocar a senha no primeiro uso da troca de usuário.
6. **Orçamentos:** passam a ter API própria, com dados persistidos (não mais locais).
7. **Autoria:** criação de OS, orçamento, lançamentos financeiros e movimentações de estoque registram o usuário que fez a ação. No formulário, o campo vem preenchido com o usuário logado.
8. **Log de ações:** a tela de Usuários terá um relatório de todas as ações feitas no app, com data/hora e nome do usuário.

### 9.1 Fluxo de troca de usuário

- O cabeçalho mostra o usuário ativo e o seu perfil (Administrador ou Funcionário), com um seletor.
- Ao escolher um funcionário, o front pede a senha dele. O back-end valida a senha no Supabase (grant de senha) e devolve a sessão do funcionário.
- A sessão do admin é mantida em memória e no armazenamento local do navegador. Voltar para o admin pede a senha do admin.
- A permissão efetiva é sempre a do usuário ativo, lida do banco a cada requisição. O front só esconde menus e rotas; a API recusa o acesso (403).
- Se o funcionário ainda tiver a senha temporária, o sistema obriga a troca antes de liberar as telas.

### 9.2 Modelo de dados (V6, nova migração)

- Tabela `usuario_permissoes` (`usuario_id`, `modulo`), com `modulo` em: `CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA`, `FINANCEIRO`.
- Coluna `criado_por` (usuário) em `ordens_servico`, `orcamentos` (nova tabela), `pagamentos`, `despesas` e `movimentacoes_estoque`, já existentes ou criadas na V6.
- Tabela `auditoria` (`id`, `oficina_id`, `usuario_id`, `usuario_nome`, `acao`, `entidade`, `entidade_id`, `resumo`, `criado_em`), append-only.
- Nenhuma migração anterior (V1 a V5) é editada.

### 9.3 Back-end

- Autorização por módulo: `@PreAuthorize("@permissoes.pode('OS')")` nos controllers, substituindo o acesso livre dos endpoints operacionais.
- Endpoint de troca: `POST /auth/trocar-usuario` (`usuarioId`, `senha`) para o funcionário; `POST /auth/voltar-admin` (`senha`) para o admin.
- `POST /auth/login` recusa usuários com perfil `FUNCIONARIO` (retorna erro explicando a troca de usuário).
- Auditoria: interceptor/aspecto em toda operação de escrita (POST, PUT, PATCH, DELETE) que grava usuário, data/hora, ação e entidade. Leitura: `GET /auditoria` (somente admin), com filtros por período, usuário e módulo.
- Orçamentos: CRUD completo (itens, descontos, validade, aprovação, conversão em OS), persistido, com `criado_por`.

### 9.4 Front-end

- Cadastro de usuário: campo **Tipo** (Funcionário / Administrador) e checkboxes de permissões (visíveis só para admin).
- Cabeçalho: seletor de usuário, modal de senha para entrar como funcionário e para voltar ao admin.
- Menu e rotas filtrados pelas permissões do usuário ativo.
- Orçamentos: trocar o estado local pelo `api.orcamentos`.
- Campo "Criado por" (somente leitura) preenchido com o usuário logado em OS, orçamento, lançamentos e movimentações.
- Tela de Usuários: aba "Relatório de ações" com filtros e exportação CSV.

### 9.5 Fases de entrega

1. **Fase A:** V6, permissões, `criado_por` e auditoria no back-end.
2. **Fase B:** troca de usuário (back-end e cabeçalho), senha temporária e bloqueio de login direto de funcionário.
3. **Fase C:** API de orçamentos e migração da tela de Orçamentos.
4. **Fase D:** relatório de ações na tela de Usuários e campo "Criado por" nas telas.
5. **Testes:** cada fase com testes de back-end (autorização e auditoria) e teste de ponta a ponta no navegador.

### 9.6 Riscos

| Risco | Mitigação |
| --- | --- |
| Funcionário acessar a conta do admin | Voltar para admin exige senha do admin; API recusa rotas sem permissão |
| Senha do funcionário exposta | A senha é enviada só ao back-end, por HTTPS; nunca é armazenada no navegador |
| Auditoria incompleta | Interceptor único em escritas, com teste para cada módulo |
| Permissões só no front | Verificação obrigatória no back-end (`@PreAuthorize`) |
| Orçamentos locais já existentes | Migração não automática; avisar que orçamentos locais não são importados |

### 9.7 Status de implementação

**Fase A — concluída (sem commit, conforme pedido):**

- migração `V6__permissoes_por_modulo_e_auditoria.sql` validada em PostgreSQL 17 (Flyway e `ddl-auto=validate`);
- `usuario_permissoes`, `usuarios.dono` e `auditoria`;
- `@PreAuthorize` por módulo em todos os controllers operacionais;
- auditoria automática de escritas via `AuditoriaInterceptor`;
- `GET /auditoria` com filtros (ADMIN);
- `POST/PUT /usuarios` com `perfil` e `permissoes`;
- `GET /auth/me` e `GET /usuarios` com `dono` e `permissoes`;
- testes: 24 no total, incluindo `PermissaoServiceTest`, `AuditoriaInterceptorTest` e regras do dono em `UsuarioServiceTest`;
- validação funcional local: ADMIN acessa tudo; FUNCIONARIO com apenas `CLIENTES` recebe `403` em veículos, OS, financeiro, equipe e auditoria; escrita gera registro de auditoria com nome do usuário.

**Fase B — concluída (sem commit, conforme pedido):**

- `POST /auth/login` restrito ao dono da oficina;
- `POST /auth/trocar-usuario`, `POST /auth/validar-senha` e `POST /auth/alterar-senha`;
- cabeçalho com seletor de usuários, pedido de senha para entrar e voltar, e troca obrigatória de senha no primeiro uso;
- cadastro/edição com tipo (Funcionário/Administrador) e checkboxes de permissões;
- menu e rotas filtrados pelas permissões do usuário ativo;
- build do front validado (`vite build` e `eslint` sem erros).

**Fase C — concluída (sem commit, conforme pedido):**

- migração `V7__orcamentos_campos_comerciais.sql`;
- `OrcamentoController` com CRUD, status e conversão;
- front `useOrcamentosState` consumindo `api.orcamentos` em modo real;
- build do front validado.

**Fase D — concluída (sem commit, conforme pedido):**

- aba "Relatório de ações" em `/equipe` com filtros e exportação CSV;
- OS exibe o operador de abertura a partir de `criadoPorNome`;
- orçamentos exibem o responsável a partir de `criadoPorNome`;
- build do front validado.

Todas as fases A a D foram implementadas. O módulo de orçamentos permanece com aprovação pública por token e timeline locais como previsto.

---

## 10. Próximos passos (histórico v1)

1. Implementar a V5 e o back-end de usuários.
2. Implementar a tela de Equipe e as guardas no front.
3. Integrar o dropdown de mecânicos e esconder o financeiro.
4. Atacar os mocks P1: boxes, vistoria, notificações e configurações da oficina.
