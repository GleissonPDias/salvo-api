# Salvô API — Back-end

API REST do **Salvô**, sistema de gestão para oficinas mecânicas. Este repositório contém o back-end em **Spring Boot 4 / Java 21**, responsável por autenticação, isolamento por oficina (SaaS multi-oficina), clientes, veículos, ordens de serviço, catálogo, estoque, financeiro, agenda e notificações.

O front-end React está em um repositório separado (`pi05`) e consome esta API via HTTP.

> **Status:** em desenvolvimento. A API está publicada em `https://salvo-api.onrender.com`.
> Documentação interativa (Swagger UI): `https://salvo-api.onrender.com/swagger-ui/index.html`.

---

## Sumário

- [Visão geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Como rodar localmente](#como-rodar-localmente)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Autenticação](#autenticação)
- [Multi-oficina (tenancy)](#multi-oficina-tenancy)
- [Grupos de endpoints](#grupos-de-endpoints)
- [Funcionalidades da V4](#funcionalidades-da-v4)
- [Funcionalidades da V5](#funcionalidades-da-v5)
- [Funcionalidades da V6](#funcionalidades-da-v6)
- [Boxes e dados da oficina](#boxes-e-dados-da-oficina)
- [Banco de dados e Flyway](#banco-de-dados-e-flyway)
- [Deploy no Render](#deploy-no-render)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Erros e validação](#erros-e-validação)
- [Documentação detalhada](#documentação-detalhada)
- [Problemas comuns](#problemas-comuns)

---

## Visão geral

| Item | Descrição |
| --- | --- |
| Framework | Spring Boot 4.1.1 |
| Java | 21 |
| Banco | PostgreSQL (Supabase) |
| Migrações | Flyway (`src/main/resources/db/migration`) |
| ORM | Spring Data JPA / Hibernate (`ddl-auto=validate`) |
| Autenticação | JWT do Supabase Auth (OAuth2 Resource Server) |
| Documentação | springdoc OpenAPI / Swagger UI |
| Build | Maven (`mvnw`) |
| Deploy | Render (Docker) |

Princípios do projeto:

- **Multi-oficina:** toda consulta e escrita é filtrada pela oficina do usuário autenticado.
- **Migrações aditivas:** nenhuma migração apaga dados existentes.
- **Contrato estável:** o front-end antigo continua funcionando durante a evolução do schema.
- **Validação no banco:** regras críticas (estoque, numeração, unicidade) também existem no PostgreSQL.

---

## Tecnologias

- Spring Boot Web MVC
- Spring Data JPA
- Spring Security + OAuth2 Resource Server
- Bean Validation (`jakarta.validation`)
- Flyway
- PostgreSQL / Supabase
- Lombok
- springdoc OpenAPI
- Maven

---

## Como rodar localmente

### Pré-requisitos

- Java 21 (JDK)
- PostgreSQL 15+ (local, Docker ou Supabase)
- Maven (ou use o wrapper `./mvnw`)

### Passo a passo

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd salvo-api

# 2. Configurar as variáveis de ambiente (veja a seção abaixo)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="postgres"
export SUPABASE_AUTH_JWK_SET_URI="https://SEU-PROJETO.supabase.co/auth/v1/.well-known/jwks.json"
export SUPABASE_AUTH_URL="https://SEU-PROJETO.supabase.co/auth/v1"
export SUPABASE_ANON_KEY="sua-anon-key"

# 3. Subir a aplicação (Flyway aplica as migrações automaticamente)
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Scripts úteis

| Comando | O que faz |
| --- | --- |
| `./mvnw spring-boot:run` | Inicia a API em desenvolvimento |
| `./mvnw clean package` | Gera o JAR em `target/` |
| `./mvnw test` | Executa os testes |
| `java -jar target/salvo-api-0.0.1-SNAPSHOT.jar` | Executa o JAR gerado |

### Perfis

O projeto usa `SPRING_PROFILES_ACTIVE` (vazio por padrão). Para configurações locais que não devem ir para o Git, crie `src/main/resources/application-local.properties` e rode com:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

---

## Variáveis de ambiente

Todas têm valor padrão em `src/main/resources/application.properties`, mas em produção devem ser definidas explicitamente.

### Banco de dados

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/postgres` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Senha do banco |

### Supabase Auth

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `SUPABASE_AUTH_ISSUER` | `https://seu-projeto.supabase.co/auth/v1` | Emissor esperado do JWT |
| `SUPABASE_AUTH_JWK_SET_URI` | `https://seu-projeto.supabase.co/auth/v1/.well-known/jwks.json` | Chaves públicas para validar o JWT |
| `SUPABASE_AUTH_URL` | `https://gkgnfxkbwexpihqybcue.supabase.co/auth/v1` | Base do Supabase Auth usada em login/cadastro |
| `SUPABASE_ANON_KEY` | vazio | Chave anon usada em `/auth/login` e `/auth/registrar` |
| `SUPABASE_SERVICE_ROLE_KEY` | vazio | Chave de serviço usada pela Admin API para cadastrar funcionários, redefinir senha e banir usuários |
| `SUPABASE_JWT_SECRET` | vazio | Reservado para validação simétrica legada |

> Sem `SUPABASE_ANON_KEY`, o login e o cadastro retornam erro de regra de negócio explicando que a chave precisa ser configurada. Sem `SUPABASE_SERVICE_ROLE_KEY`, os endpoints de equipe que dependem do Supabase Admin retornam `503`; o restante da API continua funcionando.

### Flyway / JPA

| Propriedade | Valor | Observação |
| --- | --- | --- |
| `spring.flyway.enabled` | `true` | Migrações aplicadas no start |
| `spring.flyway.baseline-on-migrate` | `true` | Bancos já existentes recebem baseline |
| `spring.flyway.baseline-version` | `0` | V1 roda em bancos legados |
| `spring.flyway.validate-on-migrate` | `true` | Checksums validados |
| `spring.flyway.postgresql.transactional.lock` | `false` | Evita deadlock no Supabase |
| `spring.jpa.hibernate.ddl-auto` | `validate` | O schema é responsabilidade do Flyway |

---

## Autenticação

A identidade é gerenciada pelo **Supabase Auth**; a API apenas valida o JWT e associa o `sub` do token ao usuário local (`usuarios.auth_user_id`).

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "usuario@oficina.com",
  "password": "12345678"
}
```

Resposta (snake_case, igual ao contrato do Supabase):

```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "expires_in": 3600,
  "refresh_token": "..."
}
```

O front-end guarda o token e envia `Authorization: Bearer <access_token>` em todas as chamadas.

### Cadastro de oficina

```http
POST /auth/registrar
Content-Type: application/json

{
  "nomeOficina": "Auto Mecânica Silva",
  "nomeUsuario": "Carlos Silva",
  "email": "carlos@silva.com",
  "password": "12345678",
  "telefone": "(11) 99999-9999",
  "cnpj": "00.000.000/0001-00"
}
```

O fluxo:

1. Verifica e-mail/CNPJ duplicado no banco local.
2. Cria o usuário no Supabase Auth (`/signup`).
3. Cria a oficina e o usuário local com cargo `ADMIN`.
4. Cria o checklist padrão da oficina (12 itens).
5. Retorna tokens **se a confirmação de e-mail estiver desabilitada** no Supabase; caso contrário, retorna apenas os dados do usuário e o front exibe a mensagem de confirmação.

### Usuário logado

```http
GET /auth/me
Authorization: Bearer <token>
```

Retorna `id`, `oficinaId`, `oficinaNome`, `nome`, `email`, `cargo`, `perfil`, `ativo`, `executaOs`, `telefone`, `especialidades`, `lider`, `comissaoPercentual`, `corAgenda`, `fotoUrl`, `deveTrocarSenha`, `dono` e `permissoes`.

`perfil` é `ADMIN` ou `FUNCIONARIO`. `executaOs` indica se o usuário pode ser escolhido como mecânico responsável na OS. `dono` identifica o usuário que faz login direto no app; `permissoes` lista os módulos liberados (administradores recebem todos).

### Rotas públicas

- `POST /auth/login`
- `POST /auth/registrar`
- `GET /actuator/health`
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `OPTIONS /**` (preflight CORS)

Todas as demais exigem JWT válido.

---

## Multi-oficina (tenancy)

- O JWT contém o `sub` do Supabase (`auth.uid`).
- `OficinaContexto` resolve o usuário local pelo `auth_user_id` e obtém `oficina_id`.
- Serviços e repositórios sempre filtram por `oficina_id`.
- O banco tem RLS habilitado em `public`; a policy permissiva é criada apenas para o papel da API (`current_user`). `anon` e `authenticated` não recebem grants — o front nunca fala direto com o banco.
- Se um JWT válido não tiver usuário local correspondente **ou** o usuário estiver inativo, a API responde `403 ACESSO_NEGADO`.
- O perfil (`ADMIN`/`FUNCIONARIO`) e as permissões por módulo são lidos do banco, não do JWT, e aplicados com `@PreAuthorize`. Administradores têm acesso total; funcionários acessam apenas os módulos marcados. Equipe, Configurações e Auditoria são exclusivos do `ADMIN`.

---

## Grupos de endpoints

Referência completa, com corpos de requisição e resposta em [docs/API.md](docs/API.md).

| Grupo | Base | Resumo |
| --- | --- | --- |
| Auth | `/auth` | Login, cadastro de oficina e usuário logado |
| Clientes | `/clientes` | CRUD com paginação e busca; exclusão lógica |
| Veículos | `/veiculos`, `/clientes/{id}/veiculos` | CRUD por oficina; exclusão lógica |
| Ordens de Serviço | `/ordens-servico` | Lista paginada, detalhe por número, status, itens, histórico |
| Catálogo | `/servicos`, `/pecas`, `/mecanicos` | Serviços, peças, peças sugeridas e equipe técnica |
| Equipe | `/usuarios` | Cadastro e gestão de funcionários (somente `ADMIN`)
| Auditoria | `/auditoria` | Relatório de ações com usuário, data/hora e módulo (somente `ADMIN`)
| Estoque | `/pecas/{id}/movimentacoes`, `/estoque/movimentacoes` | Entradas, saídas, ajustes e histórico |
| Vistoria | `/ordens-servico/{numero}/vistoria` | Vistoria de entrada com danos e fotos |
| Fornecedores | `/fornecedores` | CRUD de fornecedores |
| Pedidos de compra | `/pedidos-compra` | Criação, recebimento e entrada no estoque |
| Financeiro | `/pagamentos`, `/despesas` | Contas a receber e a pagar |
| Agenda | `/agendamentos` | Agendamentos por período, mecânico e box |
| Notificações | `/notificacoes` | Listagem, contador de não lidas e marcação |

---

## Funcionalidades da V4

A migração `V4__saas_multi_oficina_features.sql` e o código correspondente entregam:

### Multi-oficina

- `veiculos.oficina_id` com backfill a partir do cliente.
- Unicidade de placa/chassi **por oficina** (não mais global).
- Trigger que mantém `oficina_id` coerente em inserts/updates.
- Diversas tabelas novas com `oficina_id` e `on delete cascade`.

### Ordens de serviço

- Status `PRONTA` e `ENTREGUE` separados; `CONCLUIDA` fica inativa como legado.
- Campos de cabeçalho: tipo, canal, título, relato, observações, previsão, combustível, km de saída, desconto (valor/percentual), pagamento, garantia, aprovação e cancelamento.
- `PUT /ordens-servico/{numero}` para editar o cabeçalho.
- `PUT /ordens-servico/{numero}/servicos/{id}` e `PUT .../pecas/{id}` para editar itens.
- Histórico de status e eventos.
- Vistoria de entrada (`os_vistorias`, `os_vistoria_danos`, `os_vistoria_fotos`).

### Catálogo e estoque

- CRUD de serviços e peças com código automático (`SV-001`, `PC-001`).
- Peças sugeridas por serviço (`servico_pecas_sugeridas`).
- Movimentações de estoque com trigger `BEFORE INSERT`:
  - soma o saldo, trava a peça com `SELECT ... FOR UPDATE`;
  - rejeita saldo negativo;
  - valida que a peça pertence à mesma oficina.
- Custo médio ponderado atualizado em entradas com custo.
- Pedidos de compra com numeração sequencial por oficina; ao receber, dá entrada das quantidades pendentes.

### Financeiro, agenda e notificações

- `pagamentos` (contas a receber) e `despesas` (contas a pagar) com status validados.
- `agendamentos` com validação de período (`fim > inicio`) e status.
- `notificacoes` por oficina/usuário, com contador de não lidas.

---

## Funcionalidades da V5

A migração `V5__cadastro_equipe_e_permissoes.sql` adiciona o cadastro de funcionários pelo administrador:

- `usuarios.perfil`: `ADMIN` ou `FUNCIONARIO`;
- `usuarios.executa_os`: define quem aparece como mecânico responsável na OS;
- `usuarios.deve_trocar_senha`: prepara o fluxo de senha temporária;
- backfill preserva como `ADMIN` quem já tinha cargo `ADMIN`, `DONO` ou `PROPRIETARIO`;
- `GET/POST /usuarios`, `GET/PUT /usuarios/{id}`, `PATCH /usuarios/{id}/status` e `POST /usuarios/{id}/reset-senha`;
- funcionário criado sempre nasce `FUNCIONARIO` e é criado no Supabase Auth via Admin API;
- `GET /mecanicos` devolve apenas ativos com `executa_os = true`, em payload enxuto;
- `Financeiro`, `Equipe` e `Configurações` são admin-only no back-end (`@PreAuthorize`);
- usuário inativo recebe `403` em qualquer rota autenticada;
- sem `SUPABASE_SERVICE_ROLE_KEY`, cadastro/reset de funcionário retorna `503`.

O plano completo está em [docs/PLANO-EQUIPE.md](docs/PLANO-EQUIPE.md).

---

## Funcionalidades da V6

A migração `V6__permissoes_por_modulo_e_auditoria.sql` adiciona permissões por módulo e auditoria:

- `usuario_permissoes`: módulos `CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA` e `FINANCEIRO` por funcionário;
- `usuarios.dono`: único usuário que faz login direto no app; o administrador mais antigo de cada oficina é marcado no backfill;
- `POST/PUT /usuarios` aceitam `perfil` e `permissoes`; administradores têm acesso total;
- `@PreAuthorize` por módulo em clientes, veículos, catálogo/estoque, OS, vistoria, agenda, financeiro, fornecedores e pedidos de compra;
- o dono não pode ser rebaixado nem desativado, e só ele gerencia a própria conta;
- `auditoria` registra toda escrita concluída com sucesso, com usuário, data/hora, ação, módulo, entidade e caminho;
- `GET /auditoria` com filtros por usuário, módulo e período (somente `ADMIN`);
- `GET /ordens-servico/{numero}` devolve `criadoPorId` e `criadoPorNome`.

Testes: `PermissaoServiceTest`, `AuditoriaInterceptorTest` e regras do dono em `UsuarioServiceTest`. A validação funcional local confirmou `403` para funcionário sem o módulo e registro de auditoria nas escritas.

---

## Boxes e dados da oficina

- `GET /boxes` (ativos por padrão, `incluirInativos=true` para todos), `POST /boxes`, `PUT /boxes/{id}` e `DELETE /boxes/{id}` (desativação). Escrita restrita ao `ADMIN`; nome único por oficina.
- `GET /oficina` e `PUT /oficina` com dados cadastrais (nome fantasia, CNPJ, contatos, endereço, validade de orçamento, desconto máximo e cor primária). Usados no cabeçalho dos documentos impressos. Escrita restrita ao `ADMIN`.
- Não exige migração nova: usa as tabelas `boxes` e `oficinas` já criadas pela V1/V2/V4.

Detalhes e exemplos em [docs/API.md](docs/API.md#boxes-e-oficina).

## Banco de dados e Flyway

As migrações ficam em `src/main/resources/db/migration`:

| Versão | Arquivo | Conteúdo |
| --- | --- | --- |
| V1 | `V1__baseline.sql` | Schema base (oficinas, clientes, veículos, OS, catálogo, orçamentos) |
| V2 | `V2__corrigir_baseline.sql` | Correções de nomes, triggers e tipos |
| V3 | `V3__numeracao_sequencial_os.sql` | Numeração sequencial e imutável de OS por oficina |
| V4 | `V4__saas_multi_oficina_features.sql` | Multi-oficina, status, OS, estoque, financeiro, agenda, notificações e RLS |
| V5 | `V5__cadastro_equipe_e_permissoes.sql` | Perfil de acesso, flag de mecânico e senha temporária |
| V6 | `V6__permissoes_por_modulo_e_auditoria.sql` | Permissões por módulo, dono da oficina e auditoria |
| V7 | `V7__orcamentos_campos_comerciais.sql` | Campos comerciais do orçamento |
| V8 | `V8__orcamento_itens_permite_itens_avulsos.sql` | Permite itens avulsos no orçamento |

Regras importantes:

- **Nunca edite V1 a V5.** Toda mudança nova entra em uma nova versão.
- A V4, a V5 e a V6 são **idempotentes** e **aditivas**.
- Detalhes de triggers, RLS e validação local estão em [docs/BANCO-E-MIGRACOES.md](docs/BANCO-E-MIGRACOES.md).

---

## Deploy no Render

A ordem importa: **o back-end com a V5 deve ser publicado antes do front-end novo**.

1. Configure as variáveis de ambiente no Render (`SPRING_DATASOURCE_*`, `SUPABASE_*` e `SUPABASE_SERVICE_ROLE_KEY`).
2. Faça o deploy da `main`.
3. No start, o Flyway aplica as migrações pendentes.
4. Confirme nos logs:
   - `Successfully applied ... migration` para a V4;
   - `Started SalvoApiApplication`;
   - nenhuma falha de `ddl-auto=validate`.
5. Verifique `GET /actuator/health` e o Swagger.

O `Dockerfile` usa build em duas etapas (Maven + JRE 21) e expõe a porta `8080`.

---

## Estrutura do projeto

```
salvo-api/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
├── docs/
│   ├── API.md
│   ├── BANCO-E-MIGRACOES.md
│   └── ALTERACOES.md
└── src/
    ├── main/
    │   ├── java/com/salvo/salvo_api/
    │   │   ├── configs/        # Segurança, CORS, OpenAPI, contexto da oficina
    │   │   ├── controllers/    # Endpoints REST
    │   │   ├── dtos/           # Contratos de entrada/saída
    │   │   ├── entities/       # Entidades JPA
    │   │   ├── exceptions/     # Exceções e handler global
    │   │   ├── repositories/   # Repositórios Spring Data
    │   │   ├── services/       # Regras de negócio
    │   │   └── SalvoApiApplication.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/   # V1..V4
    └── test/
```

---

## Erros e validação

Formato único de erro (`ErroDTO`):

```json
{
  "codigo": "REGRA_DE_NEGOCIO",
  "mensagem": "Estoque insuficiente da peça 12: saldo 2, movimento -5",
  "detalhes": [],
  "timestamp": "2026-11-10T12:00:00Z"
}
```

| HTTP | Código | Quando |
| --- | --- | --- |
| 400 | — | Erro de sintaxe/parâmetro |
| 401 | — | Token ausente, inválido ou expirado |
| 403 | `ACESSO_NEGADO` | Usuário sem oficina/registro local, inativo ou sem permissão |
| 404 | `NAO_ENCONTRADO` | Recurso inexistente na oficina |
| 409 | `CONFLITO` | Unicidade (CPF, placa, código, e-mail...) |
| 422 | `VALIDACAO` | Campos inválidos (com `detalhes`) |
| 422 | `REGRA_DE_NEGOCIO` | Regra de negócio violada |
| 503 | `SERVICO_INDISPONIVEL` | Dependência externa não configurada (ex.: `SUPABASE_SERVICE_ROLE_KEY`) |
| 500 | `ERRO_INTERNO` | Falha inesperada |

---

## Documentação detalhada

- [docs/API.md](docs/API.md) — referência de endpoints, contratos e exemplos.
- [docs/BANCO-E-MIGRACOES.md](docs/BANCO-E-MIGRACOES.md) — schema, triggers, RLS, V1–V5 e validação local.
- [docs/ALTERACOES.md](docs/ALTERACOES.md) — changelog das entregas.
- [docs/PLANO-EQUIPE.md](docs/PLANO-EQUIPE.md) — plano de cadastro de equipe e permissões.

---

## Problemas comuns

**`Schema-validation: missing column` ou `wrong column type` no start**
O banco está atrás das migrações ou uma migração nova não foi aplicada. Verifique se o Flyway rodou (`spring.flyway.enabled=true`) e se `ddl-auto=validate` está falhando por divergência real entre entidade e schema.

**`Flyway detected resolved migration not applied to database`**
Há migração nova no repositório que ainda não foi aplicada no banco. Faça o deploy/restart e confira os logs.

**401 com erro de CORS no navegador**
Token inválido/expirado pode fazer a API responder 401 **sem** cabeçalhos CORS; o navegador exibe erro de CORS. Faça login novamente antes de investigar o `CorsConfig`.

**`anon-key` não configurada**
Defina `SUPABASE_ANON_KEY` no ambiente. Sem ela, `/auth/login` e `/auth/registrar` não funcionam.

**Deadlock na migração**
A V4 ignora `flyway_schema_history` no loop de RLS justamente para evitar o deadlock entre `ALTER TABLE` e o advisory lock do Flyway. Se você criar uma migração nova com loop de RLS, mantenha essa exclusão.

**Banco vazio falha na V2**
A `V2` do repositório não é totalmente compatível com um banco criado do zero (dependência do enum `tipo_orcamento_item`). A validação local usa uma cópia corrigida em `/tmp`, sem alterar o arquivo versionado. Veja [docs/BANCO-E-MIGRACOES.md](docs/BANCO-E-MIGRACOES.md).
