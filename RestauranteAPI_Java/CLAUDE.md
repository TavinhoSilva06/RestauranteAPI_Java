# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão geral do projeto

RestauranteAPI_Java é uma API REST em Spring Boot 4.1.0 (Java 21), com MongoDB. **Módulo implementado**: cadastro e autenticação de clientes com JWT stateless (via JJWT). **Módulos planejados mas não implementados**: cardápio e pedidos (ver `src/main/java/com/example/Restaurante/docs/roadmap.md`).

## Comandos

- Rodar: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Build: `./mvnw clean install`
- Testes: `./mvnw test`
- Teste específico: `./mvnw test -Dtest=NomeDaClasse` ou `./mvnw test -Dtest=NomeDaClasse#nomeDoMetodo`
- Empacotar: `./mvnw clean package`

MongoDB via Docker Compose: `docker compose up -d` (mongo:7, porta 27017, sem auth, volume `mongodb_data`). URI: `mongodb://localhost:27017/restaurante` (`application.properties`).

## Arquitetura

Pacote raiz: `com.example.Restaurante`.

- **`config/`** — `SecurityConfig` (stateless JWT, libera só `/clientes` + `/auth/login`, resto autenticado); `PasswordEncoderConfig` (BCrypt).
- **`controller/`** — `AuthController` (`POST /auth/login`, `GET /auth/me`); `ClienteController` (`POST /clientes`, público).
- **`document/`** — `Cliente` (id, nome, email único, senha hash, papel, dataCriacao); `Papel` (enum `CLIENTE`/`FUNCIONARIO`/`ADMIN` — só `CLIENTE` usado hoje).
- **`dto/`** — records com Bean Validation (`@NotBlank`, `@Email`, `@Size`).
- **`exception/`** — `GlobalExceptionHandler` mapeia `EmailJaCadastradoException`→409, `ValidacaoException`→400, `CredenciaisInvalidasException`→401.
- **`repository/`** — `ClienteRepository extends MongoRepository<Cliente, String>`.
- **`security/`** — `JwtService` (JWT HS256 via JJWT), `JwtAuthenticationFilter`, `ClienteUserDetails(Service)`.
- **`service/`** — `AuthService` (login + geração de token), `ClienteService` (cadastro).

## Fluxo de autenticação

1. **Cadastro** `POST /clientes` (público): valida via anotações no DTO, checa e-mail duplicado, hash BCrypt, salva com `Papel.CLIENTE`.
2. **Login** `POST /auth/login` (público): autentica via `AuthenticationManager`, gera JWT 24h, retorna token + `ClienteResponse`.
3. **Requisições protegidas**: registro envia header `Authentication: <token>`. `JwtAuthenticationFilter` valida e popula `SecurityContextHolder`.

## Pontos de atenção

- Sem autorização por papel (`Papel` tem 3 valores, nenhuma rota checa role).
- `jwt.secret` hardcoded em `application.properties` (valor de dev, commitado) — migrar para env vars em produção.
- `Passos/` e `explicacoes/` documentam design antigo (cookie/sessão) — desatualizadas.
- Header não-padrão: `Authentication` em vez de `Authorization: Bearer` — considerar padronizar se houver clientes externos.
