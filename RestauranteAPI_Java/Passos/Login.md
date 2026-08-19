# Login de Cliente

## Objetivo

Permitir que um cliente cadastrado autentique-se na API usando seu e-mail e senha, criando uma sessão para acessar recursos protegidos.

## Dados necessários

- E-mail (identifica a conta)
- Senha (conferida contra o hash armazenado)

## Regras de negócio

1. **Validação de credenciais**: o e-mail deve estar cadastrado e a senha deve ser a mesma fornecida no cadastro (conferida via BCrypt).
2. **Erro genérico**: se o e-mail não existir ou a senha estiver errada, a resposta é a mesma ("E-mail ou senha inválidos"), sem detalhar qual falha — protege contra enumeração de contas.
3. **Sessão baseada em cookie**: o login cria uma sessão HTTP identificada por um cookie `JSESSIONID`, que deve ser reenviado em requisições futuras para manter a autenticação.
4. **Validade**: a sessão persiste enquanto o navegador/cliente a mantém (padrão do Spring Security para sesões HTTP baseadas em memória).
5. **Logout**: disponível via `POST /logout` para encerrar a sessão.

## Fluxo passo a passo

1. O cliente envia e-mail e senha para `POST /auth/login`.
2. O sistema procura um cliente com esse e-mail no banco.
   - Se não encontrar ou a senha não conferir: responde 401 Unauthorized.
3. Se as credenciais forem válidas:
   - O sistema cria uma sessão HTTP.
   - Grava a autenticação no contexto de segurança.
   - Retorna a sessão em um cookie `JSESSIONID`.
4. O cliente guarda o cookie e o reenvia em requisições futuras (browsers fazem isso automaticamente).
5. Ao acessar `GET /auth/me` com o cookie válido, o sistema confirma a autenticação.
6. Para encerrar a sessão, o cliente envia `POST /auth/logout`, que invalida o cookie e limpa o contexto.

## O que fica fora deste fluxo

- Recuperação/troca de senha: não faz parte do login. É um fluxo à parte.
- Autenticação multi-fator (2FA): não está no escopo atual.
- Integração com provedores externos (OAuth, SAML, etc.): fora do escopo.
- Atualização de perfil do cliente após login: é uma ação separada (fora deste módulo).

## Estratégia técnica

Segue a arquitetura em camadas já adotada no projeto (`Controller → Service → Repository → Database`).

### Camadas e classes

| Camada | Classe | Responsabilidade |
|---|---|---|
| Security | `security/ClienteUserDetails.java` | Implementa `UserDetails`, envolvendo um `Cliente`; `username` = email, `password` = hash, `authority` = `ROLE_CLIENTE` ou `ROLE_FUNCIONARIO`, etc. |
| Security | `security/ClienteUserDetailsService.java` | Implementa `UserDetailsService`; busca `Cliente` por email no repository. |
| DTO | `dto/LoginRequest.java` | Record com `email`, `senha`. |
| Exception | `exception/CredenciaisInvalidasException.java` | Lançada quando email/senha não conferem. |
| Service | `service/AuthService.java` | `login(...)`: autentica via `AuthenticationManager`; em caso de erro, relança como `CredenciaisInvalidasException`; em caso de sucesso, salva o contexto na sessão HTTP e retorna `ClienteResponse`. `me(...)`: extrai dados do usuário autenticado pela sessão. |
| Controller | `controller/AuthController.java` | `POST /auth/login` e `GET /auth/me` (protegida por autenticação). `POST /auth/logout` é configurado automaticamente pelo Spring Security. |
| Config | `config/SecurityConfig.java` | Expõe `AuthenticationManager`; autoriza `POST /clientes` e `POST /auth/login` sem autenticação; configura logout. |
| Exception | `exception/GlobalExceptionHandler.java` | Mapeia `CredenciaisInvalidasException` → 401 Unauthorized. |

### Fluxo técnico da requisição

```
POST /auth/login {email, senha}
  → AuthController.login() chama AuthService.login(request, httpRequest, httpResponse)
    → AuthService monta UsernamePasswordAuthenticationToken(email, senha)
    → AuthenticationManager.authenticate(...) usa ClienteUserDetailsService para carregar o Cliente
      → ClienteUserDetailsService busca Cliente por email via ClienteRepository.findByEmail
      → PasswordEncoder valida o hash da senha
      → sucesso: Authentication carrega ClienteUserDetails como principal
      → falha: BadCredentialsException → capturada → relançada como CredenciaisInvalidasException
    → AuthService salva o Authentication no SecurityContext e persiste na sessão HTTP
      → Set-Cookie: JSESSIONID (browser guarda automaticamente)
    → AuthService mapeia Cliente para ClienteResponse
  → AuthController retorna 200 OK com ClienteResponse

GET /auth/me (Cookie: JSESSIONID=...)
  → Spring Security intercepta; valida sessão pelo JSESSIONID e popula SecurityContext
    → se sessão inválida/ausente: 401 Unauthorized (filtro bloqueia antes de chegar no controller)
  → AuthController.me(authentication) recebe o Authentication já resolvido
  → AuthService.me() extrai ClienteUserDetails e mapeia para ClienteResponse
  → AuthController retorna 200 OK com ClienteResponse

POST /auth/logout (Cookie: JSESSIONID=...)
  → Spring Security invalidata sessão (delete JSESSIONID)
  → Retorna 200 e limpa o contexto
```

### Dependência de Bean

O `AuthenticationManager` é exposto como bean em `SecurityConfig` e injetado no `AuthService`. O Spring
detecta automaticamente o `ClienteUserDetailsService` como `UserDetailsService` e o usa no `DaoAuthenticationProvider`,
que passa para o `AuthenticationManager`.

## Testes

Consulte `AuthServiceTest.java` para testes de:
- Login com sucesso (credenciais válidas).
- Login com senha inválida.
- Recuperação de dados do usuário autenticado (`me`).

## Exemplos de requisição e resposta

### POST /auth/login

**Requisição:**
```json
{
  "email": "joao@example.com",
  "senha": "senha123456"
}
```

**Resposta (200 OK):**
```json
{
  "id": "123abc",
  "nome": "João Silva",
  "email": "joao@example.com",
  "papel": "CLIENTE"
}
```

**Header de resposta:**
```
Set-Cookie: JSESSIONID=ABC123...
```

**Resposta (401 Unauthorized):**
```json
{
  "instante": "2026-08-04T10:30:00Z",
  "status": 401,
  "erro": "Unauthorized",
  "mensagem": "E-mail ou senha inválidos"
}
```

### GET /auth/me

**Requisição:**
```
Cookie: JSESSIONID=ABC123...
```

**Resposta (200 OK):**
```json
{
  "id": "123abc",
  "nome": "João Silva",
  "email": "joao@example.com",
  "papel": "CLIENTE"
}
```

**Resposta (401 Unauthorized, sem sessão válida):**
```json
{
  "instante": "2026-08-04T10:30:00Z",
  "status": 401,
  "erro": "Unauthorized",
  "mensagem": "Erro interno no servidor"
}
```

### POST /auth/logout

**Requisição:**
```
Cookie: JSESSIONID=ABC123...
```

**Resposta (200 OK):**
Sessão invalidada; cookie é deletado.
