# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão geral do projeto

RestauranteAPI_Java é uma API REST em Spring Boot 4.1.0 (Java 21), com MongoDB, para um restaurante italiano. **Módulo implementado**: registro e autenticação com autorização por papel (`CLIENTE`, `FUNCIONARIO`, `ADMIN`) via JWT stateless (JJWT). **Módulos planejados**: cardápio (com categorias italianas) e pedidos (ver `src/main/java/com/example/Restaurante/docs/roadmap.md`).

## Comandos

- Rodar: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Build: `./mvnw clean install`
- Testes: `./mvnw test`
- Teste específico: `./mvnw test -Dtest=NomeDaClasse` ou `./mvnw test -Dtest=NomeDaClasse#nomeDoMetodo`
- Empacotar: `./mvnw clean package`

MongoDB via Docker Compose: `docker compose up -d` (mongo:7, porta 27017, sem auth, volume `mongodb_data`). URI: `mongodb://localhost:27017/restaurante` (`application.properties`).

## Arquitetura

Pacote raiz: `com.example.Restaurante`.

- **`config/`** — `SecurityConfig` (`@EnableMethodSecurity`, stateless JWT, libera só `POST /clientes` + `/auth/login`); `PasswordEncoderConfig` (BCrypt); `AdminSeedRunner` (seed ADMIN inicial).
- **`controller/`** — `AuthController` (`POST /auth/login`, `GET /auth/me`); `RegistroController` (`POST /clientes` público; `GET /clientes`, `POST /clientes/funcionarios` restritos por papel).
- **`document/`** — `Registro` (id, nome, email único, senha hash, papel, dataCriacao); `Papel` (enum `CLIENTE`/`FUNCIONARIO`/`ADMIN`, todos em uso).
- **`dto/`** — records com Bean Validation (`@NotBlank`, `@Email`, `@Size`); `RegistroCadastroRequest`, `FuncionarioCadastroRequest`, `Registro` (response).
- **`exception/`** — `GlobalExceptionHandler` mapeia `EmailJaCadastradoException`→409, `ValidacaoException`→400, `CredenciaisInvalidasException`→401, `AccessDeniedException`→403.
- **`repository/`** — `RegistroMongoTemplate` (usa `MongoTemplate`, distribui por coleção conforme papel: `clientes`, `funcionarios`, `admins`).
- **`security/`** — `JwtService` (JWT HS256 via JJWT), `JwtAuthenticationFilter`, `RegistroUserDetails(Service)`.
- **`service/`** — `AuthService` (login + geração de token), `RegistroService` (cadastro cliente + funcionário/admin).

## Fluxo de autenticação

1. **Cadastro de cliente** `POST /clientes` (público): valida DTO, checa e-mail duplicado, hash BCrypt, salva em coleção `clientes` com `Papel.CLIENTE`.
2. **Cadastro de funcionário/admin** `POST /clientes/funcionarios` (requer `ADMIN`): análogo, mas em coleção `funcionarios`/`admins` conforme papel informado (validação `@PreAuthorize`).
3. **Seed do ADMIN** roda no startup (`AdminSeedRunner`): se não houver admin, cria um via `app.admin-seed.*` properties.
4. **Login** `POST /auth/login` (público): autentica via `AuthenticationManager`, gera JWT HS256 24h, retorna token + dados do `Registro`.
5. **Requisições protegidas**: envia header `Authentication: <token>`. `JwtAuthenticationFilter` valida JWT e popula `SecurityContextHolder`.

## Pontos de atenção

- `jwt.secret` e credenciais do admin (`app.admin-seed.*`) hardcoded em `application.properties` (dev only) — migrar para env vars em produção.
- Header não-padrão: `Authentication` em vez de `Authorization: Bearer` — considerar padronizar se houver clientes externos.
- `Passos/` e `explicacoes/` documentam design antigo (cookie/sessão) — desatualizadas.
