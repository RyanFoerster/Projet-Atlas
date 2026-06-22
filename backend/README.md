# Atlas — Backend

Application Spring Boot du projet Atlas (modular monolith DDD). Pour le contexte produit, l'architecture et les conventions, voir [`/CLAUDE.md`](../CLAUDE.md) et [`/docs`](../docs).

## Stack

- **Java 25 LTS**, **Spring Boot 4.1**, **Spring Modulith 2.1** (isolation des modules)
- **PostgreSQL 17** + **Flyway** (migrations) + **Hibernate / Spring Data JPA**
- Tests : **JUnit 6**, **AssertJ**, **Testcontainers** (PostgreSQL réel, jamais H2)

## Prérequis

- **JDK 25** (le `JAVA_HOME` doit pointer dessus)
- **Docker** (Testcontainers pour les tests ; PostgreSQL local via `docker-compose.yml` à la racine)

## Commandes essentielles

Toutes via le Maven wrapper, depuis `backend/` :

```bash
./mvnw clean verify        # build + tous les tests (Modulith verify + intégration Testcontainers)
./mvnw spring-boot:run     # démarre l'application (profil local, PostgreSQL sur localhost:5432)
```

Depuis la racine du dépôt, `scripts/dev-start.sh` lance PostgreSQL (docker compose) puis l'application.

## Repères

- Endpoints : `/actuator/health`, Swagger UI sur `/swagger-ui.html`
- Architecture et décisions : [`/docs/adr`](../docs/adr) (notamment ADR-001, ADR-003, ADR-010)
