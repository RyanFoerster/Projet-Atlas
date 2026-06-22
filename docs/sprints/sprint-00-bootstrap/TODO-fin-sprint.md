# TODO de fin de Sprint 0

> Éléments à traiter au moment de clôturer le sprint (livrables documentaires de la Definition of Done + demandes spécifiques de Ryan accumulées en cours de route).

## Livrables DoD restants
- [ ] Devblog post `docs/blog/01-demarrage-atlas.md`
- [ ] Mini-cours `docs/learning/sprint-00-bootstrap.md`
- [ ] `RETROSPECTIVE.md` du sprint
- [ ] Récap pédagogique de fin de session

## Demandes spécifiques à intégrer dans le mini-cours `sprint-00-bootstrap.md`

### Section « Anatomie du pom.xml » (demandée par Ryan, étape 1)
Expliquer les concepts Maven moins évidents pour quelqu'un qui n'en a pas fait depuis quelques années :
- **`parent` vs `dependencyManagement` vs `dependencies`** : héritage (config + plugins + gestion de versions du parent) vs alignement de versions sans déclaration vs déclaration effective qui ajoute la dépendance au classpath.
- **`import` scope sur un BOM** : pourquoi importer un BOM (`<type>pom</type><scope>import</scope>`) diffère d'hériter d'un parent — permet d'agréger plusieurs BOMs là où on n'a qu'un seul parent.
- **`relativePath`** : à quoi il sert, et ce qui se passe quand il est vide (Maven ne cherche pas un parent sur le disque local, va directement au dépôt distant).
- **`-starter-*` vs artefact « nu »** : un starter est un méta-package d'opinion (agrège plusieurs dépendances cohérentes + auto-config) vs la lib sous-jacente seule.
- **Stratégie BOM-first (ADR-009)** appliquée concrètement dans notre `pom.xml` : ce qu'on hérite, ce qu'on fige dans `<properties>`, ce qu'on pinne explicitement.

### Section « Data layer & tests d'intégration » (demandée par Ryan, étape 3)
Détailler particulièrement :
- **`@ServiceConnection`** (Spring Boot 3.1+) : ce que ça fait (auto-config de la DataSource depuis le container), pourquoi c'est supérieur à la config manuelle `@DynamicPropertySource` (pas de chaînes de propriétés à la main, type-safe, couvre plus que url/user/pass), et pourquoi beaucoup de devs Spring l'ignorent encore (habitude pré-3.1).
- **Singleton container pattern** (Testcontainers) : pourquoi réutiliser le container entre tests plutôt qu'un par classe (coût de démarrage d'un container Postgres), gains de perf concrets, et limites — isolation des tests par **truncate** des tables entre tests, pas par recreate du container (ADR-008).
- **Synchro de version Postgres à source unique** : propriété pom `postgres.image.version` → surefire `systemPropertyVariables` → constante Java (Testcontainers) ; → scripts `help:evaluate` → env (docker-compose). Expliquer le trade-off pom↔compose.
