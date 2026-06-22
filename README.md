# Atlas

> Un **Football Manager du lifting** : dirige une écurie d'athlètes de force, programme leurs cycles, amène-les en compétition — avec une simulation basée sur la vraie sport science. Un de tes athlètes, l'« athlète miroir », progresse selon **tes propres séances** loggées dans l'app.

Produit premium one-shot, sans mécanique prédatrice. Web app responsive (Angular + Spring Boot), self-hosted.

## Stack

- **Backend** — Java 25 LTS · Spring Boot 4.1 · Spring Modulith · PostgreSQL 17 · Flyway · Testcontainers
- **Frontend** — Angular 22 (zoneless, signals) · Tailwind CSS
- **Architecture** — Modular Monolith en DDD (8 bounded contexts isolés), domaine pur sans framework

## Structure (monorepo)

```
atlas/
├── backend/            # application Spring Boot (racine Maven + wrapper ./mvnw)
├── frontend/           # application Angular 22
├── docs/               # vision, glossaire, ADRs, sprints, devblog, mini-cours
├── scripts/            # dev-start.sh, db-reset.sh
└── docker-compose.yml  # PostgreSQL local (dev)
```

## Démarrage rapide

Prérequis : **JDK 25**, **Docker**, **Node 24**.

```bash
# Backend + base de données
./scripts/dev-start.sh           # lance PostgreSQL (docker compose) puis l'app Spring Boot

# Frontend (dans un autre terminal)
cd frontend && npm install && npm start
```

- API : http://localhost:8080 · health : `/actuator/health` · Swagger : `/swagger-ui.html`
- Frontend : http://localhost:4200

Tests backend : `cd backend && ./mvnw clean verify` (Spring Modulith verify + intégration Testcontainers sur un vrai PostgreSQL).

## Documentation

- [`CLAUDE.md`](CLAUDE.md) — contexte projet, conventions, architecture
- [`docs/vision.md`](docs/vision.md) — le pourquoi
- [`docs/domain/glossary.md`](docs/domain/glossary.md) — ubiquitous language
- [`docs/adr/`](docs/adr/) — décisions structurantes (ADR)
- [`docs/learning/`](docs/learning/) — mini-cours par sprint · [`docs/blog/`](docs/blog/) — devblog

## Statut

En développement. **Sprint 0 (bootstrap) terminé** : fondations, CI, persistence testée, frontend. Prochaine étape : Sprint 1 — Identity & onboarding.
