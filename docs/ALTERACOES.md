# Changelog — Salvô API

## Entrega em andamento — Fase D: relatório de ações e autoria

Escopo: relatório de ações na tela de Equipe e exibição do usuário que criou OS (e demais registros) nas telas.

**Front-end**

- Aba "Relatório de ações" em `/equipe` com filtros por usuário, módulo e período, listagem e exportação CSV via `GET /auditoria`.
- OS exibe o operador que abriu a ordem a partir de `criadoPorNome` retornado pela API.
- Orçamentos exibem o responsável (criador) a partir de `criadoPorNome`.

**Back-end**

- Nenhuma mudança de schema; reutiliza `auditoria` e os campos `criadoPor`/`criadoPorNome` já expostos.

---

## Entrega em andamento — Fase C: API de orçamentos

Escopo: orçamentos persistidos no back-end, com itens, status e conversão em OS. A tela de Orçamentos passou a usar a API em modo real (demonstração mantém os dados locais).

**Migração:** `V7__orcamentos_campos_comerciais.sql` (V1–V6 não foram editadas).

### Adicionado

- Entidades `Orcamento`, `OrcamentoItem`, repositórios e `OrcamentoService`.
- `OrcamentoController` (`/orcamentos`) com `GET`, `GET/{id}`, `POST`, `PUT/{id}`, `DELETE/{id}`, `PATCH/{id}/status` e `POST/{id}/converter`, protegido por `ORCAMENTOS`.
- DTOs `OrcamentoDTO`, `CriarOrcamentoDTO`, `AtualizarOrcamentoDTO` e `OrcamentoItemDTO`.
- Colunas `titulo`, `tipo`, `validade_dias`, `desconto_tipo` e `canal_envio` em `orcamentos`.

### Alterado

- `useOrcamentosState` (front) consome `api.orcamentos` em modo real: listagem, criação, edição, exclusão, envio, aprovação, rejeição, conversão e duplicação.

### Observações

- Aprovação pública por token e eventos de timeline permanecem locais por enquanto.
- Nenhuma migração anterior foi alterada.

---

## Entrega em andamento — Fase B: troca de usuário e senha temporária

Escopo: entrada direta restrita ao dono da oficina, troca de usuário com senha, validação de senha para voltar ao admin, troca obrigatória de senha no primeiro uso e tela de cadastro com tipo e permissões no front.

**Back-end**

- `POST /auth/login` agora rejeita usuários que não sejam o dono da oficina (`422`), com mensagem explicando a troca de usuário.
- `POST /auth/trocar-usuario` (ADMIN): valida a senha do funcionário e devolve o token dele.
- `POST /auth/validar-senha`: valida a senha de um administrador (usado para voltar ao admin).
- `POST /auth/alterar-senha`: altera a senha do usuário logado e limpa `deveTrocarSenha`.
- `UsuarioRepository.findByEmailIgnoreCase`.

**Front-end**

- Cabeçalho com seletor de usuários da oficina, pedido de senha para entrar e para voltar ao admin, e modal de troca obrigatória de senha no primeiro uso.
- Cadastro/edição de usuário com tipo (Funcionário/Administrador) e checkboxes de permissões.
- Menu e rotas filtrados pelas permissões do usuário ativo (além de `adminOnly`).

A autorização e a auditoria continuam sendo validadas no back-end.

---

## Entrega em andamento — Fase A: permissões por módulo e auditoria (V6)

Escopo: autorização por módulo no back-end, marcação do dono da oficina e trilha de auditoria das ações. A troca de usuário no cabeçalho e o bloqueio de login direto de funcionário ficam para a Fase B.

**Migração:** `V6__permissoes_por_modulo_e_auditoria.sql` (V1–V5 não foram editadas).

### Adicionado

- Tabela `usuario_permissoes` (`usuario_id`, `modulo`), com módulos `CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA` e `FINANCEIRO`.
- Coluna `usuarios.dono`, com índice único por oficina. Em bancos existentes, o administrador mais antigo de cada oficina é marcado como dono.
- Tabela `auditoria` (append-only) e endpoint `GET /auditoria` com filtros por usuário, módulo e período (somente ADMIN).
- `AuditoriaInterceptor`: registra toda escrita concluída com sucesso, com usuário, data/hora, ação, módulo, entidade e caminho.
- Enum `ModuloPermissao` e entidade/repositório `UsuarioPermissao`.
- Testes `PermissaoServiceTest`, `AuditoriaInterceptorTest` e novos casos em `UsuarioServiceTest`.

### Alterado

- `@PreAuthorize` por módulo nos controllers de clientes, veículos, catálogo/estoque, ordens de serviço, vistoria, agenda, financeiro, fornecedores e pedidos de compra. Antes, funcionários tinham acesso operacional livre.
- `FinanceiroController` deixa de ser `admin-only` e passa a exigir a permissão `FINANCEIRO`.
- `POST /usuarios` e `PUT /usuarios/{id}` aceitam `perfil` (`ADMIN`/`FUNCIONARIO`) e `permissoes`.
- Somente o dono pode alterar, desativar ou redefinir a senha da própria conta de dono; o perfil do dono não pode ser rebaixado.
- `GET /auth/me` e `GET /usuarios` passam a devolver `dono` e `permissoes`.
- `GET /ordens-servico/{numero}` passa a devolver `criadoPorId` e `criadoPorNome`.
- `RegistroService` marca o usuário criado no cadastro da oficina como dono.
- Leituras compartilhadas de clientes, veículos e peças liberam acesso quando o usuário tem `OS`, `AGENDA` ou `ORCAMENTOS`, para não quebrar os formulários.

### Observações

- A autorização é sempre validada no back-end; o front apenas esconderá menus e rotas na próxima fase.
- A tabela `orcamentos` já existe desde a V1; a API de orçamentos será entregue na Fase C.
- Nenhuma migração anterior foi alterada.

---

## Correção — listagem da equipe (`GET /usuarios`)

**Sintoma:** a tela Equipe retornava `500` com `function lower(bytea) does not exist`.

**Causa:** na query `UsuarioRepository.buscarEquipe`, os filtros opcionais (`:busca`, `:ativo`, `:executaOs`) chegavam como `null` sem tipo. Com `lower(?)`, o PostgreSQL inferia `bytea`.

**Correção:** os parâmetros opcionais agora são tipados na query (`cast(:busca as string)`, `cast(:ativo as boolean)` e `cast(:executaOs as boolean)`). Nenhuma migração foi necessária.

---

## Entrega atual — cadastro de equipe e permissões (V5)

Escopo: funcionários cadastrados pelo administrador da oficina, com acesso operacional sem Financeiro, Equipe e Configurações.

**Migração:** `V5__cadastro_equipe_e_permissoes.sql` (V1–V4 não foram editadas).

### Adicionado

- Colunas `usuarios.perfil` (`ADMIN`/`FUNCIONARIO`), `executa_os` e `deve_trocar_senha`.
- Backfill: usuários com cargo `ADMIN`, `DONO` ou `PROPRIETARIO` viram `ADMIN`; os demais, `FUNCIONARIO`.
- Constraint `usuarios_perfil_chk` e índices por oficina/perfil e oficina/executa_os.
- Enum `PerfilUsuario`.
- Endpoints `GET/POST /usuarios`, `GET/PUT /usuarios/{id}`, `PATCH /usuarios/{id}/status` e `POST /usuarios/{id}/reset-senha`.
- `SupabaseAdminService`: criar usuário, convidar, redefinir senha, banir/desbanir e excluir via Admin API.
- Senha temporária gerada pelo back-end e devolvida uma única vez.
- `PermissaoService` (bean `@permissoes`) e `@EnableMethodSecurity`.
- `403` para usuário inativo no `OficinaContexto`.
- `503 SERVICO_INDISPONIVEL` quando a service role key não está configurada.
- `docs/PLANO-EQUIPE.md`.
- Endpoints `GET/POST /boxes`, `PUT /boxes/{id}` e `DELETE /boxes/{id}` (desativação), com nome único por oficina (`BoxService`).
- Endpoints `GET /oficina` e `PUT /oficina` com dados cadastrais e de configuração (escrita só ADMIN).

### Alterado

- `GET /auth/me` passa a devolver `perfil`, `ativo` e `executaOs`.
- `GET /mecanicos` passa a devolver apenas ativos com `executa_os = true`, em `MecanicoDTO` enxuto (sem e-mail, telefone ou comissão).
- `FinanceiroController` e `UsuarioController` são admin-only via `@PreAuthorize`.
- `RegistroService` cria o dono da oficina como `ADMIN`.

### Variável de ambiente

- `SUPABASE_SERVICE_ROLE_KEY` (obrigatória para cadastro/reset de funcionário).

---

## Entrega anterior — V4 multi-oficina

Registro das alterações feitas nesta entrega (integração do front-end + V4 multi-oficina).

**Commit:** `3f1712a feat(saas): V4 multi-oficina com cadastro, edicao de OS e catalogo`

---

## Resumo

Esta entrega transforma a API em um back-end **SaaS multi-oficina** e fecha lacunas que impediam o front-end de operar sem overlays locais. A mudança de schema está concentrada em uma única migração nova (`V4`), sem editar V1–V3.

Principais frentes:

1. Cadastro de oficina (`POST /auth/registrar`).
2. Multi-oficina em veículos e tabelas novas.
3. Edição de OS (cabeçalho e itens).
4. Catálogo de serviços/peças com CRUD, soft-delete e peças sugeridas.
5. Estoque com movimentações persistentes e custo médio.
6. Financeiro, agenda, notificações, fornecedores, pedidos de compra e vistoria.
7. Endurecimento da migração e correções de bugs encontrados na validação.

---

## 1. Autenticação e cadastro

### Adicionado

- `POST /auth/registrar` (`RegistrarOficinaDTO`):
  - valida e-mail e CNPJ duplicados;
  - cria identidade no Supabase Auth (`/signup`);
  - cria oficina, usuário `ADMIN` e checklist padrão com 12 itens;
  - retorna tokens quando o Supabase devolve sessão.
- `RegistroService` e integração de signup no `AuthService`.
- Tratamento de respostas do Supabase com e sem confirmação de e-mail.

### Ajustado

- `AuthService` agora diferencia falha de credenciais, e-mail já cadastrado e indisponibilidade do Supabase.
- `SecurityConfig` libera `POST /auth/login` e `POST /auth/registrar`.

---

## 2. Multi-oficina

### Schema (V4)

- `veiculos.oficina_id` com backfill, FK, índice e `not null`.
- Trigger `trg_veiculos_oficina` para manter o vínculo mesmo com código legado.
- Remoção da unicidade global de placa/chassi; criação de índices únicos por oficina.
- `oficinas.uf` normalizada para `varchar(2)`.

### Código

- `OficinaContexto` resolve usuário/oficina a partir do `sub` do JWT.
- Repositórios e serviços filtram por `oficina_id`.
- `Veiculo` passa a mapear `oficinaId`.
- Conflitos de unicidade traduzidos para mensagens claras no `GlobalExceptionHandler`.

---

## 3. Ordens de serviço

### Adicionado

- `PUT /ordens-servico/{numero}` — edição do cabeçalho (box, mecânico, prioridade, km, diagnóstico, observações, previsão, combustível, desconto, pagamento, garantia, aprovação e cancelamento).
- `PUT /ordens-servico/{numero}/servicos/{id}` — edição de serviço da OS.
- `PUT /ordens-servico/{numero}/pecas/{id}` — edição de peça da OS.
- `GET /ordens-servico/{numero}/historico`.
- Campos novos de OS na V4 e nos DTOs (`OrdemServicoDTO`, `OrdemServicoResumoDTO`, `CriarOsDTO`, `AtualizarOsDTO`).
- Efeitos automáticos de `pronta_em`, `entregue_em`, `cancelada_em` e `fechada_em` na mudança de status.

### Ajustado

- Status `PRONTA` (ordem 8) e `ENTREGUE` (ordem 10) separados.
- `CONCLUIDA` permanece como legado inativo (ordem 9).
- `OrdemServicoService` valida pertencimento de cliente/veículo/box/mecânico à oficina.

### Limitação resolvida

Antes, o front precisava **excluir e recriar** itens para alterar quantidade, valor, horas ou responsável. Agora existem endpoints de `PUT` para serviços e peças, mantendo o `id` do item.

---

## 4. Catálogo de serviços e peças

### Adicionado

- CRUD completo de serviços:
  - `POST /servicos`
  - `PUT /servicos/{id}`
  - `DELETE /servicos/{id}` (soft-delete)
- CRUD completo de peças:
  - `POST /pecas`
  - `PUT /pecas/{id}`
  - `DELETE /pecas/{id}` (soft-delete)
- Geração automática de código (`SV-001`, `PC-001`) quando omitido.
- Peças sugeridas por serviço:
  - `GET /servicos/{id}/pecas-sugeridas`
  - `PUT /servicos/{id}/pecas-sugeridas`
  - tabela `servico_pecas_sugeridas`.
- Busca por texto (`q`) em serviços e peças.
- `GET /mecanicos` para alimentar filtros e seleção de responsável.

### Limitação resolvida

O front antes mantinha serviços/peças criados apenas no navegador e precisava simular exclusão. Agora o catálogo é persistido na API, e a exclusão é lógica (`ativo=false`), preservando histórico de OS.

---

## 5. Estoque

### Adicionado

- `POST /pecas/{id}/movimentacoes` com tipos `entrada`, `saida`, `ajuste` e `devolucao`.
- `GET /pecas/{id}/movimentacoes?limite=`.
- `GET /estoque/movimentacoes?limite=`.
- `fornecedores` e vínculo `pecas.fornecedor_id`.
- Coluna gerada `estoque_disponivel`.
- Trigger `trg_estoque_movimentacoes_aplica`.
- Custo médio ponderado em entradas com custo.

### Regras

- `entrada`/`devolucao` somam; `saida` subtrai; `ajuste` recebe a **nova contagem física**.
- Saldo negativo é rejeitado pelo banco.
- A peça é travada com `SELECT ... FOR UPDATE`.
- A movimentação valida que a peça pertence à oficina.

### Limitação resolvida

Movimentações e baixa de peças deixaram de ser apenas overlay local. O saldo agora é uma verdade única no banco, com histórico auditável.

---

## 6. Vistoria de entrada

### Adicionado

- `GET /ordens-servico/{numero}/vistoria`
- `PUT /ordens-servico/{numero}/vistoria`
- Tabelas `os_vistorias`, `os_vistoria_danos` e `os_vistoria_fotos`.
- `OsVistoriaDTO`, `RegistrarVistoriaDTO`, `VistoriaDanoDTO` e `VistoriaFotoDTO`.

Regras: uma vistoria por OS; dano único por área + tipo; URL de foto obrigatória.

---

## 7. Fornecedores e pedidos de compra

### Adicionado

- CRUD de fornecedores (`/fornecedores`).
- CRUD de pedidos de compra (`/pedidos-compra`), com status `RASCUNHO`, `ENVIADO`, `RECEBIDO` e `CANCELADO`.
- Numeração sequencial por oficina via `fn_proximo_numero_pedido_compra`.
- Recebimento que dá entrada automática das quantidades pendentes no estoque, usando o custo do item.
- Bloqueio de edição/exclusão em estados finais.

---

## 8. Financeiro

### Adicionado

- Pagamentos (contas a receber):
  - `GET/POST/PUT/PATCH/DELETE /pagamentos`
  - status `PENDENTE`, `PAGO`, `CANCELADO`, `ESTORNADO`.
- Despesas (contas a pagar):
  - `GET/POST/PUT/PATCH/DELETE /despesas`
  - status `PENDENTE`, `PAGO`, `CANCELADO`.
- Vínculos com OS, orçamento, cliente e fornecedor.

---

## 9. Agenda

### Adicionado

- `GET/POST/PUT/PATCH/DELETE /agendamentos`.
- Filtro por `status`, `inicio` e `fim`.
- Status `AGENDADO`, `CONFIRMADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`, `NAO_COMPARECEU`.
- Validação `fim > inicio`.

---

## 10. Notificações

### Adicionado

- `GET /notificacoes`
- `GET /notificacoes/nao-lidas/contador`
- `POST /notificacoes`
- `PATCH /notificacoes/{id}/lida`
- `PATCH /notificacoes/lidas`
- `DELETE /notificacoes/{id}`

---

## 11. Bugs e problemas encontrados na validação

### Corrigido — deadlock de RLS na V4

**Sintoma:** a migração travava ao habilitar RLS.
**Causa:** o loop incluía `flyway_schema_history`, que já estava travada pelo advisory lock do Flyway; o `ALTER TABLE` criava deadlock.
**Correção:** o loop ignora `flyway_schema_history`.

### Corrigido — `oficinas.uf` incompatível com `ddl-auto=validate`

**Sintoma:** Hibernate falhava na validação do schema.
**Causa:** coluna `varchar(3)` no banco e `@Column(length = 2)` na entidade.
**Correção:** V4 normaliza `uf` para `varchar(2)` e a entidade declara `length = 2`.

### Corrigido — saldo de estoque sobrescrito pelo Hibernate

**Sintoma:** a movimentação era inserida, o trigger atualizava o saldo, mas o valor voltava ao anterior.
**Causa:** a entidade `Peca` gerenciada ainda tinha o saldo antigo; um `save` posterior sobrescrevia o trigger.
**Correção:** o serviço faz `flush` + `refresh` e atualiza **somente** `preco_custo` via SQL nativo.

### Corrigido — leitura de enums nativos

**Sintoma:** risco de falha ao gravar `estoque_movimentacoes.tipo` em bancos cujo tipo é enum nativo.
**Correção:** `CatalogoService` descobre o tipo da coluna e resolve o rótulo do enum dinamicamente, funcionando com enum ou `varchar`.

### Encontrado — V2 falha em banco criado do zero

**Sintoma:** `cannot drop type tipo_orcamento_item because other objects depend on it`.
**Situação:** a V2 versionada não é compatível com banco vazio. Não foi alterada porque bancos de produção já a aplicaram.
**Contorno:** validação local usa uma cópia corrigida fora do repositório. Ver [BANCO-E-MIGRACOES.md](BANCO-E-MIGRACOES.md).

### Encontrado — unicidade global de placa/chassi

**Sintoma:** duas oficinas não podiam cadastrar o mesmo veículo.
**Correção:** V4 remove a unicidade global e cria índices por oficina, com normalização de placa e chassi. Se houver duplicidade legada, o índice não é criado e a migração avisa.

---

## 12. Validação executada

- Banco PostgreSQL 17.11 limpo.
- Flyway aplicou V1→V4 em ~0,7 s.
- `Started SalvoApiApplication` sem erros.
- `ddl-auto=validate` passou.
- Testes de banco com rollback:
  - trigger de estoque (entrada/saída/ajuste);
  - rejeição de saldo negativo;
  - `estoque_disponivel`;
  - numeração sequencial de pedido;
  - RLS e policy;
  - tipo de `oficinas.uf`.
- Endpoints principais exercitados contra dados reais de teste (1 cliente, 1 veículo, 1 OS), sem criar registros permanentes.

---

## 13. Compatibilidade e ordem de deploy

- As mudanças de schema são aditivas; o código antigo continua funcionando durante a transição.
- **Deploy do back-end primeiro**, depois o front-end que usa os campos/endpoints novos.
- Migrações não devem ser rodadas manualmente no Supabase; o Flyway aplica no start.
- Nenhuma migração aplicada (V1–V3) foi editada.

---

## 14. O que ainda não tem tela no front

Os endpoints abaixo existem e estão documentados, mas o front ainda não tem páginas dedicadas:

- fornecedores;
- pedidos de compra;
- financeiro (pagamentos/despesas);
- agenda;
- notificações;
- vistoria persistida (a tela existe, mas ainda usa parte do overlay local).

Isso está registrado também no changelog do front (`pi05/docs/ALTERACOES.md`).
