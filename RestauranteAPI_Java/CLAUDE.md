# CLAUDE.md

Este arquivo fornece orientações ao Claude Code (claude.ai/code) ao trabalhar com o código deste repositório.

## Visão geral do projeto

RestauranteAPI_Java é uma API REST em Spring Boot 4.1.0 (Java 21), com MongoDB como banco de dados. O projeto está atualmente em estágio de esqueleto: existem apenas o entrypoint padrão da aplicação e o teste padrão (`RestauranteApplication.java`, `RestauranteApplicationTests.java`). Nenhum controller, service, repository ou código de domínio foi adicionado ainda.

## Comandos

- Rodar a aplicação: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
- Build: `./mvnw clean install`
- Rodar todos os testes: `./mvnw test`
- Rodar uma classe de teste específica: `./mvnw test -Dtest=NomeDaClasse`
- Rodar um método de teste específico: `./mvnw test -Dtest=NomeDaClasse#nomeDoMetodo`
- Empacotar: `./mvnw clean package`

O MongoDB precisa estar acessível em `mongodb://localhost:27017/restaurante` (configurado em `src/main/resources/application.properties`) para a aplicação subir.

## Arquitetura

Pacote raiz: `com.example.Restaurante`. Ainda não existem camadas ou pacotes além do entrypoint da aplicação — esta seção deve ser expandida conforme controllers, services e repositories forem introduzidos.
