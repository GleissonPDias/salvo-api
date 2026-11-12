# Banco de dados e migrações — Salvô API

O schema é versionado com **Flyway** e validado pelo Hibernate no start (`ddl-auto=validate`). Este documento descreve as migrações, as regras que vivem no PostgreSQL e como validar tudo localmente.

> **Regra de ouro:** migrações já aplicadas (`V1` a `V5`) **não são editadas**. Toda mudança nova entra em uma nova versão (`V6`, `V7`...). A entrega atual adicionou a `V6`.

---

## Sumário

- [Visão das migrações](#visão-das-migrações)
- [V1 — baseline](#v1--baseline)
- [V2 — correções](#v2--correções)
- [V3 — numeração de OS](#v3--numeração-de-os)
- [V4 — SaaS multi-oficina](#v4--saas-multi-oficina)
- [V5 — equipe e permissões](#v5--equipe-e-permissões)
- [V6 — permissões por módulo e auditoria](#v6--permissões-por-módulo-e-auditoria)
- [Regras no banco](#regras-no-banco)
- [RLS e segurança](#rls-e-segurança)
- [Validação local](#validação-local)
- [Cuidados conhecidos](#cuidados-conhecidos)
- [Como criar uma nova migração](#como-criar-uma-nova-migração)

---

## Visão das migrações

| Versão | Arquivo | Papel |
| --- | --- | --- |
| V1 | `V1__baseline.sql` | Schema base do sistema |
| V2 | `V2__corrigir_baseline.sql` | Corrige nomes de colunas/triggers do schema legado |
| V3 | `V3__numeracao_sequencial_os.sql` | Numeração sequencial e imutável de OS por oficina |
| V4 | `V4__saas_multi_oficina_features.sql` | Multi-oficina + funcionalidades usadas pelo front |
| V5 | `V5__cadastro_equipe_e_permissoes.sql` | Perfil de acesso, flag de mecânico e senha temporária |
| V6 | `V6__permissoes_por_modulo_e_auditoria.sql` | Permissões por módulo, dono da oficina e auditoria |

As migrações são **idempotentes** e usam `if not exists`, `do $$ ... $$` e `on conflict` para poderem rodar em bancos que já têm parte do schema.

---

## V1 — baseline

Cria os tipos enumerados e as tabelas centrais:

**Tipos**

- `prioridade_os`: `BAIXA`, `NORMAL`, `ALTA`, `URGENTE`
- `status_checklist`: `NAO_VERIFICADO`, `CONFORME`, `NAO_CONFORME`, `NAO_APLICAVEL`
- `status_orcamento`: `RASCUNHO`, `ENVIADO`, `APROVADO`, `REJEITADO`, `CONVERTIDO`, `CANCELADO`
- `status_aprovacao`: `PENDENTE`, `APROVADO`, `REJEITADO`
- `tipo_orcamento_item`: `PECA`, `SERVICO`

**Tabelas**

`oficinas`, `usuarios`, `clientes`, `enderecos_clientes`, `veiculos`, `boxes`, `status_ordens_servico`, `servicos`, `pecas`, `ordens_servico`, `os_servicos`, `os_pecas`, `os_mecanicos`, `os_historico`, `checklist_itens`, `os_checklist`, `orcamentos`, `orcamento_itens`, `orcamento_aprovacoes`, `estoque_movimentacoes`.

Pontos de atenção:

- `veiculos` nasce com unicidade **global** de `placa` e `chassi` (a V4 muda para por oficina).
- `oficinas` nasce com `uf varchar(3)`, que a V4 normaliza para `varchar(2)` para bater com a entidade JPA.
- `estoque_movimentacoes.tipo` e `orcamento_itens.tipo` podem ser enums nativos dependendo do banco de origem.

---

## V2 — correções

- Padroniza colunas de auditoria para `criado_em` / `atualizado_em`.
- Corrige o trigger de `oficinas`, que referenciava uma coluna inexistente.
- Consolida a função `fn_atualiza_atualizado_em()` e recria um trigger por tabela.
- Ajusta constraints declaradas inline na V1.

### Caveat em banco vazio

A V2 versionada **não é 100% compatível com um banco criado do zero**. Em PostgreSQL limpo, a remoção do tipo `tipo_orcamento_item` (seção 5 da V2) falha com:

```
cannot drop type tipo_orcamento_item because other objects depend on it
```

Isso ocorre porque a V1 cria `orcamento_itens` usando o enum e a V2 tenta removê-lo (`drop type if exists tipo_orcamento_item`) enquanto a tabela ainda depende dele. **Não edite a V2 versionada** (bancos de produção já a aplicaram). Para validação local, use uma cópia corrigida da migração (ver [Validação local](#validação-local)).

---

## V3 — numeração de OS

- Cria `oficina_sequencias (oficina_id, entidade, ultimo_numero)`.
- Alinha o contador com o maior número já existente por oficina.
- Cria `fn_proximo_numero_ordem_servico(p_oficina)` com alocação atômica:

```sql
insert into oficina_sequencias (oficina_id, entidade, ultimo_numero)
values (p_oficina, 'ordem_servico', 1)
on conflict (oficina_id, entidade)
do update set ultimo_numero = oficina_sequencias.ultimo_numero + 1
returning ultimo_numero;
```

- Cria `fn_valida_numero_ordem_servico()` e o trigger `trg_ordens_servico_numero_imutavel` para impedir alteração do número depois de criado.

O número é **queimado**: se a OS for excluída, o próximo não reutiliza o número. Isso evita documentos impressos com o mesmo número.

---

## V4 — SaaS multi-oficina

Arquivo: `src/main/resources/db/migration/V4__saas_multi_oficina_features.sql`

### 1. Veículos por oficina

- Adiciona `veiculos.oficina_id`.
- Faz backfill a partir de `clientes.oficina_id`.
- Cria FK e índice.
- Cria `fn_preenche_veiculo_oficina()` + trigger `trg_veiculos_oficina` para manter o vínculo coerente mesmo com código antigo.
- Torna `oficina_id` `not null`.
- Remove unicidade global de placa/chassi.
- Cria índices únicos por oficina:
  - `ux_veiculos_oficina_placa` em `(oficina_id, upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g')))`
  - `ux_veiculos_oficina_chassi` em `(oficina_id, upper(btrim(chassi)))` quando `chassi` não é nulo.

Se já existirem duplicidades, o índice correspondente **não é criado** e a migração emite `raise notice`, em vez de falhar.

### 2. Clientes e endereços

- Adiciona campos usados pelo front (whatsapp, aceita comunicações, observações, classificação).
- Cria/ajusta `enderecos_clientes`.
- Cria unicidade de CPF/CNPJ por oficina.

### 3. Oficinas

- Adiciona dados de impressão/configuração (nome fantasia, endereço, telefone, e-mail, logo etc.).
- Normaliza `uf` para `varchar(2)`, alinhando com `Oficina.uf` (`@Column(length = 2)`).

### 4. Usuários

- Adiciona `auth_user_id`, especialidades, liderança, comissão, cor de agenda e foto.
- Cria unicidade de e-mail por oficina/usuário.

### 5. Boxes

- Adiciona campos de configuração e unicidade de nome por oficina.

### 6. Serviços (catálogo)

- Adiciona categoria, detalhe, tempo estimado, garantia e preço base (quando ausentes).
- Cria `ux_servicos_oficina_codigo`.

### 7. Fornecedores e peças

- Cria `fornecedores` com unicidade de nome e CNPJ por oficina.
- Adiciona em `pecas`: OEM, categoria, aplicação, unidade, estoque máximo, localização, fornecedor, observação, estoque reservado, código de barras, garantia e NCM.
- Cria `estoque_disponivel` como coluna gerada:

```sql
estoque_disponivel numeric generated always as (estoque_atual - estoque_reservado) stored
```

- Cria índices únicos por oficina para código e código de barras.

### 8. Checklist

- Adiciona `checklist_itens.codigo` e gera códigos a partir do nome.
- Resolve colisões de código anexando o `id`.
- Cria `ux_checklist_itens_oficina_codigo`.
- Semeia os 12 itens padrão para todas as oficinas existentes.
- Adiciona `criado_em`/`atualizado_em` em `os_checklist`.
- Remove duplicidades antigas e cria `ux_os_checklist_os_item`.

Mapeamento usado pelo backend:

| Front | Banco (`status_checklist`) |
| --- | --- |
| `ok` | `CONFORME` |
| `atencao` | `NAO_CONFORME` (observação "Atenção") |
| `avaria` | `NAO_CONFORME` (observação "Avaria") |
| `nao_aplicavel` | `NAO_APLICAVEL` |

### 9. Ordens de serviço

Adiciona ao cabeçalho:

`tipo`, `canal`, `titulo`, `relato_cliente`, `observacoes_internas`, `observacoes_cliente`, `previsao_entrega`, `nivel_combustivel` (0–4), `desconto_tipo` (`VALOR`/`PERCENTUAL`), `km_saida`, `diagnostico_validado`, `pronta_em`, `entregue_em`, `cancelada_em`, `motivo_cancelamento`, `forma_pagamento`, `parcelas`, `aprovacao_status`, `aprovacao_canal`, `aprovacao_enviada_em`, `aprovacao_decidida_em`, `aprovacao_visualizada_em`, `garantia_tipo`, `atualizado_por`.

Cria índices para status, previsão e tipo.

### 10. Itens e histórico da OS

- `os_servicos`: `concluido`, `concluido_em`, `desconto`, `desconto_tipo`, `atualizado_em`.
- `os_pecas`: `cotada`, `instalada`, `instalada_em`, `desconto`, `desconto_tipo`, `unidade`, `atualizado_em`.
- `os_historico`: `tipo_evento`, `metadata jsonb`.
- Remove vínculos repetidos em `os_mecanicos` e cria `ux_os_mecanicos_os_usuario`.

### 11. Status de OS

```sql
insert into status_ordens_servico (nome, ordem, ativo) values
  ('PRONTA',    8, true),
  ('ENTREGUE', 10, true);

update status_ordens_servico set ordem = 9,  ativo = false where nome = 'CONCLUIDA';
update status_ordens_servico set ordem = 11               where nome = 'CANCELADA';
```

### 12. Orçamentos

- Adiciona campos usados pelo front e reforça a numeração sequencial por oficina.
- Cria `fn_proximo_numero_orcamento`, `fn_valida_numero_documento` e o trigger `trg_orcamentos_numero_imutavel`.

### 13. Vistoria de entrada

- `os_vistorias` (1 por OS, `ux_os_vistorias_os`).
- `os_vistoria_danos` (área + tipo únicos por vistoria).
- `os_vistoria_fotos` (URL obrigatória, `storage_path` opcional).

### 14. Estoque: pedidos de compra e movimentações

- `pedidos_compra` + `pedido_compra_itens`.
- Sequência por oficina via `fn_proximo_numero_pedido_compra`.
- Trigger `trg_pedidos_compra_numero_imutavel`.
- `estoque_movimentacoes` ganha `oficina_id`, `direcao`, `saldo_anterior`, `saldo_posterior`, `custo_unitario`, `documento`, `pedido_compra_id`, `fornecedor_id`.
- Backfill de `direcao` para movimentações antigas:

```sql
update estoque_movimentacoes
   set direcao = case
     when tipo::text ilike '%SAIDA%' then -1
     when tipo::text ilike '%ENTRADA%' or tipo::text ilike '%DEVOLU%' then 1
     else null
   end
 where direcao is null;
```

- **Trigger de estoque** `trg_estoque_movimentacoes_aplica` (`BEFORE INSERT`):

```sql
select p.estoque_atual, p.oficina_id
  into v_saldo, v_oficina
  from pecas p
 where p.id = new.peca_id
 for update;

if v_oficina is distinct from new.oficina_id then
  raise exception 'peca % pertence a oficina %, nao a %', ...;
end if;

v_novo_saldo := v_saldo + new.direcao * new.quantidade;

if v_novo_saldo < 0 then
  raise exception 'estoque insuficiente ...';
end if;

new.saldo_anterior := v_saldo;
new.saldo_posterior := v_novo_saldo;
update pecas set estoque_atual = v_novo_saldo where id = new.peca_id;
```

Consequências:

- `direcao` é obrigatória (`1` entrada, `-1` saída).
- O saldo nunca fica negativo.
- A peça precisa pertencer à mesma oficina da movimentação.
- A peça é travada com `FOR UPDATE`, evitando corrida entre movimentações simultâneas.

### 15. Financeiro

- `pagamentos` (contas a receber): status `PENDENTE`, `PAGO`, `CANCELADO`, `ESTORNADO`.
- `despesas` (contas a pagar): status `PENDENTE`, `PAGO`, `CANCELADO`.
- Índices por oficina/status/vencimento e vínculos com OS, orçamento, cliente e fornecedor.

### 16. Agenda

- `agendamentos` com cliente, veículo, serviço, mecânico, box e OS.
- `check (fim > inicio)`.
- Status: `AGENDADO`, `CONFIRMADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`, `NAO_COMPARECEU`.

### 17. Notificações

- `notificacoes` por oficina/usuário, com `lida`, `lida_em`, `link` e `tipo`.

### 18. Triggers de `atualizado_em`

Percorre todas as tabelas de `public` que têm `atualizado_em` e recria `trg_<tabela>_updated_at` chamando `fn_atualiza_atualizado_em()`.

### 19. RLS

Ver [RLS e segurança](#rls-e-segurança).

### 20. Comentários

Comenta as tabelas novas para facilitar consultas no Supabase.

---

## V5 — equipe e permissões

Arquivo: `src/main/resources/db/migration/V5__cadastro_equipe_e_permissoes.sql`

Cadastro de funcionários pelo administrador e separação entre **perfil de acesso** e **função operacional**.

### Colunas novas em `usuarios`

| Coluna | Tipo | Default | Papel |
| --- | --- | --- | --- |
| `perfil` | `varchar(20)` | `FUNCIONARIO` | `ADMIN` ou `FUNCIONARIO` |
| `executa_os` | `boolean` | `true` | Aparece como mecânico responsável na OS |
| `deve_trocar_senha` | `boolean` | `false` | Senha temporária gerada pelo admin |

O `cargo` continua sendo texto livre para exibição (Mecânico, Atendente etc.) e não define permissão.

### Backfill

```sql
update public.usuarios
   set perfil = 'ADMIN'
 where upper(btrim(cargo)) in ('ADMIN', 'DONO', 'PROPRIETARIO');
```

Quem já era dono da conta permanece `ADMIN`. Os demais viram `FUNCIONARIO`. `executa_os` nasce `true` para preservar os usuários existentes no dropdown de mecânicos.

### Constraint e índices

- `usuarios_perfil_chk`: garante `perfil in ('ADMIN', 'FUNCIONARIO')`.
- `ix_usuarios_oficina_perfil_ativo` em `(oficina_id, perfil, ativo)`.
- `ix_usuarios_oficina_executa_os` em `(oficina_id, executa_os, ativo)`.

### Compatibilidade

A V5 é **aditiva e idempotente**. O front antigo continua funcionando: os defaults preservam o comportamento anterior. A autorização por perfil é aplicada na API (`@PreAuthorize` + `PermissaoService`), não no banco.

> A identidade do funcionário é criada no **Supabase Auth** pela Admin API. A V5 não altera o schema `auth`; apenas grava o `auth_user_id` em `usuarios`.

---

## V6 — permissões por módulo e auditoria

Arquivo: `src/main/resources/db/migration/V6__permissoes_por_modulo_e_auditoria.sql`

Libera módulos operacionais por funcionário, marca o dono da oficina e cria a trilha de auditoria.

### `usuarios.dono`

- `dono boolean not null default false`;
- índice único parcial `ux_usuarios_oficina_dono` garante um dono por oficina;
- backfill marca o `ADMIN` mais antigo de cada oficina que ainda não tem dono;
- o dono é o único que faz login direto no app e só ele gerencia a própria conta.

### `usuario_permissoes`

| Coluna | Tipo | Papel |
| --- | --- | --- |
| `id` | `bigint identity` | Chave |
| `usuario_id` | `bigint` | Referência a `usuarios` (`on delete cascade`) |
| `modulo` | `varchar(30)` | `CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA` ou `FINANCEIRO` |
| `criado_em` | `timestamptz` | Data da concessão |

- `ux_usuario_permissoes_usuario_modulo`: impede módulo repetido para o mesmo usuário;
- `usuario_permissoes_modulo_chk`: limita os valores aceitos;
- administradores não precisam de linhas: o `PermissaoService` libera todos os módulos por perfil.

### `auditoria`

- `oficina_id`, `usuario_id`, `usuario_nome`, `acao`, `modulo`, `entidade`, `entidade_id`, `metodo`, `caminho`, `status_http`, `resumo` e `criado_em`;
- `usuario_nome` é copiado no momento da ação para preservar o histórico;
- índices por oficina/data, usuário/data e módulo/data;
- escrita feita pelo `AuditoriaInterceptor` em transação própria (`REQUIRES_NEW`); falha na auditoria não derruba a operação.

### Compatibilidade

A V6 é **aditiva e idempotente**. Nenhuma migração anterior foi alterada. O schema continua compatível com o front antigo; o bloqueio de rotas passou a considerar as permissões por módulo.

---

## Regras no banco

| Regra | Onde | Efeito |
| --- | --- | --- |
| Numeração de OS por oficina | V3 | Sequencial, atômica e imutável |
| Numeração de orçamento | V4 | Sequencial e imutável |
| Numeração de pedido de compra | V4 | Sequencial e imutável |
| Saldo de estoque | V4 trigger | Aplica direção, trava a peça e rejeita saldo negativo |
| `estoque_disponivel` | V4 coluna gerada | Sempre `estoque_atual - estoque_reservado` |
| Placa/chassi por oficina | V4 índice único | Normaliza placa e chassi |
| Código de serviço/peça por oficina | V4 índice único | Evita duplicidade no catálogo |
| Vistoria única por OS | V4 índice único | Uma vistoria por OS |
| Período de agendamento | V4 check | `fim > inicio` |
| Perfil de acesso | V5 check | `perfil` limitado a `ADMIN`/`FUNCIONARIO` |
| `atualizado_em` | V2/V4 triggers | Atualizado em todo `UPDATE` |

---

## RLS e segurança

O front **não acessa o banco diretamente**; todas as operações passam pela API Spring, que usa o papel dono das tabelas (`postgres` ou equivalente). Por isso a estratégia é:

1. Habilitar RLS em todas as tabelas de `public`.
2. Criar uma policy permissiva **somente para o papel da API** (`current_user`):

```sql
create policy api_role_full_access on public.<tabela>
  for all to <current_user>
  using (true) with check (true);
```

3. Revogar todos os grants de `anon` e `authenticated`, inclusive default privileges.

Se um dia o front for falar direto com o Supabase, será necessário criar policies por oficina usando `usuarios.auth_user_id = auth.uid()`.

### Deadlock do Flyway

O loop que habilita RLS **ignora `flyway_schema_history`**. Essa tabela já está travada pelo advisory lock da própria migração; um `ALTER TABLE` nela causaria deadlock e a migração nunca terminaria.

```sql
where schemaname = 'public'
  and tablename <> 'flyway_schema_history'
```

Mantenha essa exclusão em qualquer migração futura que faça loop de RLS.

---

## Validação local

A entrega foi validada em um PostgreSQL 17.11 limpo, com Flyway aplicando V1→V5 em aproximadamente 0,7 s, seguido de `Started SalvoApiApplication` e `ddl-auto=validate` sem erros.

Como a V2 versionada falha em banco vazio, a validação usa uma **cópia corrigida** das migrações fora do repositório:

```
/tmp/opencode/pg/migrations   # cópia corrigida, somente para teste
/tmp/opencode/pg/validate.sh  # sobe o banco e roda a API
```

Passos conceituais:

1. Subir um PostgreSQL limpo em porta alternativa (ex.: `55432`).
2. Copiar `src/main/resources/db/migration` para o diretório de teste.
3. Corrigir apenas a cópia da V2 para não tentar remover o enum em uso (ou remover a seção 5 só na cópia).
4. Rodar a aplicação apontando para esse banco.
5. Conferir nos logs: migrações aplicadas, aplicação iniciada e schema validado.

Testes de banco executados (com rollback, sem deixar registros):

- trigger de estoque soma/subtrai corretamente;
- saída maior que o saldo é rejeitada;
- `estoque_disponivel` reflete físico − reservado;
- numeração de pedido de compra é sequencial;
- flags de RLS e policy existem nas tabelas;
- `oficinas.uf` tem tamanho 2;
- V4 aplica sobre banco com dados legados;
- V5 aplica sobre banco com dados legados e preserva o admin existente.

**Nunca crie registros de teste no banco de produção do usuário.** Use transações com rollback ou um banco local descartável.

---

## Cuidados conhecidos

### V2 em banco vazio

Já descrito acima. Não altere a V2 versionada; use cópia corrigida apenas para validação local.

### Enums nativos de estoque/orçamento

Em produção, `estoque_movimentacoes.tipo` e `orcamento_itens.tipo` podem ser enums nativos. `CatalogoService` resolve o nome do tipo e o rótulo do enum dinamicamente (`information_schema` + `pg_enum`), então o código funciona tanto com enum quanto com `varchar`.

### Estoque e Hibernate

O trigger altera `pecas.estoque_atual`, mas a entidade `Peca` gerenciada ainda tem o valor antigo em memória. Se o serviço salvasse a entidade depois da movimentação, o Hibernate sobrescreveria o saldo e desfaria o trigger. Por isso `CatalogoService`:

- faz `em.flush()` + `em.refresh(peca)` após inserir a movimentação;
- atualiza **somente** `preco_custo` por SQL nativo nas entradas com custo.

Não “simplifique” isso para `pecaRepository.save(peca)` sem revisar o efeito no saldo.

### Placas duplicadas no legado

Se houver duplicidade de placa/chassi na mesma oficina, o índice único correspondente não é criado e a migração emite aviso. Corrija os dados e crie o índice manualmente em uma migração futura.

### `oficinas.uf`

A V4 normaliza para `varchar(2)`. Se o banco tiver UFs com mais de 2 caracteres, o `ALTER` falha. Verifique antes de aplicar em bases legadas.

---

## Como criar uma nova migração

1. Crie `V6__descricao_curta.sql` (nunca edite V1–V5).
2. Use operações **aditivas e idempotentes**: `if not exists`, `do $$ ... $$`, `on conflict`.
3. Prefira colunas nullable ou com default para não quebrar código antigo durante o deploy.
4. Se adicionar coluna mapeada pela entidade, atualize a entidade e rode com `ddl-auto=validate`.
5. Se criar loop de RLS, exclua `flyway_schema_history`.
6. Teste em banco limpo e em banco com dados.
7. Verifique se a migração aparece em `flyway_schema_history` após o start.
8. Faça o deploy do back-end **antes** de publicar um front que dependa dos campos novos.
