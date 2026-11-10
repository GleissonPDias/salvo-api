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
  "telefone": "(11) 99999-9999",
  "especialidades": null,
  "lider": true,
  "comissaoPercentual": null,
  "corAgenda": null,
  "fotoUrl": null
}
```

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
| `GET` | `/mecanicos` | Equipe técnica da oficina |

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

`GET /mecanicos` retorna `UsuarioLogadoDTO[]` dos usuários ativos da oficina.

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
| 403 | `ACESSO_NEGADO` | JWT sem usuário local/oficina |
| 404 | `NAO_ENCONTRADO` | Recurso fora da oficina ou inexistente |
| 409 | `CONFLITO` | CPF, placa, chassi, código, e-mail, CNPJ ou número duplicado |
| 422 | `VALIDACAO` | Falha de Bean Validation |
| 422 | `REGRA_DE_NEGOCIO` | Estoque insuficiente, status inválido, pedido já recebido etc. |
| 500 | `ERRO_INTERNO` | Falha inesperada |
