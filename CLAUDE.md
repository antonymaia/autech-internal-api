# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos Essenciais

```bash
# Build e empacotamento
mvn clean package

# Executar a aplicação
mvn spring-boot:run

# Executar testes
mvn test

# Executar um único teste
mvn test -Dtest=NomeDaClasseTest

# Build sem testes
mvn clean package -DskipTests
```

A API sobe na porta `5000` com contexto `/internal-admin-api`. Em dev: `http://localhost:5000/internal-admin-api`.

## Arquitetura

Aplicação REST em Spring Boot 2.6.1 / Java 11 com Maven. Segue arquitetura em camadas clássica:

```
controller/   → endpoints HTTP (recebe DTOs, delega para services)
service/      → regras de negócio
repository/   → acesso ao banco (Spring Data JPA)
model/        → entidades JPA
dtos/         → objetos de transferência de dados
config/       → beans de configuração Spring
ScheduledTasks/ → tarefas agendadas (@Scheduled)
```

**Tratamento de erros:** `ResourceExceptionHandler` (ControllerAdvice) centraliza todas as exceções. Exceções customizadas ficam em `controller/exception/` e `service/exception/`. Retornam `StandardError` ou `ValidationError` com código HTTP adequado.

## Domínio Principal

O sistema gerencia clientes com assinaturas e faturas:

- **Cliente** — chave primária é `cnpjCpf` (String). Possui endereço (`Endereco` → `Cidade` → `Estado`) e relação `OneToOne` com `Assinatura`.
- **Assinatura** — controla vigência e status (`ATIVA`, `CANCELADA`, `SUSPENSA`) e tipo (`TipoAssinatura` enum).
- **Fatura** — estados: `ABERTA=1`, `PAGA=2`, `VENCIDA=3`. Ligada a `Cliente` (ManyToOne) e a pagamentos via `PagamentoFatura` (tabela de junção).
- **Pagamento** — classe abstrata com herança `JOINED`. Subtipos: `PagamentoPix`, `PagamentoDinheiro`, `PagamentoCartaoCredito`, `PagamentoCartaoDebito`.
- **Ncm / Siscomex** — tabela de códigos NCM com integração a Siscomex para importação de dados.

`Cliente.getChave()` gera uma chave derivada do CNPJ/CPF — lógica customizada relevante para buscas e integrações.

## Banco de Dados

MySQL remoto em `autechbd.mysql.uhserver.com:3306` (banco `autechbd`). Credenciais em `application-dev.properties` e `application-prod.properties`.

O perfil ativo é definido em `application.properties`:
```properties
spring.profiles.active=dev
```

`DBService.popularBancoDados()` existe para seed de dados iniciais, mas está **comentado** em `DevConfig`. Ativar apenas em dev quando necessário recriar o banco.

## Segurança e CORS

`SecurityConfig` libera todos os endpoints (`.anyRequest().permitAll()`) e configura CORS para `localhost:4200`. CSRF está desabilitado. Ao adicionar restrições de acesso, editar esta classe.

## Tarefas Agendadas

`PagamentoTask` (em `ScheduledTasks/`) executa geração de faturas e processamento de pagamentos automaticamente. `@EnableScheduling` está ativo na classe principal.

## Email

Configurado para SMTP Gmail (`smtp.gmail.com:587`). Templates HTML usam Thymeleaf via `TemplateBuilder`. Template `EmailLicencaVencida` notifica clientes com licença vencida.
