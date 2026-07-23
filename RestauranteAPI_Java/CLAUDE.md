Convenções Java:

Sempre usar Java 21.
Nunca usar Field Injection.
Sempre usar Constructor Injection.
Nunca acessar Repository diretamente pelo Controller.


Organização:

Controllers apenas recebem requisições.
Services possuem toda regra de negócio.
Repositories apenas acessam dados.
Toda feature nova deve possuir testes.


DTOs:

Nunca retornar Entity diretamente.
Sempre utilizar DTOs.


Tratamento de erros:

Sempre utilizar GlobalExceptionHandler.
Nunca lançar Exception genérica.


Código:

Sempre comentar apenas quando necessário.
Métodos pequenos.
Nomes em inglês.