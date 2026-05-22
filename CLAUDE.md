# Java / Spring Project

## Stack
- Java 21+ (use virtual threads, records, sealed classes, pattern matching where appropriate)
- Spring Boot 3.x
- Spring Security 6.x
- Spring Data JPA + Hibernate
- Spring WebFlux (reactive where needed)
- Flyway for migrations
- Kafka / RabbitMQ for messaging
- Docker + Kubernetes for deployment

## Project structure
Follow a layered package structure by feature (not by layer):
```
com.pgoogol.app/
  user/
    UserController.java
    UserService.java
    UserRepository.java
    UserMapper.java
    dto/
    domain/
  order/
    ...
  shared/
    exception/
    config/
    util/
```

## Build & run
```bash
./mvnw clean verify          # full build with tests
./mvnw spring-boot:run       # local run
./mvnw test                  # unit tests only
./mvnw verify -Pfailsafe     # integration tests
docker compose up -d         # start dependencies (DB, Kafka, etc.)
```

## Key rules (details in .claude/rules/)
- Code style & naming → rules/code-style.md
- Security → rules/security.md
- Testing → rules/testing.md
- Error handling & logging → rules/error-handling.md
- Database & performance → rules/database.md

## Non-negotiables
- Never commit secrets, credentials, or API keys
- Every public service method must have a unit test
- All DB changes go through Flyway migrations — never modify schema manually
- Ask before any destructive operation (drop table, delete data, force push)
- Run `./mvnw verify` before marking a task as done
