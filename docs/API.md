# Referência da API — Salvô

Base local: `http://localhost:8080`
Base publicada: `https://salvo-api.onrender.com`
Swagger UI: `/swagger-ui/index.html`

Todos os endpoints, exceto os públicos listados em [Autenticação](#autenticação), exigem:

```http
Authorization: Bearer <access_token>
```

Os exemplos usam JSON. Datas e horários com fuso usam ISO-8601 (`2026-11-10T14:30:00-03:00`); datas puras usam `YYYY-MM-DD`.

---

## Sumário

- [Convenções](#convenções)
- [Autenticação](#autenticação)
- [Equipe e permissões](#equipe-e-permissões)
- [Boxes e oficina](#boxes-e-oficina)
- [Clientes](#clientes)
- [Veículos](#veículos)
- [Ordens de serviço](#ordens-de-serviço)
- [Catálogo de serviços](#catálogo-de-serviços)
- [Peças e estoque](#peças-e-estoque)
- [Vistoria da OS](#vistoria-da-os)
- [Fornecedores](#fornecedores)
- [Pedidos de compra](#pedidos-de-compra)
- [Financeiro](#financeiro)
- [Agenda](#agenda)
- [Notificações](#notificações)
- [Enums e status](#enums-e-status)
- [Formato de erro](#formato-de-erro)

---

## Convenções

### Paginação (Spring `Page`)

`GET /clientes`, `GET /veiculos` e `GET /ordens-servico` aceitam:

| Parâmetro | Padrão | Descrição |
| --- | --- | --- |
| `page` | `0` | Página (base zero) |
| `size` | `20` | Itens por página |
| `sort` | depende do endpoint | Ex.: `numero,desc` |

Resposta:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true,
  "empty": true
}
```

### Arrays simples

`/servicos`, `/pecas`, `/mecanicos`, `/fornecedores`, `/pedidos-compra`, `/pagamentos`, `/despesas`, `/agendamentos` e `/notificacoes` devolvem arrays JSON, não `Page`.

### Exclusão lógica

`DELETE /clientes/{id}`, `DELETE /veiculos/{id}`, `DELETE /servicos/{id}` e `DELETE /pecas/{id}` **desativam** (`ativo=false`) o registro, preservando histórico. Retornam `204 No Content`.

---

## Autenticação

### `POST /auth/login` (público)

```json
{
  "email": "usuario@oficina.com",
  "password": "12345678"
}
```

Resposta `200`:

```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "expires_in": 3600,
  "refresh_token": "..."
}
```

### `POST /auth/registrar` (público)

```json
{
  "nomeOficina": "Auto Mecânica Silva",
  "nomeUsuario": "Carlos Silva",
  "email": "carlos@silva.com",
  "password": "12345678",
  "telefone": "(11) 99999-9999",
  "cnpj": "00.000.000/0001-00"
}
```

Regras:

- `nomeOficina`, `nomeUsuario`, `email` e `password` são obrigatórios.
- Senha com no mínimo 6 caracteres.
- E-mail e CNPJ (somente dígitos) não podem repetir.
- Cria oficina, usuário `ADMIN`, checklist padrão (12 itens) e identidade no Supabase.

Resposta: mesma estrutura do login quando o Supabase devolve sessão; caso a confirmação de e-mail esteja ativa, os tokens vêm nulos e o front exibe a mensagem de confirmação.

### `GET /auth/me`

```json
{
  "id": 5,
  "oficinaId": 16,
  "oficinaNome": "Oficina Matriz",
  "nome": "Carlos Silva",
  "email": "carlos@silva.com",
  "cargo": "ADMIN",
  "perfil": "ADMIN",
  "ativo": true,
  "executaOs": true,
  "telefone": "(11) 99999-9999",
  "especialidades": null,
  "lider": true,
  "comissaoPercentual": null,
  "corAgenda": null,
  "fotoUrl": null,
  "deveTrocarSenha": false,
  "dono": true,
  "permissoes": ["CLIENTES", "VEICULOS", "OS", "ORCAMENTOS", "ESTOQUE", "AGENDA", "FINANCEIRO"]
}
```

`perfil` é `ADMIN` ou `FUNCIONARIO`. `executaOs` indica se o usuário aparece como mecânico responsável na OS, independentemente do `cargo` exibido.

### Troca de usuário (Fase B)

| Método | Caminho | Autorização | Descrição |
| --- | --- | --- | --- |
| `POST` | `/auth/trocar-usuario` | ADMIN | Valida a senha do usuário e devolve o token dele |
| `POST` | `/auth/validar-senha` | Autenticado | Valida a senha de um administrador (voltar ao admin) |
| `POST` | `/auth/alterar-senha` | Autenticado | Altera a senha do usuário logado e limpa `deveTrocarSenha` |

`POST /auth/login` aceita apenas o dono da oficina. Os demais usuários entram pela troca de usuário, a partir da sessão do admin.

`dono` identifica o único usuário que faz login direto no app. `permissoes` lista os módulos liberados: administradores recebem todos; funcionários recebem apenas os marcados no cadastro.

---

## Equipe e permissões

O cadastro de funcionários é feito pelo **administrador** da oficina, já autenticado. Todo usuário criado por aqui nasce com `perfil = FUNCIONARIO`.

| Método | Caminho | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/usuarios?busca=&ativo=&executaOs=` | ADMIN | Lista a equipe da oficina |
| `GET` | `/usuarios/{id}` | ADMIN ou o próprio | Detalhe do usuário |
| `POST` | `/usuarios` | ADMIN | Cria funcionário no Supabase Auth + `usuarios` |
| `PUT` | `/usuarios/{id}` | ADMIN | Edita dados operacionais |
| `PATCH` | `/usuarios/{id}/status` | ADMIN | Ativa/desativa |
| `POST` | `/usuarios/{id}/reset-senha` | ADMIN | Gera senha temporária |
| `GET` | `/mecanicos` | Autenticado | Ativos com `executaOs = true` |
| `GET` | `/auditoria?usuarioId=&modulo=&de=&ate=` | ADMIN | Relatório de ações (paginado) |

### Criar funcionário

```json
{
  "nome": "João Mecânico",
  "email": "joao@oficina.com",
  "senhaInicial": null,
  "enviarConvite": false,
  "telefone": "(11) 98888-7777",
  "cargo": "Mecânico",
  "especialidades": "Motor e freios",
  "lider": false,
  "comissaoPercentual": 10.00,
  "corAgenda": "#2563eb",
  "executaOs": true,
  "perfil": "FUNCIONARIO",
  "permissoes": ["OS", "AGENDA", "ESTOQUE"]
}
```

Regras:

- `nome`, `email` e `cargo` são obrigatórios;
- `perfil` aceita `FUNCIONARIO` (padrão) ou `ADMIN`;
- `permissoes` vale para funcionários e aceita `CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA` e `FINANCEIRO`; administradores têm acesso total e não precisam de marcação;
- somente o dono da oficina pode alterar, desativar ou redefinir a senha da própria conta de dono;
- o perfil do dono não pode ser rebaixado nem ele pode ser desativado;
- `executaOs` (padrão `true`) define se o funcionário aparece em `GET /mecanicos`;
- `senhaInicial`, quando omitida, é gerada pelo back-end e devolvida **uma única vez**;
- `enviarConvite = true` envia convite por e-mail pelo Supabase (requer SMTP configurado);
- e-mail já existente retorna `422 REGRA_DE_NEGOCIO`.

Resposta `201`:

```json
{
  "usuario": {
    "id": 20,
    "nome": "João Mecânico",
    "email": "joao@oficina.com",
    "cargo": "Mecânico",
    "perfil": "FUNCIONARIO",
    "telefone": "(11) 98888-7777",
    "especialidades": "Motor e freios",
    "lider": false,
    "comissaoPercentual": 10.00,
    "corAgenda": "#2563eb",
    "fotoUrl": null,
    "ativo": true,
    "executaOs": true,
    "deveTrocarSenha": true,
    "dono": false,
    "permissoes": ["OS", "AGENDA", "ESTOQUE"],
    "criadoEm": "2026-11-10T12:00:00Z"
  },
  "senhaTemporaria": "Xy7kPq2mN4rs"
}
```

`senhaTemporaria` vem `null` quando uma senha inicial foi informada ou quando o convite foi enviado.

### Editar, ativar e redefinir senha

`PUT /usuarios/{id}` aceita `nome`, `telefone`, `cargo`, `especialidades`, `lider`, `comissaoPercentual`, `corAgenda`, `executaOs`, `perfil` e `permissoes`. E-mail não é alterável (é a identidade no Supabase Auth). Ao promover para `ADMIN`, as permissões são ignoradas e o usuário passa a ter acesso total.

```http
PATCH /usuarios/{id}/status
```

```json
{ "ativo": false }
```

- desativar o próprio usuário retorna `422`;
- desativar o último `ADMIN` ativo da oficina retorna `422`;
- usuário inativo recebe `403` em qualquer rota autenticada;
- o banimento no Supabase é feito em modo *best-effort*.

```http
POST /usuarios/{id}/reset-senha
```

```json
{ "senhaTemporaria": "Xy7kPq2mN4rs" }
```

A senha é exibida uma única vez e `deveTrocarSenha` volta para `true`.

### Supabase Admin API

A criação, o convite, a redefinição de senha e o banimento usam a **service role key** do Supabase:

```properties
salvo.security.supabase.service-role-key=${SUPABASE_SERVICE_ROLE_KEY:}
```

Sem essa variável, as operações que dependem do Supabase Admin retornam `503 SERVICO_INDISPONIVEL`; o restante da API continua funcionando.

### Matriz de acesso

| Área | Permissão | ADMIN | FUNCIONARIO |
| --- | --- | --- | --- |
| Clientes | `CLIENTES` | sim | se marcada |
| Veículos | `VEICULOS` | sim | se marcada |
| Ordens de serviço, catálogo de serviços e vistoria | `OS` | sim | se marcada |
| Orçamentos | `ORCAMENTOS` | sim | se marcada |
| Peças, estoque, fornecedores e pedidos de compra | `ESTOQUE` | sim | se marcada |
| Agenda | `AGENDA` | sim | se marcada |
| Financeiro (`/pagamentos`, `/despesas`) | `FINANCEIRO` | sim | se marcada |
| Equipe (`/usuarios`), configurações e auditoria | — | sim | **não** |

Leituras compartilhadas (busca de clientes, veículos e peças) liberam o acesso quando o usuário tem qualquer um dos módulos que usam esses dados (`OS`, `AGENDA`, `ORCAMENTOS` ou `VEICULOS`, conforme o caso).

A restrição é aplicada no back-end com `@PreAuthorize`; esconder o item de menu no front é apenas usabilidade.

### Auditoria

Toda operação de escrita concluída com sucesso (`POST`, `PUT`, `PATCH` e `DELETE`) é registrada na tabela `auditoria`, com:

- usuário (id e nome copiado no momento da ação);
- data/hora;
- ação (`CRIAR`, `ATUALIZAR`, `ALTERAR`, `EXCLUIR`);
- módulo, entidade e id do registro quando houver;
- método HTTP, caminho e status da resposta.

`GET /auditoria` aceita `usuarioId`, `modulo` (`CLIENTES`, `VEICULOS`, `OS`, `ORCAMENTOS`, `ESTOQUE`, `AGENDA` ou `FINANCEIRO`), `de` e `ate` no formato `AAAA-MM-DD`, além da paginação padrão. É exclusivo de administradores.

Login, notificações e endpoints de documentação não são auditados.

---

## Boxes e oficina

### Boxes de atendimento

| Método | Caminho | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/boxes?incluirInativos=false` | Autenticado | Lista os boxes ativos (ou todos, com `incluirInativos=true`) |
| `POST` | `/boxes` | ADMIN | Cria um box |
| `PUT` | `/boxes/{id}` | ADMIN | Altera nome, descrição ou situação (`ativo`) |
| `DELETE` | `/boxes/{id}` | ADMIN | Desativa o box (não remove: OS e agendamentos antigos continuam referenciando-o) |

```json
{ "nome": "Box 07", "descricao": "Elevador hidráulico" }
```

Resposta: `{ "id": 7, "nome": "Box 07", "descricao": "Elevador hidráulico", "ativo": true }`.

Regras:

- nome obrigatório (até 60 caracteres), único por oficina ignorando maiúsculas e espaços (`422` se repetido);
- no `PUT`, `null` mantém o valor atual e `""` limpa a descrição; `nome` em branco retorna `422`;
- boxes desativados deixam de aparecer em `GET /boxes` padrão.

### Dados da oficina

| Método | Caminho | Autorização | Descrição |
| --- | --- | --- | --- |
| `GET` | `/oficina` | Autenticado | Dados cadastrais e de configuração da oficina do usuário |
| `PUT` | `/oficina` | ADMIN | Atualiza os dados da oficina |

Resposta de `GET`:

```json
{
  "id": 16,
  "nome": "Oficina Matriz",
  "nomeFantasia": "Salvô Gestão Automotiva",
  "cnpj": "12345678000190",
  "telefone": "1130457700",
  "whatsapp": "11977774500",
  "email": "contato@oficina.com",
  "site": null,
  "logoUrl": null,
  "cep": "04556500",
  "logradouro": "Av. Santo Amaro",
  "numero": "4120",
  "complemento": null,
  "bairro": "Brooklin",
  "cidade": "São Paulo",
  "uf": "SP",
  "horarioFuncionamento": "Seg a sex, 8h às 18h",
  "validadeOrcamentoDias": 7,
  "descontoMaximoPercentual": 10.00,
  "corPrimaria": "#2563eb",
  "ativa": true
}
```

No `PUT`, `nome` é obrigatório. Nos demais campos, `null` (ou campo omitido) mantém o valor atual, enquanto texto vazio `""` limpa o campo. `cnpj` e `cep` são gravados somente com dígitos; `uf` é normalizado para maiúsculas. `validadeOrcamentoDias` (1 a 365) e `descontoMaximoPercentual` (0 a 100) são validados.

---

## Clientes

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/clientes?q=&page=&size=&sort=nome` | Lista paginada; `q` busca nome/CPF/CNPJ |
| `GET` | `/clientes/{id}` | Detalhe |
| `POST` | `/clientes` | Cria |
| `PUT` | `/clientes/{id}` | Atualiza |
| `DELETE` | `/clientes/{id}` | Desativa |

### Corpo de criação

```json
{
  "nome": "Robertinho Alves",
  "cpfCnpj": "12345678901",
  "email": "robertinho@email.com",
  "telefone": "11999998888",
  "telefoneSecundario": null,
  "classificacao": "REGULAR",
  "whatsapp": true,
  "aceitaComunicacoes": true,
  "observacoes": "Cliente preferencial"
}
```

`nome`, `cpfCnpj` e `telefone` são obrigatórios.

### Resposta

```json
{
  "id": 1,
  "nome": "Robertinho Alves",
  "cpfCnpj": "12345678901",
  "email": "robertinho@email.com",
  "telefone": "11999998888",
  "telefoneSecundario": null,
  "classificacao": "REGULAR",
  "whatsapp": true,
  "aceitaComunicacoes": true,
  "observacoes": "Cliente preferencial",
  "ativo": true,
  "criadoEm": "2026-11-01T10:00:00Z"
}
```

No `PUT`, apenas `nome`, `email`, `telefone`, `telefoneSecundario`, `classificacao`, `whatsapp`, `aceitaComunicacoes` e `observacoes` são aceitos. CPF/CNPJ não é alterável.

---

## Veículos

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/veiculos?page=&size=&sort=modelo` | Lista paginada da oficina |
| `GET` | `/veiculos/{id}` | Detalhe |
| `GET` | `/clientes/{clienteId}/veiculos` | Veículos de um cliente |
| `POST` | `/clientes/{clienteId}/veiculos` | Cria veículo para o cliente |
| `PUT` | `/veiculos/{id}` | Atualiza |
| `DELETE` | `/veiculos/{id}` | Desativa |

### Criação

```json
{
  "placa": "ABC1D23",
  "chassi": "9BWZZZ377VT004251",
  "marca": "Volkswagen",
  "modelo": "Gol",
  "versao": "1.0 Flex",
  "anoFabricacao": 2020,
  "anoModelo": 2021,
  "cor": "Prata",
  "combustivel": "FLEX",
  "motorizacao": "1.0",
  "transmissao": "MANUAL",
  "kmAtual": 45000,
  "fotoUrl": null,
  "categoria": "HATCH",
  "rascunho": false
}
```

`placa`, `marca` e `modelo` são obrigatórios. A placa é única **por oficina**, ignorando hífens e caixa (`ABC-1251` = `ABC1251`).

### Resposta

```json
{
  "id": 10,
  "clienteId": 1,
  "clienteNome": "Robertinho Alves",
  "oficinaId": 16,
  "placa": "ABC1D23",
  "chassi": "9BWZZZ377VT004251",
  "marca": "Volkswagen",
  "modelo": "Gol",
  "versao": "1.0 Flex",
  "anoFabricacao": 2020,
  "anoModelo": 2021,
  "cor": "Prata",
  "combustivel": "FLEX",
  "motorizacao": "1.0",
  "transmissao": "MANUAL",
  "kmAtual": 45000,
  "fotoUrl": null,
  "categoria": "HATCH",
  "rascunho": false,
  "ativo": true
}
```

No `PUT`, são aceitos `cor`, `combustivel`, `motorizacao`, `transmissao`, `kmAtual`, `versao`, `fotoUrl`, `categoria` e `rascunho`.

---

## Ordens de serviço

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/ordens-servico?status=&clienteId=&page=&size=&sort=numero` | Lista paginada |
| `GET` | `/ordens-servico/{numero}` | Detalhe pelo número da OS |
| `POST` | `/ordens-servico` | Cria OS |
| `PUT` | `/ordens-servico/{numero}` | Atualiza o cabeçalho |
| `PATCH` | `/ordens-servico/{numero}/status` | Muda o status |
| `GET` | `/ordens-servico/{numero}/historico` | Histórico de status/eventos |
| `POST` | `/ordens-servico/{numero}/servicos` | Adiciona serviço |
| `PUT` | `/ordens-servico/{numero}/servicos/{id}` | Atualiza serviço |
| `DELETE` | `/ordens-servico/{numero}/servicos/{id}` | Remove serviço |
| `POST` | `/ordens-servico/{numero}/pecas` | Adiciona peça |
| `PUT` | `/ordens-servico/{numero}/pecas/{id}` | Atualiza peça |
| `DELETE` | `/ordens-servico/{numero}/pecas/{id}` | Remove peça |

> O identificador público da OS é o **número** (sequencial por oficina), não o `id` interno.

### Criação

```json
{
  "clienteId": 1,
  "veiculoId": 10,
  "boxId": 1,
  "mecanicoLiderId": 5,
  "prioridade": "NORMAL",
  "kmEntrada": 45000,
  "diagnostico": "Cliente relata ruído na suspensão",
  "observacoes": "Revisão de 45 mil km",
  "tipo": "corretiva",
  "canal": "balcao",
  "titulo": "Revisão e suspensão",
  "relatoCliente": "Barulho ao passar em lombadas",
  "observacoesInternas": "Verificar amortecedores",
  "previsaoEntrega": "2026-11-12T17:00:00-03:00",
  "nivelCombustivel": 2
}
```

Regras:

- `clienteId` e `veiculoId` são obrigatórios.
- O veículo precisa pertencer ao cliente informado.
- A OS nasce com status `ABERTA`.
- O número é gerado de forma atômica e **nunca reutilizado**.
- `prioridade`: `BAIXA`, `NORMAL`, `ALTA`, `URGENTE`.
- `nivelCombustivel`: 0 a 4.

### Atualização do cabeçalho (`PUT`)

É um `PUT`: os campos de texto/data são gravados como enviados e `null` limpa o valor; `boxId`/`mecanicoLiderId` nulos limpam o vínculo. Campos com default (`prioridade`, `parcelas`, `diagnosticoValidado`, `aprovacaoStatus`, `desconto`, `descontoTipo`) são atualizados apenas quando enviados. Status e itens não são alterados por aqui.

```json
{
  "boxId": 2,
  "mecanicoLiderId": 5,
  "prioridade": "ALTA",
  "kmEntrada": 45000,
  "kmSaida": 45020,
  "diagnostico": "Amortecedores dianteiros desgastados",
  "diagnosticoValidado": true,
  "observacoes": "Revisão de 45 mil km",
  "observacoesInternas": "Trocar par dianteiro",
  "observacoesCliente": "Serviço aprovado por WhatsApp",
  "tipo": "corretiva",
  "canal": "whatsapp",
  "titulo": "Revisão e suspensão",
  "relatoCliente": "Barulho ao passar em lombadas",
  "previsaoEntrega": "2026-11-12T17:00:00-03:00",
  "nivelCombustivel": 2,
  "desconto": 10,
  "descontoTipo": "PERCENTUAL",
  "formaPagamento": "PIX",
  "parcelas": 1,
  "garantiaTipo": "90d-5000",
  "garantiaDias": 90,
  "garantiaKm": 5000,
  "aprovacaoStatus": "APROVADO",
  "aprovacaoCanal": "whatsapp",
  "aprovacaoEnviadaEm": "2026-11-10T10:00:00-03:00",
  "aprovacaoDecididaEm": "2026-11-10T10:30:00-03:00",
  "aprovacaoVisualizadaEm": "2026-11-10T10:05:00-03:00",
  "motivoCancelamento": null
}
```

`descontoTipo`: `VALOR` ou `PERCENTUAL`. `aprovacaoStatus`: `PENDENTE`, `APROVADO` ou `RECUSADO`.

### Mudança de status

```http
PATCH /ordens-servico/1/status
```

```json
{
  "status": "EM_EXECUCAO",
  "observacao": "Serviço iniciado"
}
```

Status aceitos: os cadastrados em `status_ordens_servico` (`ABERTA`, `EM_ORCAMENTO`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_PECA`, `EM_EXECUCAO`, `EM_TESTE`, `PRONTA`, `ENTREGUE`, `CANCELADA`; `CONCLUIDA` é legado inativo).

Efeitos automáticos:

- `PRONTA` ou `EM_TESTE` preenchem `pronta_em`.
- `ENTREGUE` ou `CONCLUIDA` preenchem `entregue_em` e `fechada_em`.
- `CANCELADA` preenche `cancelada_em` e `fechada_em`.

### Detalhe (resumo dos campos)

```json
{
  "id": 1,
  "numero": 1,
  "status": "ABERTA",
  "prioridade": "NORMAL",
  "clienteId": 1,
  "clienteNome": "Robertinho Alves",
  "clienteTelefone": "11999998888",
  "veiculoId": 10,
  "veiculoPlaca": "ABC1251",
  "veiculoModelo": "Gol",
  "boxId": null,
  "boxNome": null,
  "mecanicoLiderId": null,
  "mecanicoLiderNome": null,
  "tipo": "corretiva",
  "canal": "balcao",
  "titulo": "Revisão",
  "relatoCliente": "...",
  "observacoes": "...",
  "observacoesInternas": "...",
  "observacoesCliente": "...",
  "kmEntrada": 45000,
  "kmSaida": null,
  "nivelCombustivel": 2,
  "previsaoEntrega": null,
  "diagnostico": "...",
  "diagnosticoValidado": false,
  "desconto": 0,
  "descontoTipo": "VALOR",
  "formaPagamento": null,
  "parcelas": 1,
  "garantiaTipo": null,
  "garantiaDias": null,
  "garantiaKm": null,
  "aprovacaoStatus": null,
  "aprovacaoCanal": null,
  "aprovacaoEnviadaEm": null,
  "aprovacaoDecididaEm": null,
  "aprovacaoVisualizadaEm": null,
  "totalServicos": 0,
  "totalPecas": 0,
  "total": 0,
  "abertaEm": "2026-11-01T10:00:00Z",
  "prontaEm": null,
  "entregueEm": null,
  "canceladaEm": null,
  "fechadaEm": null,
  "motivoCancelamento": null,
  "servicos": [],
  "pecas": []
}
```

### Serviço da OS

```http
POST /ordens-servico/1/servicos
```

```json
{
  "servicoId": 3,
  "descricao": "Troca de óleo",
  "quantidade": 1,
  "valorUnitario": 180.00,
  "mecanicoId": 5,
  "tempoEstimadoHoras": 1,
  "concluido": false,
  "desconto": 0,
  "descontoTipo": "VALOR"
}
```

`descricao`, `quantidade` (> 0) e `valorUnitario` (>= 0) são obrigatórios. O `PUT` usa o mesmo corpo.

### Peça da OS

```http
POST /ordens-servico/1/pecas
```

```json
{
  "pecaId": 7,
  "descricao": "Filtro de óleo",
  "quantidade": 1,
  "valorUnitario": 45.00,
  "cotada": true,
  "instalada": false,
  "desconto": 0,
  "descontoTipo": "VALOR",
  "unidade": "un"
}
```

`descricao`, `quantidade` (> 0) e `valorUnitario` (>= 0) são obrigatórios. O `PUT` usa o mesmo corpo.

### Histórico

```json
[
  {
    "id": 12,
    "statusAnterior": null,
    "statusNovo": "ABERTA",
    "descricao": "OS #1 criada",
    "usuario": "Carlos Silva",
    "criadoEm": "2026-11-01T10:00:00Z"
  }
]
```

---

## Catálogo de serviços

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/servicos?q=` | Lista/busca serviços |
| `POST` | `/servicos` | Cria serviço |
| `PUT` | `/servicos/{id}` | Atualiza |
| `DELETE` | `/servicos/{id}` | Desativa |
| `GET` | `/servicos/{id}/pecas-sugeridas` | Peças sugeridas |
| `PUT` | `/servicos/{id}/pecas-sugeridas` | Substitui peças sugeridas |
| `GET` | `/mecanicos` | Mecânicos ativos que podem ser responsáveis por uma OS |

### Criação de serviço

```json
{
  "codigo": "SV-010",
  "nome": "Alinhamento",
  "descricao": "Alinhamento de direção",
  "categoria": "Suspensão",
  "detalhe": "Inclui teste de rodagem",
  "tempoEstimadoHoras": 1,
  "garantiaTipo": "90d-5000",
  "precoBase": 150.00,
  "ativo": true
}
```

`nome` é obrigatório. Se `codigo` não for enviado, é gerado no padrão `SV-001` (sequencial por oficina). Código duplicado na oficina retorna `409`.

> `GET /servicos` **inclui serviços inativos** (`ativo=false`). Filtre no cliente se quiser exibir apenas ativos.

### Serviço (resposta)

```json
{
  "id": 3,
  "codigo": "SV-001",
  "nome": "Troca de óleo",
  "descricao": "Troca de óleo e filtro",
  "categoria": "Manutenção",
  "detalhe": "Óleo 5W30 sintético",
  "tempoEstimadoHoras": 1,
  "garantiaTipo": "90d-5000",
  "precoBase": 180.00,
  "ativo": true
}
```

### Peças sugeridas

```http
PUT /servicos/3/pecas-sugeridas
```

```json
{
  "pecaIds": [7, 8]
}
```

A resposta é a lista de `PecaDTO` atualizada. IDs de peças de outra oficina retornam `404`.

### Equipe técnica

`GET /mecanicos` retorna `MecanicoDTO[]` apenas dos usuários ativos com `executaOs = true`, ordenados por liderança e nome. O payload é enxuto e não expõe e-mail, telefone ou comissão:

```json
[
  {
    "id": 20,
    "nome": "João Mecânico",
    "cargo": "Mecânico",
    "especialidades": "Motor e freios",
    "lider": false,
    "corAgenda": "#2563eb",
    "fotoUrl": null
  }
]
```

O cadastro e a edição da equipe ficam em [`/usuarios`](#equipe-e-permissões).

---

## Peças e estoque

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/pecas?q=` | Lista/busca peças |
| `POST` | `/pecas` | Cria peça |
| `PUT` | `/pecas/{id}` | Atualiza |
| `DELETE` | `/pecas/{id}` | Desativa |
| `POST` | `/pecas/{id}/movimentacoes` | Movimenta estoque |
| `GET` | `/pecas/{id}/movimentacoes?limite=200` | Histórico da peça |
| `GET` | `/estoque/movimentacoes?limite=200` | Histórico geral da oficina |

### Criação de peça

```json
{
  "codigo": "PC-010",
  "nome": "Filtro de óleo",
  "descricao": "Filtro de óleo do motor",
  "fabricante": "Tecfil",
  "oem": "06A115561B",
  "categoria": "Filtros",
  "aplicacao": "Gol 1.0",
  "unidade": "un",
  "precoCusto": 25.00,
  "precoVenda": 45.00,
  "estoqueAtual": 10,
  "estoqueMinimo": 3,
  "estoqueMaximo": 30,
  "localizacao": "Corredor A3",
  "fornecedorId": 2,
  "observacao": null,
  "codigoBarras": "7891234567890",
  "garantiaDias": 90,
  "ncm": "84212300",
  "ativo": true
}
```

`nome` é obrigatório. O código é gerado como `PC-001` quando omitido.

> `GET /pecas` **inclui peças inativas** (`ativo=false`). Filtre no cliente se quiser exibir apenas ativas.

### Resposta

`PecaDTO` inclui os campos acima mais:

- `estoqueReservado`
- `estoqueDisponivel` (calculado)
- `ativo`

O `PUT` não aceita `estoqueAtual`: alterações de saldo devem passar por movimentação. O estoque inicial só é aceito na criação.

### Movimentação

```http
POST /pecas/7/movimentacoes
```

```json
{
  "tipo": "entrada",
  "quantidade": 5,
  "custoUnitario": 26.50,
  "fornecedorId": 2,
  "pedidoCompraId": null,
  "documento": "NF 1234",
  "observacao": "Compra de reposição"
}
```

| `tipo` | Efeito |
| --- | --- |
| `entrada` | Soma ao saldo (`direcao=1`) |
| `devolucao` | Soma ao saldo (`direcao=1`) |
| `saida` | Subtrai do saldo (`direcao=-1`); rejeita saldo negativo |
| `ajuste` | `quantidade` é a **nova contagem física**; aplica a diferença |

Um `ajuste` igual ao saldo atual é rejeitado com `422 REGRA_DE_NEGOCIO`.

Resposta:

```json
{
  "id": 40,
  "pecaId": 7,
  "pecaCodigo": "PC-001",
  "tipo": "entrada",
  "quantidade": 5,
  "direcao": 1,
  "saldoAnterior": 10,
  "saldoPosterior": 15,
  "saldoAtual": 15,
  "custoUnitario": 26.50,
  "documento": "NF 1234",
  "observacao": "Compra de reposição",
  "criadoEm": "2026-11-10T12:00:00Z"
}
```

`limite` no histórico é limitado entre 1 e 500.

---

## Vistoria da OS

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/ordens-servico/{numero}/vistoria` | Retorna a vistoria ou uma estrutura vazia |
| `PUT` | `/ordens-servico/{numero}/vistoria` | Cria/substitui a vistoria |

```json
{
  "clienteCiente": true,
  "pertences": "Estepe, macaco e triângulo",
  "observacoes": "Veículo com riscos no para-choque",
  "realizadaEm": "2026-11-10T09:30:00-03:00",
  "danos": [
    {
      "area": "para-choque-dianteiro",
      "tipo": "risco",
      "observacao": "Risco leve do lado esquerdo"
    }
  ],
  "fotos": [
    {
      "url": "https://storage.exemplo.com/os/1/foto1.jpg",
      "storagePath": "os/1/foto1.jpg",
      "legenda": "Frente"
    }
  ]
}
```

Regras: uma vistoria por OS; área + tipo de dano não podem repetir na mesma vistoria; `url` da foto é obrigatória.

---

## Fornecedores

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/fornecedores?incluirInativos=true` | Lista da oficina; `incluirInativos=false` mostra só ativos |
| `GET` | `/fornecedores/{id}` | Detalhe |
| `POST` | `/fornecedores` | Cria |
| `PUT` | `/fornecedores/{id}` | Atualiza |
| `DELETE` | `/fornecedores/{id}` | Desativa |

```json
{
  "nome": "Auto Peças Central",
  "razaoSocial": "Auto Peças Central Ltda",
  "cnpj": "11222333000144",
  "email": "vendas@central.com",
  "telefone": "1133334444",
  "contato": "Marcos",
  "leadTimeDias": 3,
  "endereco": "Rua das Peças, 100",
  "observacoes": "Entrega às terças",
  "ativo": true
}
```

`nome` é obrigatório. Nome e CNPJ são únicos por oficina.

---

## Pedidos de compra

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/pedidos-compra?status=` | Lista |
| `GET` | `/pedidos-compra/{id}` | Detalhe |
| `POST` | `/pedidos-compra` | Cria em `RASCUNHO` |
| `PUT` | `/pedidos-compra/{id}` | Atualiza cabeçalho/itens |
| `PATCH` | `/pedidos-compra/{id}/status` | Muda status; ao receber, dá entrada no estoque |
| `DELETE` | `/pedidos-compra/{id}` | Exclui rascunho ou cancelado |

### Criação

```json
{
  "fornecedorId": 2,
  "previsaoEntrega": "2026-11-15",
  "notaFiscal": null,
  "observacoes": "Reposição de filtros",
  "itens": [
    {
      "pecaId": 7,
      "descricao": "Filtro de óleo",
      "quantidade": 10,
      "custoUnitario": 25.00
    }
  ]
}
```

Regras:

- `fornecedorId` obrigatório e da mesma oficina.
- Ao menos um item.
- O número é sequencial por oficina (`Pedido #1`, `#2`...) e imutável.
- `pecaId` pode ser nulo (item avulso); se informado, precisa existir na oficina.

### Status

| Status | Permitido |
| --- | --- |
| `RASCUNHO` | Inicial; pode editar e excluir |
| `ENVIADO` | Pode editar itens; pode receber ou cancelar |
| `RECEBIDO` | Estado final; não altera itens nem status |
| `CANCELADO` | Estado final; não altera status; pode excluir |

A edição (`PUT`) não trava pelo status, mas os **itens** de um pedido `RECEBIDO` não podem ser alterados. A mudança de status é bloqueada a partir de `RECEBIDO` e `CANCELADO`.

```http
PATCH /pedidos-compra/1/status
```

```json
{ "status": "RECEBIDO" }
```

Ao entrar em `RECEBIDO`:

- cada item com `quantidadeRecebida < quantidade` gera uma movimentação de `entrada`;
- usa o custo unitário do item e o fornecedor do pedido;
- atualiza `quantidadeRecebida` para o total;
- preenche `recebidoEm`.

### Resposta

```json
{
  "id": 1,
  "numero": 1,
  "fornecedorId": 2,
  "fornecedorNome": "Auto Peças Central",
  "status": "RASCUNHO",
  "dataPedido": "2026-11-10",
  "previsaoEntrega": "2026-11-15",
  "recebidoEm": null,
  "notaFiscal": null,
  "observacoes": "Reposição de filtros",
  "criadoPor": 5,
  "criadoEm": "2026-11-10T12:00:00Z",
  "atualizadoEm": "2026-11-10T12:00:00Z",
  "itens": [
    {
      "id": 1,
      "pecaId": 7,
      "descricao": "Filtro de óleo",
      "quantidade": 10,
      "custoUnitario": 25.00,
      "quantidadeRecebida": 0,
      "subtotal": 250.00
    }
  ],
  "total": 250.00
}
```

---

## Financeiro

### Pagamentos (contas a receber)

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/pagamentos?status=` | Lista |
| `GET` | `/pagamentos/{id}` | Detalhe |
| `POST` | `/pagamentos` | Cria |
| `PUT` | `/pagamentos/{id}` | Atualiza |
| `PATCH` | `/pagamentos/{id}/status` | Muda status |
| `DELETE` | `/pagamentos/{id}` | Exclui |

```json
{
  "ordemServicoId": 1,
  "orcamentoId": null,
  "clienteId": 1,
  "descricao": "Saldo da OS #1",
  "valor": 500.00,
  "formaPagamento": "PIX",
  "parcelas": 1,
  "status": "PENDENTE",
  "vencimento": "2026-11-20",
  "pagoEm": null,
  "observacoes": null
}
```

`valor` (> 0) é obrigatório. Status: `PENDENTE`, `PAGO`, `CANCELADO`, `ESTORNADO`.

### Despesas (contas a pagar)

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/despesas?status=` | Lista |
| `GET` | `/despesas/{id}` | Detalhe |
| `POST` | `/despesas` | Cria |
| `PUT` | `/despesas/{id}` | Atualiza |
| `PATCH` | `/despesas/{id}/status` | Muda status |
| `DELETE` | `/despesas/{id}` | Exclui |

```json
{
  "fornecedorId": 2,
  "descricao": "Compra de filtros",
  "categoria": "Peças",
  "valor": 250.00,
  "status": "PENDENTE",
  "vencimento": "2026-11-20",
  "pagoEm": null,
  "observacoes": null
}
```

`descricao` e `valor` (> 0) são obrigatórios. Status: `PENDENTE`, `PAGO`, `CANCELADO`.

### Mudança de status

```http
PATCH /pagamentos/1/status
```

```json
{ "status": "PAGO" }
```

O status é normalizado para maiúsculas e validado contra a lista do recurso.

---

## Agenda

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/agendamentos?status=&inicio=&fim=` | Lista; filtro opcional por período |
| `GET` | `/agendamentos/{id}` | Detalhe |
| `POST` | `/agendamentos` | Cria |
| `PUT` | `/agendamentos/{id}` | Atualiza |
| `PATCH` | `/agendamentos/{id}/status` | Muda status |
| `DELETE` | `/agendamentos/{id}` | Exclui |

```json
{
  "clienteId": 1,
  "veiculoId": 10,
  "servicoId": 3,
  "mecanicoId": 5,
  "boxId": 1,
  "ordemServicoId": null,
  "titulo": "Revisão de 45 mil km",
  "descricao": "Cliente aguarda no local",
  "inicio": "2026-11-12T08:00:00-03:00",
  "fim": "2026-11-12T10:00:00-03:00",
  "status": "AGENDADO"
}
```

Regras:

- `titulo`, `inicio` e `fim` são obrigatórios.
- `fim` precisa ser posterior a `inicio`.
- Status: `AGENDADO`, `CONFIRMADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`, `NAO_COMPARECEU`.

`inicio`/`fim` no filtro são `OffsetDateTime` ISO-8601.

---

## Notificações

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/notificacoes?somenteNaoLidas=&limite=` | Lista da oficina/usuário |
| `GET` | `/notificacoes/nao-lidas/contador` | Contador |
| `POST` | `/notificacoes` | Cria |
| `PATCH` | `/notificacoes/{id}/lida` | Marca uma como lida |
| `PATCH` | `/notificacoes/lidas` | Marca todas como lidas |
| `DELETE` | `/notificacoes/{id}` | Exclui |

```json
{
  "usuarioId": 5,
  "tipo": "OS",
  "titulo": "OS #1 pronta",
  "mensagem": "O veículo está pronto para retirada.",
  "link": "/ordens-de-servico/1"
}
```

`titulo` é obrigatório. `limite` padrão `100`, limitado entre 1 e 500. Contador:

```json
{ "naoLidas": 3 }
```

---

## Enums e status

### Prioridade da OS

`BAIXA`, `NORMAL`, `ALTA`, `URGENTE`

### Status da OS

`ABERTA`, `EM_ORCAMENTO`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_PECA`, `EM_EXECUCAO`, `EM_TESTE`, `PRONTA`, `ENTREGUE`, `CANCELADA`

`CONCLUIDA` existe como legado inativo.

### Aprovação da OS

`PENDENTE`, `APROVADO`, `RECUSADO`

### Desconto

`VALOR`, `PERCENTUAL`

### Movimentação de estoque

`entrada`, `devolucao`, `saida`, `ajuste`

### Pedido de compra

`RASCUNHO`, `ENVIADO`, `RECEBIDO`, `CANCELADO`

### Pagamento

`PENDENTE`, `PAGO`, `CANCELADO`, `ESTORNADO`

### Despesa

`PENDENTE`, `PAGO`, `CANCELADO`

### Agendamento

`AGENDADO`, `CONFIRMADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `CANCELADO`, `NAO_COMPARECEU`

---

## Formato de erro

```json
{
  "codigo": "VALIDACAO",
  "mensagem": "Dados inválidos",
  "detalhes": ["Nome é obrigatório", "Telefone é obrigatório"],
  "timestamp": "2026-11-10T12:00:00Z"
}
```

| HTTP | `codigo` | Causa |
| --- | --- | --- |
| 403 | `ACESSO_NEGADO` | JWT sem usuário local/oficina, usuário inativo ou perfil sem permissão |
| 404 | `NAO_ENCONTRADO` | Recurso fora da oficina ou inexistente |
| 409 | `CONFLITO` | CPF, placa, chassi, código, e-mail, CNPJ ou número duplicado |
| 422 | `VALIDACAO` | Falha de Bean Validation |
| 422 | `REGRA_DE_NEGOCIO` | Estoque insuficiente, status inválido, pedido já recebido, e-mail duplicado etc. |
| 503 | `SERVICO_INDISPONIVEL` | Dependência externa não configurada (ex.: `SUPABASE_SERVICE_ROLE_KEY`) |
| 500 | `ERRO_INTERNO` | Falha inesperada |
