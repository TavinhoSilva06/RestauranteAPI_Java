# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão geral do projeto

RestauranteAPI_Java é uma API REST em Spring Boot 4.1.0 (Java 21), com MongoDB, para um restaurante italiano. **Módulos implementados**: (1) registro e autenticação com autorização por papel (`CLIENTE`, `FUNCIONARIO`, `ADMIN`) via JWT stateless (JJWT); (2) cardápio com categorias e pratos com validação e controle de disponibilidade. **Módulos planejados**: pedidos (ver `src/main/java/com/example/Restaurante/docs/roadmap.md`).

## Comandos

- Rodar: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Build: `./mvnw clean install`
- Testes: `./mvnw test`
- Teste específico: `./mvnw test -Dtest=NomeDaClasse` ou `./mvnw test -Dtest=NomeDaClasse#nomeDoMetodo`
- Empacotar: `./mvnw clean package`

MongoDB via Docker Compose: `docker compose up -d` (mongo:7, porta 27017, sem auth, volume `mongodb_data`). URI: `mongodb://localhost:27017/restaurante` (`application.properties`).

## Arquitetura

Pacote raiz: `com.example.Restaurante`.

- **`config/`** — `SecurityConfig` (`@EnableMethodSecurity`, stateless JWT, libera `POST /clientes` + `/auth/login` + `GET /categorias` + `GET /pratos`); `PasswordEncoderConfig` (BCrypt); `AdminSeedRunner` (seed ADMIN inicial).
- **`controller/`** — `AuthController` (`POST /auth/login`, `GET /auth/me`); `RegistroController` (`POST /clientes` público; `GET /clientes`, `POST /clientes/funcionarios` restritos); `CategoriaController` (`GET /categorias`, `GET /categorias/{id}` públicos; `POST/PUT/DELETE` requerem `ADMIN`); `PratoController` (`GET /pratos`, `GET /pratos/{id}` públicos com filtro por categoria; `POST/PUT/PATCH/DELETE` requerem `ADMIN`).
- **`document/`** — `Registro` (id, nome, email único, senha hash, papel, dataCriacao); `Papel` (enum `CLIENTE`/`FUNCIONARIO`/`ADMIN`); `Categoria` (id, nome único, descricao, dataCriacao); `Prato` (id, nome, descricao, preco, categoriaId, disponivel, imagemUrl, dataCriacao).
- **`dto/`** — records com Bean Validation; grupo autenticação: `RegistroCadastroRequest`, `FuncionarioCadastroRequest`; grupo cardápio: `CategoriaRequest`, `CategoriaResponse`, `PratoRequest`, `PratoResponse`.
- **`exception/`** — `GlobalExceptionHandler` mapeia: autenticação (`EmailJaCadastradoException`→409, `CredenciaisInvalidasException`→401); validação (`ValidacaoException`→400); acesso (`AccessDeniedException`→403); cardápio (`CategoriaNaoEncontradaException`, `CategoriaEmUsoException`, `PratoNaoEncontradoException`→404).
- **`repository/`** — `RegistroMongoTemplate` (usa `MongoTemplate`, distribui por coleção: `clientes`, `funcionarios`, `admins`); `CategoriaMongoTemplate` (coleção `categorias`, métodos: `findAll()`, `findById()`, `existsByNome()`, `save()`, `deleteById()`); `PratoMongoTemplate` (coleção `pratos`, métodos: `findAll()`, `findById()`, `findByCategoriaId()`, `existsByCategoriaId()`, `save()`, `deleteById()`).
- **`security/`** — `JwtService` (JWT HS256 via JJWT), `JwtAuthenticationFilter`, `RegistroUserDetails(Service)`.
- **`service/`** — `AuthService` (login + geração de token), `RegistroService` (cadastro cliente + funcionário/admin); `CategoriaService` (CRUD categorias, valida unicidade nome, bloqueia remoção se categoria em uso); `PratoService` (CRUD pratos, valida categoria existe, alterna disponibilidade via PATCH).

## Fluxo de autenticação

1. **Cadastro de cliente** `POST /clientes` (público): valida DTO, checa e-mail duplicado, hash BCrypt, salva em coleção `clientes` com `Papel.CLIENTE`.
2. **Cadastro de funcionário/admin** `POST /clientes/funcionarios` (requer `ADMIN`): análogo, mas em coleção `funcionarios`/`admins` conforme papel informado (validação `@PreAuthorize`).
3. **Seed do ADMIN** roda no startup (`AdminSeedRunner`): se não houver admin, cria um via `app.admin-seed.*` properties.
4. **Login** `POST /auth/login` (público): autentica via `AuthenticationManager`, gera JWT HS256 24h, retorna token + dados do `Registro`.
5. **Requisições protegidas**: envia header `Authentication: <token>`. `JwtAuthenticationFilter` valida JWT e popula `SecurityContextHolder`.

## Fluxo de cardápio

1. **Listar categorias** `GET /categorias` (público): retorna todas as categorias ativas ordenadas por dataCriacao.
2. **Buscar categoria** `GET /categorias/{id}` (público): retorna uma categoria por ID ou 404.
3. **Criar categoria** `POST /categorias` (requer `ADMIN`): valida nome único, cria `Categoria` com id automático e timestamp.
4. **Editar categoria** `PUT /categorias/{id}` (requer `ADMIN`): valida nome único (se mudou), atualiza descricao e nome.
5. **Remover categoria** `DELETE /categorias/{id}` (requer `ADMIN`): bloqueia se existem pratos com essa categoria (lança `CategoriaEmUsoException`).
6. **Listar pratos** `GET /pratos` (público) ou `GET /pratos?categoriaId=X` (público): retorna todos ou filtrados por categoria.
7. **Buscar prato** `GET /pratos/{id}` (público): retorna um prato por ID ou 404.
8. **Criar prato** `POST /pratos` (requer `ADMIN`): valida categoria existe, cria `Prato` com `disponivel=true` e timestamp.
9. **Editar prato** `PUT /pratos/{id}` (requer `ADMIN`): valida categoria existe, atualiza nome, descricao, preco, categoriaId, imagemUrl.
10. **Alterar disponibilidade** `PATCH /pratos/{id}/disponibilidade` (requer `ADMIN`): inverte flag `disponivel`.
11. **Remover prato** `DELETE /pratos/{id}` (requer `ADMIN`): remove prato.

## Pontos de atenção

- `jwt.secret` e credenciais do admin (`app.admin-seed.*`) hardcoded em `application.properties` (dev only) — migrar para env vars em produção.
- Header não-padrão: `Authentication` em vez de `Authorization: Bearer` — considerar padronizar se houver clientes externos.
- Categoria com pratos não pode ser removida: `CategoriaEmUsoException` previne integridade referencial (cardápio não tem constraint FK).
- `pratos` não valida duplicação de nome (ao contrário de `categorias`); comportamento pode mudar se houver requisito.
- `imagemUrl` em `Prato` é opcional (pode ser null); cliente deve tratar ausência.
