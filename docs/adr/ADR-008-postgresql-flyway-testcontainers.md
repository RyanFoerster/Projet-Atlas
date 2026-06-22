# ADR-008 : PostgreSQL + Flyway + Testcontainers pour le data layer

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Le choix de la couche de persistence influence la fidélité des tests, la qualité du schéma, et la productivité quotidienne. Plusieurs options classiques étaient envisageables :

**Pour la base de données** :
- PostgreSQL — riche en fonctionnalités (JSONB, window functions, gestion temporelle, full-text search), maturité éprouvée
- MySQL — populaire mais moins riche fonctionnellement pour les besoins analytiques
- MongoDB — overkill et inapproprié pour un domaine relationnel comme celui d'Atlas

**Pour les migrations** :
- Flyway — versionning explicite par fichiers SQL, simple et déterministe
- Liquibase — plus configurable, XML/YAML, plus lourd à utiliser
- Hibernate `ddl-auto: update` — inacceptable en production, source de bugs

**Pour les tests de persistence** :
- H2 (in-memory) — rapide mais ment sur le comportement réel de Postgres (dialectes différents, types absents)
- Testcontainers avec PostgreSQL réel — vrais tests, légère latence supplémentaire mais fidélité garantie

## Décision

Le projet adopte :

- **PostgreSQL 17** comme unique base de données (dev, test, prod)
- **Flyway 11.x** pour les migrations, fichiers SQL versionnés dans `src/main/resources/db/migration/`
- **Testcontainers 1.20.x** avec PostgreSQL réel pour TOUS les tests touchant la persistence

**Pas de H2, jamais.** Même pour des tests "rapides". Tester sur autre chose que la DB de production conduit à des bugs de prod silencieux.

**Conventions Flyway** :
- Migrations forward-only en production (jamais de `repair`, jamais de rollback automatique).
- Format de nommage : `V001__create_users_table.sql`, `V002__add_athletes_aggregate.sql`.
- Une migration par feature/PR significative.
- Migration de seed data séparée si nécessaire (`R__seed_reference_data.sql` repeatable).

**Conventions de schéma** :
- Tables nommées en `snake_case` au pluriel : `athletes`, `training_sessions`, `program_instances`.
- Colonnes en `snake_case`.
- Clés primaires : UUID v7 (générées en application) pour les aggregates métier. UUID v7 ordonné dans le temps = bon pour les indexes.
- Timestamps : `created_at`, `updated_at` en `TIMESTAMPTZ`.
- Foreign keys explicites, indexes sur toutes les colonnes filtrées fréquemment.

**Spring Data JPA + Hibernate** :
- Repositories Spring Data pour les CRUD basiques.
- JPQL ou native SQL pour les queries complexes (préférer native pour les agrégations Insights).
- Les **entités JPA vivent dans `infrastructure/persistence/`**, séparées du domaine (cf. ADR-003).
- Mapping domain ↔ JPA via MapStruct ou mappers manuels.

**Setup Testcontainers** :
- Container PostgreSQL réutilisé entre tests (avec Singleton container) pour la performance.
- Migrations Flyway appliquées au démarrage du container.
- Truncate des tables entre tests, pas drop/recreate (gain de performance significatif).

## Conséquences

**Positives**
- Fidélité maximale entre dev/test/prod : pas de surprise en déploiement.
- PostgreSQL 17 ouvre la porte à des features riches pour Insights (window functions, CTE, JSONB pour projections flexibles).
- Flyway versionne le schéma comme on versionne le code : revue de PR, history Git, etc.
- Testcontainers garantit que les tests reflètent la réalité.

**Négatives**
- Tests d'intégration plus lents qu'avec H2 (quelques secondes de boot du container vs millisecondes). Mitigation : container singleton, parallélisation, tests unitaires de domaine restent en millisecondes.
- Nécessite Docker installé en dev. Acceptable pour un dev moderne.

**Neutres**
- Choix consensuel et défendable dans n'importe quel contexte enterprise.
