# Cardápio: Categorias e Pratos

Documentação do módulo de cardápio da API Restaurante, implementando gerenciamento de categorias italianas e seus pratos associados.

## Visão geral

O módulo de cardápio permite:
- **Clientes (público)**: visualizar categorias e pratos disponíveis
- **Admin**: gerenciar categorias e pratos (CRUD), controlar disponibilidade

Estrutura de duas entidades: `Categoria` (contêiner) → `Prato` (item com preço).

---

## Arquitetura

### Documents (MongoDB)

#### `Categoria`
```java
@Document(collection = "categorias")
- id: String (ObjectId automático)
- nome: String (único, indexado)
- descricao: String
- dataCriacao: Instant
```

Exemplo: `"Risoto"`, `"Pasta"`, `"Sobremesa"`, etc.

#### `Prato`
```java
@Document(collection = "pratos")
- id: String (ObjectId automático)
- nome: String
- descricao: String
- preco: BigDecimal
- categoriaId: String (referência a Categoria.id)
- disponivel: boolean (true por padrão)
- imagemUrl: String (opcional)
- dataCriacao: Instant
```

Exemplo: `"Risoto ai Funghi"`, preço `25.50`, categoria `risoto-id`, disponível `true`.

---

### DTOs

#### Request/Response Categoria

**`CategoriaRequest` (POST/PUT)**
```
nome: @NotBlank String
descricao: @NotBlank String
```

**`CategoriaResponse` (GET/POST/PUT)**
```
id: String
nome: String
descricao: String
```
Nota: `dataCriacao` não é exposto (interno).

#### Request/Response Prato

**`PratoRequest` (POST/PUT)**
```
nome: @NotBlank String
descricao: @NotBlank String
preco: @NotNull BigDecimal (> 0)
categoriaId: @NotBlank String (deve existir)
imagemUrl: String (opcional)
```

**`PratoResponse` (GET/POST/PUT/PATCH)**
```
id: String
nome: String
descricao: String
preco: BigDecimal
categoriaId: String
disponivel: boolean
imagemUrl: String (pode ser null)
```
Nota: `dataCriacao` não é exposto.

---

### Services

#### `CategoriaService`

**Dependências:**
- `CategoriaMongoTemplate`: acesso a dados categorias
- `PratoMongoTemplate`: validação de pratos associados

**Operações:**

1. **`criar(CategoriaRequest)`** → `CategoriaResponse`
   - Valida: nome único (lança `ValidacaoException` se duplicado)
   - Cria `Categoria` com `dataCriacao = Instant.now()`
   - Retorna response

2. **`editar(String id, CategoriaRequest)`** → `CategoriaResponse`
   - Busca categoria (404 se não existe)
   - Valida nome único *se foi modificado* (permite reeditar mesmo nome)
   - Atualiza nome + descricao
   - Retorna response

3. **`listarTodas()`** → `List<CategoriaResponse>`
   - Retorna todas as categorias (sem paginação)

4. **`buscarPorId(String id)`** → `CategoriaResponse`
   - Busca por ID (404 se não existe)
   - Retorna response

5. **`remover(String id)`** → `void`
   - Valida: categoria existe (404 se não)
   - **Bloqueia**: se existem pratos com essa categoria (lança `CategoriaEmUsoException`)
   - Remove categoria

#### `PratoService`

**Dependências:**
- `PratoMongoTemplate`: acesso a dados pratos
- `CategoriaMongoTemplate`: validação de categoria

**Operações:**

1. **`criar(PratoRequest)`** → `PratoResponse`
   - Valida: categoria existe (404 se não)
   - Cria `Prato` com `disponivel = true`, `dataCriacao = Instant.now()`
   - Retorna response

2. **`editar(String id, PratoRequest)`** → `PratoResponse`
   - Busca prato (404 se não existe)
   - Valida: categoria existe (404 se não)
   - Atualiza: nome, descricao, preco, categoriaId, imagemUrl
   - **Não atualiza**: disponivel (use PATCH para isso)
   - Retorna response

3. **`listarTodos()`** → `List<PratoResponse>`
   - Retorna todos os pratos (sem paginação)

4. **`listarPorCategoria(String categoriaId)`** → `List<PratoResponse>`
   - Retorna pratos filtrados por categoria (pode estar vazia)
   - Não valida se categoria existe (cliente responsável)

5. **`buscarPorId(String id)`** → `PratoResponse`
   - Busca por ID (404 se não existe)
   - Retorna response

6. **`alternarDisponibilidade(String id)`** → `PratoResponse`
   - Busca prato (404 se não existe)
   - Inverte flag `disponivel` (true → false, false → true)
   - Retorna response

7. **`remover(String id)`** → `void`
   - Valida: prato existe (404 se não)
   - Remove prato

---

### Repositories (MongoTemplate)

#### `CategoriaMongoTemplate`

```java
interface/class
- findAll() → List<Categoria>
- findById(id) → Optional<Categoria>
- existsByNome(nome) → boolean
- existsById(id) → boolean
- save(categoria) → Categoria (insert ou update)
- deleteById(id) → void
```

Implementa busca por nome único com MongoDB query.

#### `PratoMongoTemplate`

```java
interface/class
- findAll() → List<Prato>
- findById(id) → Optional<Prato>
- findByCategoriaId(categoriaId) → List<Prato>
- existsByCategoriaId(categoriaId) → boolean
- existsById(id) → boolean
- save(prato) → Prato (insert ou update)
- deleteById(id) → void
```

Implementa busca por categoria com MongoDB query.

---

### Controllers

#### `CategoriaController`

**Base:** `GET /categorias`

| Método | Endpoint | Auth | Request | Response | Status |
|--------|----------|------|---------|----------|--------|
| GET | `/` | — | — | `List<CategoriaResponse>` | 200 |
| GET | `/{id}` | — | `id: String` | `CategoriaResponse` | 200, 404 |
| POST | `/` | ADMIN | `CategoriaRequest` | `CategoriaResponse` | 201, 400, 409 |
| PUT | `/{id}` | ADMIN | `id, CategoriaRequest` | `CategoriaResponse` | 200, 400, 404, 409 |
| DELETE | `/{id}` | ADMIN | `id: String` | — | 204, 404, 409 |

**Validação:**
- Request: `@Valid` no DTO (Bean Validation)
- Resposta: 404 `CategoriaNaoEncontradaException`
- Resposta: 409 `CategoriaEmUsoException` (DELETE com pratos)
- Resposta: 400 `ValidacaoException` (nome duplicado em POST/PUT)

#### `PratoController`

**Base:** `GET /pratos`

| Método | Endpoint | Auth | Request | Response | Status |
|--------|----------|------|---------|----------|--------|
| GET | `/` | — | `categoriaId: ?` | `List<PratoResponse>` | 200 |
| GET | `/{id}` | — | `id: String` | `PratoResponse` | 200, 404 |
| POST | `/` | ADMIN | `PratoRequest` | `PratoResponse` | 201, 400, 404 |
| PUT | `/{id}` | ADMIN | `id, PratoRequest` | `PratoResponse` | 200, 400, 404 |
| PATCH | `/{id}/disponibilidade` | ADMIN | `id: String` | `PratoResponse` | 200, 404 |
| DELETE | `/{id}` | ADMIN | `id: String` | — | 204, 404 |

**Query Parameters:**
- `GET /pratos?categoriaId=abc123` filtra por categoria

**Validação:**
- Request: `@Valid` no DTO
- Resposta: 404 `CategoriaNaoEncontradaException` (categoria não existe)
- Resposta: 404 `PratoNaoEncontradoException`
- Resposta: 400 `ValidacaoException` (validação DTO)

---

### Exceptions

#### `CategoriaNaoEncontradaException`
- Status HTTP: 404
- Trigger: `categoriaMongoTemplate.findById(id)` não encontra
- Usado por: `CategoriaService`, `PratoService` (validação categoria)

#### `CategoriaEmUsoException`
- Status HTTP: 409 (Conflict)
- Trigger: tentativa de remover categoria com pratos associados
- Usado por: `CategoriaService.remover()`

#### `PratoNaoEncontradoException`
- Status HTTP: 404
- Trigger: `pratoMongoTemplate.findById(id)` não encontra
- Usado por: `PratoService`

#### `ValidacaoException`
- Status HTTP: 400
- Trigger: violação de regra de negócio (nome duplicado em categoria)
- Usado por: `CategoriaService`

---

## Fluxos de uso

### Fluxo 1: Cliente lista cardápio

```
1. GET /categorias
   → [{ id: "cat-1", nome: "Pasta", descricao: "..." }, ...]
   
2. GET /pratos?categoriaId=cat-1
   → [{ id: "prato-1", nome: "Carbonara", preco: 18.50, disponivel: true, ... }, ...]
   
3. (Se quiser um prato específico)
   GET /pratos/prato-1
   → { id: "prato-1", nome: "Carbonara", ... }
```

### Fluxo 2: Admin cria nova categoria

```
1. POST /categorias
   Header: Authentication: <jwt-admin>
   Body: { nome: "Risoto", descricao: "Pratos de risoto" }
   → 201 { id: "cat-2", nome: "Risoto", descricao: "..." }
```

### Fluxo 3: Admin adiciona prato a uma categoria

```
1. POST /pratos
   Header: Authentication: <jwt-admin>
   Body: {
     nome: "Risoto ai Funghi",
     descricao: "Risoto com cogumelos",
     preco: 25.50,
     categoriaId: "cat-2",
     imagemUrl: "https://..."
   }
   → 201 { id: "prato-2", nome: "Risoto ai Funghi", ... }
```

### Fluxo 4: Admin edita prato

```
1. PUT /pratos/prato-1
   Header: Authentication: <jwt-admin>
   Body: { nome: "Carbonara Artesanal", descricao: "...", preco: 19.50, categoriaId: "cat-1", imagemUrl: "..." }
   → 200 { id: "prato-1", nome: "Carbonara Artesanal", ... }
```

### Fluxo 5: Admin alterna disponibilidade de prato

```
1. PATCH /pratos/prato-1/disponibilidade
   Header: Authentication: <jwt-admin>
   → 200 { id: "prato-1", ..., disponivel: false }
   
2. (Chamar novamente para ativar)
   PATCH /pratos/prato-1/disponibilidade
   → 200 { id: "prato-1", ..., disponivel: true }
```

### Fluxo 6: Admin tenta remover categoria em uso

```
1. DELETE /categorias/cat-1  (que tem pratos)
   Header: Authentication: <jwt-admin>
   → 409 { error: "CategoriaEmUsoException", message: "..." }
   
2. (Antes, remover pratos)
   DELETE /pratos/prato-1
   → 204 No Content
   
   DELETE /pratos/prato-2
   → 204 No Content
   
3. (Agora remover categoria)
   DELETE /categorias/cat-1
   → 204 No Content
```

---

## Notas de implementação

### Validação e constraints

- **Categoria.nome**: único (index), não-nulo, não-branco
- **Prato.categoriaId**: referência válida (validação em service, sem constraint FK)
- **Prato.preco**: > 0 (via Bean Validation)
- **Prato.imagemUrl**: opcional (pode ser null)

### Sem paginação

Atualmente, `listarTodas()` e `listarPorCategoria()` não pagina.
Se crescer, considere:
```java
Page<PratoResponse> listarPorCategoria(String categoriaId, Pageable pageable)
```
com `@PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC)`.

### Ordem (não implementada)

Não há `sort` explícito. MongoDB retorna ordem de inserção por padrão.
Considere adicionar: `.sort(new Sort(Sort.Direction.ASC, "nome"))`.

### Soft delete (não implementado)

Atualmente, DELETE é physical. Se precisar auditoria, adicione flag `deletado: boolean`.

---

## Testes

Testes unitários em `src/test/java/com/example/Restaurante/service/`:

- **`CategoriaServiceTest`**: testes de `CategoriaService` (criar, editar, remover, validações)
- **`PratoServiceTest`**: testes de `PratoService` (criar, editar, disponibilidade, validações)

Executar: `mvn test -Dtest=CategoriaServiceTest` ou `mvn test -Dtest=PratoServiceTest#nomeDoMetodo`

---

## Endpoints resumo

```
[PUBLIC]
GET    /categorias                  - Listar categorias
GET    /categorias/{id}             - Buscar categoria por ID
GET    /pratos                      - Listar pratos (filtro: ?categoriaId=)
GET    /pratos/{id}                 - Buscar prato por ID

[ADMIN]
POST   /categorias                  - Criar categoria
PUT    /categorias/{id}             - Editar categoria
DELETE /categorias/{id}             - Remover categoria

POST   /pratos                      - Criar prato
PUT    /pratos/{id}                 - Editar prato
PATCH  /pratos/{id}/disponibilidade - Alterar disponibilidade
DELETE /pratos/{id}                 - Remover prato
```
