# Explicação da Classe AuthService

## Visão Geral

A classe `AuthService` é um serviço Spring responsável por gerenciar a autenticação e autorização de clientes no sistema. Ela utiliza o framework **Spring Security** para validar credenciais e manter a sessão de autenticação através de cookies.

**Localização**: `src/main/java/com/example/Restaurante/service/AuthService.java`

---

## Componentes e Dependências

### 1. **AuthenticationManager**
```java
private final AuthenticationManager authenticationManager;
```

**O que é**: Interface do Spring Security responsável por autenticar um usuário usando suas credenciais.

**Referência Oficial**: [Spring Security - AuthenticationManager](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-authentication-manager)

**Funcionamento**: 
- Recebe um `UsernamePasswordAuthenticationToken` (email e senha)
- Valida as credenciais contra o banco de dados (via `ClienteUserDetailsService`)
- Retorna um objeto `Authentication` autenticado ou lança `BadCredentialsException` se inválido

---

### 2. **SecurityContextRepository**
```java
private final SecurityContextRepository securityContextRepository;
```

**O que é**: Interface responsável por carregar e salvar o contexto de segurança (autenticação) da sessão do usuário.

**Referência Oficial**: [Spring Security - SecurityContextRepository](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html)

**Funcionamento**:
- **Salvar**: Persiste o contexto de autenticação no request/response (geralmente em cookies)
- **Carregar**: Restaura a autenticação do usuário em requisições subsequentes
- Permite autenticação **stateful** (com sessão)

---

## Métodos

### `login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse)`

**Assinatura**:
```java
public ClienteResponse login(LoginRequest request, 
                            HttpServletRequest httpRequest, 
                            HttpServletResponse httpResponse)
```

**Parâmetros**:
- **`LoginRequest request`**: DTO contendo `email()` e `senha()` do registro
- **`HttpServletRequest httpRequest`**: Objeto da requisição HTTP atual (Jakarta Servlet)
- **`HttpServletResponse httpResponse`**: Objeto da resposta HTTP (Jakarta Servlet)

**Referência Oficial**: 
- [Jakarta Servlet - HttpServletRequest](https://jakarta.ee/specifications/servlet/)
- [Jakarta Servlet - HttpServletResponse](https://jakarta.ee/specifications/servlet/)

**Fluxo de Execução**:

```
1. Criar UsernamePasswordAuthenticationToken
   ↓
   new UsernamePasswordAuthenticationToken(request.email(), request.senha())
   
2. Autenticar usando AuthenticationManager
   ↓
   authenticationManager.authenticate(token)
   
3. Salvar contexto vazio (preparação)
   ↓
   securityContextRepository.saveContext(...)
   
4. Definir autenticação no SecurityContextHolder
   ↓
   SecurityContextHolder.getContext().setAuthentication(authentication)
   
5. Salvar contexto autenticado nos cookies
   ↓
   securityContextRepository.saveContext(...)
   
6. Extrair ClienteUserDetails e retornar dados
   ↓
   return ClienteResponse(...)
```

**Tratamento de Erro**:
```java
catch (BadCredentialsException e) {
    throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
}
```

**Referência Oficial**: [Spring Security - BadCredentialsException](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authentication/BadCredentialsException.html)

**O que retorna**: 
- Um objeto `ClienteResponse` contendo:
  - `id`: ID do registro
  - `nome`: Nome completo do registro
  - `email`: Email do registro
  - `papel`: Papel/role do registro (ex: ADMIN, USER)

---

### `me(Authentication authentication)`

**Assinatura**:
```java
public ClienteResponse me(Authentication authentication)
```

**Parâmetros**:
- **`Authentication authentication`**: Objeto injetado pelo Spring Security contendo a autenticação atual do usuário

**Referência Oficial**: [Spring Security - Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-authentication)

**Funcionamento**:
1. Recebe o objeto `Authentication` (injetado automaticamente pelo Spring Security)
2. Extrai o `ClienteUserDetails` da autenticação via `authentication.getPrincipal()`
3. Acessa os dados do registro através de `userDetails.getCliente()`
4. Retorna um `ClienteResponse` com os dados do registro autenticado

**O que retorna**: 
Um objeto `ClienteResponse` com os dados do registro atualmente autenticado

**Caso de Uso**: 
Endpoint que retorna os dados do usuário logado (geralmente em uma rota `GET /auth/me`)

---

## Fluxo Completo de Autenticação

```
┌─────────────────────────────────────────────────────────────┐
│ 1. CLIENTE ENVIA CREDENCIAIS                                │
│    POST /auth/login                                          │
│    {                                                         │
│      "email": "registro@example.com",                        │
│      "senha": "senha123"                                    │
│    }                                                         │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. AUTHSERVICE.LOGIN() VALIDA                               │
│    - Cria UsernamePasswordAuthenticationToken               │
│    - Chama authenticationManager.authenticate()             │
└────────────────┬────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        ▼                 ▼
    ✓ Válido         ✗ Inválido
        │                 │
        ▼                 ▼
    Continua      CredenciaisInvalidasException
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. SALVAR CONTEXTO DE SEGURANÇA                             │
│    - Salva autenticação no SecurityContextRepository        │
│    - Cria cookie de sessão na resposta HTTP                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. RETORNAR DADOS DO CLIENTE                                │
│    HTTP 200 OK                                              │
│    {                                                         │
│      "id": "123",                                           │
│      "nome": "João Silva",                                  │
│      "email": "registro@example.com",                        │
│      "papel": "USER"                                        │
│    }                                                         │
│    Set-Cookie: SESSIONID=abc123def456...                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Requisições Subsequentes (com Sessão)

```
┌──────────────────────────────────────┐
│ CLIENTE ENVIA NOVA REQUISIÇÃO        │
│ GET /api/clientes                    │
│ Cookie: SESSIONID=abc123def456...    │
└────────────────┬─────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────┐
│ SPRING SECURITY INTERCEPTA            │
│ - Lê o cookie da sessão              │
│ - SecurityContextRepository carrega   │
│   a autenticação do usuario          │
└────────────────┬─────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────┐
│ CONTEXTO RESTAURADO                  │
│ - Usuario ja autenticado             │
│ - Pode acessar recursos protegidos   │
└──────────────────────────────────────┘
```

---

## Componentes Relacionados

### **ClienteUserDetails**
- Implementação de `UserDetails` do Spring Security
- Contém o objeto `Cliente` com dados do usuário
- Usado para extrair informações do usuário autenticado

**Referência Oficial**: [Spring Security - UserDetails](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html)

### **SecurityContextHolder**
- Classe utilitária que armazena o contexto de segurança em thread-local
- Permite acesso à autenticação em qualquer lugar da aplicação
- `SecurityContextHolder.getContext()` retorna o contexto atual

**Referência Oficial**: [Spring Security - SecurityContextHolder](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder)

---

## Diagrama de Arquitetura

```
┌────────────────────────────────────────────────────────────┐
│                     AuthService                             │
├────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐        ┌─────────────────────────┐  │
│  │ login()          │        │ me()                    │  │
│  │                  │        │                         │  │
│  │ Autentica        │        │ Retorna dados do        │  │
│  │ credenciais      │        │ usuário logado          │  │
│  └────────┬─────────┘        └────────┬────────────────┘  │
│           │                           │                     │
│           ▼                           ▼                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         AuthenticationManager                        │  │
│  │  (Valida credenciais)                               │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         ClienteUserDetailsService                    │  │
│  │  (Carrega usuário do banco de dados)                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                 │
│                           ▼                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         SecurityContextRepository                    │  │
│  │  (Salva/carrega sessão via cookies)                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└────────────────────────────────────────────────────────────┘
```

---

## Referências Oficiais

1. **Spring Security Documentation**
   - https://docs.spring.io/spring-security/reference/

2. **Spring Security - Authentication Architecture**
   - https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html

3. **Spring Security - Session Management**
   - https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html

4. **Jakarta Servlet Specification**
   - https://jakarta.ee/specifications/servlet/

5. **Spring Framework - Dependency Injection**
   - https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-constructor-injection

6. **Spring Boot - Spring Security Auto-configuration**
   - https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.security

---

## Resumo

A classe `AuthService` implementa o padrão de autenticação stateful do Spring Security:

| Aspecto | Descrição |
|---------|-----------|
| **Tipo de Autenticação** | Stateful (com sessão/cookie) |
| **Validação** | Via `AuthenticationManager` |
| **Persistência** | Via `SecurityContextRepository` (cookies) |
| **Método Principal** | `login()` para autenticar |
| **Método Secundário** | `me()` para obter dados do usuário logado |
| **Tratamento de Erro** | Converte `BadCredentialsException` em `CredenciaisInvalidasException` |

