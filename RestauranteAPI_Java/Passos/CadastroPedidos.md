# Pedidos: Gestão de Pedidos de Clientes

Documentação do módulo de Pedidos da API Restaurante, implementando criação, listagem e gerenciamento de pedidos de clientes.

## Visão geral

O módulo de Pedidos permite:
- **Clientes (autenticados)**: criar pedidos, visualizar seus próprios pedidos
- **Funcionários/Admin**: visualizar todos os pedidos, gerenciar status de entrega

Estrutura central: `Pedido` (contêiner) → `ItemPedido` (item com prato e quantidade, com snapshot de preço no momento do pedido para proteger o histórico).

Ciclo de vida de um pedido: `PENDENTE` → `EM_PREPARO` → `PRONTO` → `ENTREGUE`, com possibilidade de `CANCELADO` em qualquer estado não-terminal.

---

## Arquitetura

### Documents (MongoDB)

#### `PedidoStatus` (Enum)
```java
enum PedidoStatus {
    PENDENTE, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO
}
```

Máquina de estados:
```
PENDENTE ──→ EM_PREPARO ──→ PRONTO ──→ ENTREGUE (terminal)
   ↓            ↓            ↓
   └─ CANCELADO ─┘────────────┘ (terminal, saída lateral)
```

O método `podeTransicionarPara(novoStatus)` valida transições permitidas.

#### `ItemPedido` (POJO embutido, sem `@Document`)
```java
- pratoId: String (referência a Prato.id)
- nomePrato: String (snapshot do Prato.nome no momento do pedido)
- precoUnitario: BigDecimal (snapshot do Prato.preco no momento do pedido)
- quantidade: int
- subtotal: BigDecimal (precoUnitario * quantidade)
```

**Nota**: snapshot de nome e preço protege o histórico — se o preço do prato mudar depois, o pedido já gravado não é afetado.

#### `Pedido`
```java
@Document(collection = "pedidos")
- id: String (ObjectId automático)
- clienteId: String (referência a Registro.id, coleção "clientes")
- itens: List<ItemPedido>
- valorTotal: BigDecimal (soma dos subtotais dos itens)
- status: PedidoStatus (começa em PENDENTE)
- dataCriacao: Instant
- dataAtualizacao: Instant (atualizado quando status muda)
```

Exemplo:
```
{
  id: "ped-123",
  clienteId: "cliente-456",
  itens: [
    { pratoId: "prato-1", nomePrato: "Carbonara", precoUnitario: 18.50, quantidade: 2, subtotal: 37.00 },
    { pratoId: "prato-2", nomePrato: "Tiramisu", precoUnitario: 8.00, quantidade: 1, subtotal: 8.00 }
  ],
  valorTotal: 45.00,
  status: PENDENTE,
  dataCriacao: 2026-09-15T10:30:00Z,
  dataAtualizacao: 2026-09-15T10:30:00Z
}
```

---

### DTOs

#### Request: Criar Pedido

**`PedidoItemRequest` (item dentro do pedido)**
```
pratoId: @NotBlank String (ID do prato a adicionar)
quantidade: @NotNull @Min(1) Integer (quantidade desejada)
```

**`PedidoRequest` (corpo do POST /pedidos)**
```
itens: @NotEmpty @Valid List<PedidoItemRequest> (pelo menos um item)
```

Exemplo de requisição:
```json
{
  "itens": [
    { "pratoId": "prato-1", "quantidade": 2 },
    { "pratoId": "prato-2", "quantidade": 1 }
  ]
}
```

#### Response: Pedido

**`PedidoItemResponse` (item dentro da resposta)**
```
pratoId: String
nomePrato: String (snapshot)
precoUnitario: BigDecimal (snapshot)
quantidade: int
subtotal: BigDecimal
```

**`PedidoResponse` (resposta de GET/POST/PATCH)**
```
id: String
clienteId: String
itens: List<PedidoItemResponse>
valorTotal: BigDecimal
status: PedidoStatus
dataCriacao: Instant
dataAtualizacao: Instant
```

Exemplo de resposta:
```json
{
  "id": "ped-123",
  "clienteId": "cliente-456",
  "itens": [
    { "pratoId": "prato-1", "nomePrato": "Carbonara", "precoUnitario": 18.50, "quantidade": 2, "subtotal": 37.00 },
    { "pratoId": "prato-2", "nomePrato": "Tiramisu", "precoUnitario": 8.00, "quantidade": 1, "subtotal": 8.00 }
  ],
  "valorTotal": 45.00,
  "status": "PENDENTE",
  "dataCriacao": "2026-09-15T10:30:00Z",
  "dataAtualizacao": "2026-09-15T10:30:00Z"
}
```

#### Request: Atualizar Status

**`PedidoStatusUpdateRequest` (corpo do PATCH /pedidos/{id}/status)**
```
status: @NotNull PedidoStatus (novo status desejado)
```

Exemplo:
```json
{
  "status": "EM_PREPARO"
}
```

---

### Services

#### `PedidoService`

**Dependências:**
- `PedidoMongoTemplate`: acesso a dados de pedidos
- `PratoMongoTemplate`: validação e busca de pratos

**Operações:**

1. **`criar(PedidoRequest, Authentication)`** → `PedidoResponse`
   - Extrai `clienteId` do JWT autenticado
   - Para cada item da requisição:
     - Busca o `Prato` por ID (404 `PratoNaoEncontradoException` se não existe)
     - Valida: prato está `disponivel=true` (409 `PratoIndisponivelException` se não)
     - Cria snapshot: copia `nome` e `preço` do prato
     - Calcula `subtotal = precoUnitario * quantidade`
   - Soma todos os subtotais em `valorTotal`
   - Cria `Pedido` com `status=PENDENTE`, `dataCriacao/dataAtualizacao=now()`
   - Salva e retorna response

2. **`listarTodos()`** → `List<PedidoResponse>`
   - Retorna todos os pedidos (sem paginação)
   - Uso: staff (funcionários/admin) para ver todos os pedidos

3. **`listarMeusPedidos(Authentication)`** → `List<PedidoResponse>`
   - Extrai `clienteId` do JWT autenticado
   - Filtra pedidos por `clienteId`
   - Retorna lista de pedidos do cliente

4. **`buscarPorId(String, Authentication)`** → `PedidoResponse`
   - Busca pedido por ID (404 `PedidoNaoEncontradoException` se não existe)
   - Valida acesso:
     - Se usuário é `CLIENTE`: permite apenas ver seu próprio pedido (403 `AccessDeniedException` se não é dono)
     - Se usuário é `FUNCIONARIO/ADMIN`: sempre permite
   - Retorna response

5. **`atualizarStatus(String, PedidoStatusUpdateRequest)`** → `PedidoResponse`
   - Busca pedido por ID (404 se não existe)
   - Valida transição: `pedido.getStatus().podeTransicionarPara(novoStatus)` (409 `TransicaoStatusInvalidaException` se inválida)
   - Atualiza `status` e `dataAtualizacao`
   - Salva e retorna response

---

### Repositories (MongoTemplate)

#### `PedidoMongoTemplate`

```java
interface/class
- save(pedido) → Pedido (insert ou update)
- findById(id) → Optional<Pedido>
- findAll() → List<Pedido>
- findByClienteId(clienteId) → List<Pedido>
```

**Nota:** Sem `deleteById()` — cancelamento é sempre lógico via `status=CANCELADO`. Pedidos nunca são eliminados fisicamente do histórico.

---

### Controllers

#### `PedidoController`

**Base:** `POST/GET /pedidos`

| Método | Endpoint | Auth | Request | Response | Status |
|--------|----------|------|---------|----------|--------|
| POST | `/` | CLIENTE | `PedidoRequest` | `PedidoResponse` | 201, 400, 404, 409 |
| GET | `/` | FUNCIONARIO/ADMIN | — | `List<PedidoResponse>` | 200 |
| GET | `/meus` | CLIENTE | — | `List<PedidoResponse>` | 200 |
| GET | `/{id}` | autenticado | `id: String` | `PedidoResponse` | 200, 403, 404 |
| PATCH | `/{id}/status` | FUNCIONARIO/ADMIN | `PedidoStatusUpdateRequest` | `PedidoResponse` | 200, 404, 409 |

**Validação:**
- Request: `@Valid` no DTO (Bean Validation)
- Resposta: 404 `PedidoNaoEncontradoException`
- Resposta: 404 `PratoNaoEncontradoException` (prato do item não existe)
- Resposta: 409 `PratoIndisponivelException` (prato existe mas `disponivel=false`)
- Resposta: 409 `TransicaoStatusInvalidaException` (transição de status inválida)
- Resposta: 403 `AccessDeniedException` (cliente tenta acessar pedido de outro cliente)
- Resposta: 400 `ValidacaoException` (validação DTO, ex: quantidade ≤ 0)

---

### Exceptions

#### `PedidoNaoEncontradoException`
- Status HTTP: 404
- Trigger: `pedidoMongoTemplate.findById(id)` não encontra
- Usado por: `PedidoService` (buscar, atualizar status)

#### `PratoIndisponivelException`
- Status HTTP: 409 (Conflict)
- Trigger: prato existe, mas `disponivel=false` no momento da criação do pedido
- Usado por: `PedidoService.criar()`

#### `TransicaoStatusInvalidaException`
- Status HTTP: 409 (Conflict)
- Trigger: `pedido.getStatus().podeTransicionarPara(novoStatus)` retorna false
- Usado por: `PedidoService.atualizarStatus()`

#### Reaproveitadas (sem nova classe)
- **`PratoNaoEncontradoException`** (404): pratoId do item não existe no banco
- **`AccessDeniedException`** do Spring Security (403): cliente tentando acessar pedido de outro cliente

---

## Fluxos de uso

### Fluxo 1: Cliente cria pedido com 2 itens

```
1. Login do cliente
   POST /auth/login
   Body: { email: "cliente@example.com", senha: "123456" }
   → 200 { token: "eyJhbGc...", registro: { id: "cliente-456", nome: "João", papel: "CLIENTE" } }

2. Criar pedido
   POST /pedidos
   Header: Authentication: eyJhbGc...
   Body: {
     itens: [
       { pratoId: "prato-1", quantidade: 2 },
       { pratoId: "prato-2", quantidade: 1 }
     ]
   }
   → 201 {
     id: "ped-123",
     clienteId: "cliente-456",
     itens: [
       { pratoId: "prato-1", nomePrato: "Carbonara", precoUnitario: 18.50, quantidade: 2, subtotal: 37.00 },
       { pratoId: "prato-2", nomePrato: "Tiramisu", precoUnitario: 8.00, quantidade: 1, subtotal: 8.00 }
     ],
     valorTotal: 45.00,
     status: "PENDENTE",
     dataCriacao: "2026-09-15T10:30:00Z",
     dataAtualizacao: "2026-09-15T10:30:00Z"
   }
```

### Fluxo 2: Cliente lista seus próprios pedidos

```
1. GET /pedidos/meus
   Header: Authentication: eyJhbGc...
   → 200 [
     {
       id: "ped-123",
       clienteId: "cliente-456",
       itens: [...],
       valorTotal: 45.00,
       status: "PENDENTE",
       ...
     },
     {
       id: "ped-124",
       clienteId: "cliente-456",
       itens: [...],
       valorTotal: 32.50,
       status: "ENTREGUE",
       ...
     }
   ]
```

### Fluxo 3: Admin/Funcionário lista todos os pedidos e avança status

```
1. Login de funcionário
   POST /auth/login
   Body: { email: "funcionario@example.com", senha: "123456" }
   → 200 { token: "eyJhbGc...", registro: { id: "func-789", papel: "FUNCIONARIO" } }

2. Listar todos os pedidos
   GET /pedidos
   Header: Authentication: eyJhbGc...
   → 200 [
     { id: "ped-123", clienteId: "cliente-456", status: "PENDENTE", ... },
     { id: "ped-124", clienteId: "cliente-789", status: "PENDENTE", ... }
   ]

3. Iniciar preparo do pedido ped-123
   PATCH /pedidos/ped-123/status
   Header: Authentication: eyJhbGc...
   Body: { status: "EM_PREPARO" }
   → 200 { id: "ped-123", ..., status: "EM_PREPARO", dataAtualizacao: "2026-09-15T10:35:00Z" }

4. Marcar como pronto
   PATCH /pedidos/ped-123/status
   Header: Authentication: eyJhbGc...
   Body: { status: "PRONTO" }
   → 200 { id: "ped-123", ..., status: "PRONTO", dataAtualizacao: "2026-09-15T10:40:00Z" }

5. Entregar pedido
   PATCH /pedidos/ped-123/status
   Header: Authentication: eyJhbGc...
   Body: { status: "ENTREGUE" }
   → 200 { id: "ped-123", ..., status: "ENTREGUE", dataAtualizacao: "2026-09-15T10:42:00Z" }
```

### Fluxo 4: Cliente tenta criar pedido com prato indisponível

```
1. POST /pedidos
   Header: Authentication: eyJhbGc... (cliente)
   Body: {
     itens: [
       { pratoId: "prato-indisponivel", quantidade: 1 }
     ]
   }
   → 409 { error: "PratoIndisponivelException", message: "Prato indisponível para pedido: prato-indisponivel" }
```

### Fluxo 5: Admin tenta transição de status inválida

```
1. (Pedido está em status ENTREGUE)
   PATCH /pedidos/ped-999/status
   Header: Authentication: eyJhbGc... (admin)
   Body: { status: "PENDENTE" }
   → 409 { error: "TransicaoStatusInvalidaException", message: "Transição de status inválida: ENTREGUE -> PENDENTE" }
```

### Fluxo 6: Cliente tenta acessar pedido de outro cliente

```
1. GET /pedidos/ped-456 (que pertence a outro cliente)
   Header: Authentication: eyJhbGc... (cliente diferente)
   → 403 { error: "AccessDeniedException", message: "Acesso negado" }

2. (Admin consegue acessar)
   GET /pedidos/ped-456
   Header: Authentication: eyJhbGc... (admin)
   → 200 { id: "ped-456", clienteId: "outro-cliente", ... }
```

---

## Notas de implementação

### Snapshot de Preço e Nome

Cada item do pedido armazena `nomePrato` e `precoUnitario` como snapshot (cópia no momento da criação). Isso garante que o histórico de pedidos não seja afetado se o preço ou nome do prato forem alterados depois. Exemplo: se você pedir uma pizza por R$ 35 hoje e o restaurante muda o preço para R$ 40 amanhã, seu pedido histórico continua com R$ 35.

### Sem Delete Físico

Não existe endpoint `DELETE /pedidos/{id}`. Cancelamento é sempre lógico: `PATCH /pedidos/{id}/status` com `status: CANCELADO`. Isso preserva o histórico completo de pedidos no banco.

### Sem Paginação

Atualmente, `listarTodos()` e `listarMeusPedidos()` não paginam. Se crescer o volume, considere:
```java
List<PedidoResponse> listarTodos(Pageable pageable)
```

### Autorização: `@PreAuthorize` vs `SecurityConfig`

Não há nenhuma mudança necessária em `SecurityConfig`. Todas as rotas `/pedidos/**` caem no `anyRequest().authenticated()` existente. O controle por papel (CLIENTE vs FUNCIONARIO/ADMIN) é feito inteiramente via `@PreAuthorize` nos métodos do controller, que funcionam porque `@EnableMethodSecurity` já está habilitado. Isso segue exatamente o padrão já usado em `RegistroController` e `CategoriaController`.

### Possíveis Melhorias Futuras

1. **Cancelamento pelo próprio cliente**: adicionar `DELETE /pedidos/{id}` ou `PATCH /pedidos/{id}/cancelamento` para que cliente possa cancelar pedido ainda em `PENDENTE`. Não foi confirmado pelo usuário nesta entrega.

2. **Snapshot adicional**: armazenar também `clienteNome` como snapshot no pedido para que staff não dependa de buscar o cliente separadamente ao exibir pedidos em painel.

3. **Consolidação de itens duplicados**: se cliente enviar o mesmo `pratoId` duas vezes na mesma requisição, consolidar em um item com quantidade somada.

4. **Notificações**: integração com email/webhook quando status muda (ex: "seu pedido entrou em preparo").

---

## Testes

Testes unitários em `src/test/java/com/example/Restaurante/service/PedidoServiceTest.java`:

- **`testCriarComSucesso`**: cria pedido com 2 itens, valida snapshot de nome/preço e cálculo de valorTotal
- **`testCriarComPratoIndisponivel`**: tenta criar pedido com prato `disponivel=false` → `PratoIndisponivelException`
- **`testCriarComPratoInexistente`**: tenta criar pedido com pratoId que não existe → `PratoNaoEncontradoException`
- **`testListarTodos`**: staff lista todos os pedidos
- **`testListarPorCliente`**: cliente vê apenas seus próprios pedidos via `listarMeusPedidos`
- **`testBuscarPorIdComoDono`**: cliente consegue buscar seu próprio pedido
- **`testBuscarPorIdComoStaff`**: staff consegue buscar qualquer pedido
- **`testBuscarPorIdAcessoNegado`**: cliente tentando buscar pedido de outro cliente → `AccessDeniedException`
- **`testBuscarPedidoInexistente`**: buscar pedido que não existe → `PedidoNaoEncontradoException`
- **`testAtualizarStatusComSucesso`**: transição válida `PENDENTE → EM_PREPARO` funciona
- **`testAtualizarStatusTransicaoInvalida`**: tenta `ENTREGUE → PENDENTE` → `TransicaoStatusInvalidaException`
- **`testAtualizarStatusPedidoInexistente`**: atualizar status de pedido inexistente → `PedidoNaoEncontradoException`

Executar: `mvn test -Dtest=PedidoServiceTest` ou `mvn test -Dtest=PedidoServiceTest#testCriarComSucesso`

---

## Endpoints resumo

```
[CLIENTE - autenticado]
POST   /pedidos           - Criar novo pedido
GET    /pedidos/meus      - Listar meus pedidos

[QUALQUER USUÁRIO - autenticado]
GET    /pedidos/{id}      - Buscar pedido por ID (com validação de acesso)

[FUNCIONARIO/ADMIN]
GET    /pedidos            - Listar todos os pedidos
PATCH  /pedidos/{id}/status - Atualizar status de um pedido
```
