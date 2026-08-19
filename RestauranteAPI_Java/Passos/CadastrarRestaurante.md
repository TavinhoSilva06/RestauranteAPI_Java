# Cadastro de Cliente

## Objetivo

Permitir que uma pessoa crie uma conta de cliente na API do restaurante, fornecendo seus dados básicos e uma senha, para uso futuro em login e demais funcionalidades (pedidos, etc.).

## Dados necessários

- Nome do cliente
- E-mail (identifica a conta de forma única)
- Senha (usada para autenticação futura)

## Regras de negócio

1. **E-mail único**: não pode existir duas contas com o mesmo e-mail. Antes de criar a conta, o sistema verifica se já existe um cadastro com aquele e-mail; se existir, o cadastro é recusado.
2. **Validação de formato**: nome e e-mail não podem estar vazios; o e-mail precisa ter um formato válido; a senha precisa atender a um tamanho mínimo (ex.: 8 caracteres) para evitar senhas fracas.
3. **Senha nunca é armazenada em texto puro**: antes de salvar, a senha é transformada com um algoritmo de hash (ex.: BCrypt). O sistema nunca guarda nem expõe a senha original.
4. **Papel padrão**: toda conta criada por esse fluxo público recebe o papel de "cliente". Papéis administrativos (ex.: funcionário do restaurante) não são atribuídos por este cadastro.

## Fluxo passo a passo

1. O cliente envia nome, e-mail e senha.
2. O sistema valida o formato dos dados recebidos. Se algo estiver inválido, o cadastro é recusado e o motivo é informado.
3. O sistema verifica se o e-mail já está cadastrado. Se estiver, o cadastro é recusado informando que o e-mail já está em uso (sem revelar mais detalhes, por segurança).
4. Se passou nas validações, a senha é transformada em hash.
5. Os dados (nome, e-mail, senha em hash, papel "cliente") são salvos como uma nova conta.
6. O sistema confirma o cadastro para o cliente, retornando os dados básicos da conta criada — **nunca a senha**, nem o hash dela.

## O que fica fora deste fluxo

- Login (autenticação de uma conta já existente) é uma funcionalidade separada, que vai reutilizar a senha cadastrada aqui para conferir as credenciais.
- Alteração de papel/permissões de uma conta (ex.: promover para funcionário) não faz parte do cadastro público — é uma ação administrativa à parte.
- Recuperação/troca de senha é um fluxo à parte, não coberto aqui.

## Estratégia técnica

Segue a arquitetura em camadas já adotada no projeto (`Controller → Service → Repository → Database`, ver `CLAUDE.md`), usando o pacote raiz `com.example.Restaurante`.

### Camadas e classes

| Camada | Classe | Responsabilidade |
|---|---|---|
| Document | `document/Cliente.java` | `@Document(collection = "clientes")`: id, nome, email, senha (hash), papel, dataCriacao. |
| Document | `document/Papel.java` | Enum (`CLIENTE`, `FUNCIONARIO`, `ADMIN`) — cadastro público sempre grava `CLIENTE`. |
| Repository | `repository/ClienteRepository.java` | `extends MongoRepository<Cliente, String>`; métodos `existsByEmail(String)` e `findByEmail(String)`. Só acessa dados, sem regra de negócio. |
| DTO (request) | `dto/ClienteCadastroRequest.java` | Record com `nome`, `email`, `senha`, validado com Bean Validation (`@NotBlank`, `@Email`, `@Size(min = 8)`). |
| DTO (response) | `dto/ClienteResponse.java` | Record com `id`, `nome`, `email`, `papel` — nunca inclui a senha/hash. |
| Exception | `exception/EmailJaCadastradoException.java` | Lançada pelo Service quando o e-mail já existe. |
| Exception | `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`: mapeia `EmailJaCadastradoException` → 409, erros de validação (`MethodArgumentNotValidException`) → 400, fallback → 500 padronizado (nunca vaza stacktrace). |
| Config | `config/PasswordEncoderConfig.java` | Expõe o bean `PasswordEncoder` (`BCryptPasswordEncoder`) usado pelo Service para gerar o hash. Não configura login/JWT — isso é do módulo de Autenticação, à parte. |
| Service | `service/ClienteService.java` | Recebe `ClienteCadastroRequest`; valida duplicidade de e-mail via repository; gera hash da senha via `PasswordEncoder`; monta `Cliente` com papel `CLIENTE`; salva; mapeia para `ClienteResponse`. Concentra toda a regra de negócio descrita na seção acima. Injeção via construtor. |
| Controller | `controller/ClienteController.java` | `POST /clientes` recebendo `@Valid @RequestBody ClienteCadastroRequest`, delegando 100% ao `ClienteService` e retornando `ClienteResponse` com status 201. Não acessa `ClienteRepository` diretamente. |

### Dependência adicional necessária

O `pom.xml` atual não tem suporte a Bean Validation (`@NotBlank`, `@Email`, etc.). Será preciso adicionar:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
`spring-boot-starter-security` já está no `pom.xml` e fornece `BCryptPasswordEncoder` sem dependência extra.

### Fluxo técnico da requisição

```
POST /clientes
  → ClienteController valida payload (@Valid) e chama ClienteService.cadastrar(request)
    → ClienteService verifica ClienteRepository.existsByEmail(email)
        → se true: lança EmailJaCadastradoException → GlobalExceptionHandler → 409
    → ClienteService codifica a senha com PasswordEncoder.encode(senha)
    → ClienteService monta Cliente(papel = CLIENTE) e chama ClienteRepository.save(cliente)
    → ClienteService mapeia Cliente salvo → ClienteResponse (sem senha)
  → ClienteController retorna 201 Created com ClienteResponse
```

Testes de validação (`@Valid`) que falharem são interceptados antes de chegar ao Service, pelo próprio Spring, e tratados pelo `GlobalExceptionHandler` → 400.
