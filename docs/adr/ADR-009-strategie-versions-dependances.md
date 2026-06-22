# ADR-009 : Stratégie de gestion des versions de dépendances

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Atlas repose sur Spring Boot 4.1, qui fournit un **BOM** (Bill of Materials, `spring-boot-dependencies`) gérant les versions de plusieurs centaines de bibliothèques de l'écosystème Spring et de ses dépendances directes (Hibernate, Flyway, Testcontainers, le driver PostgreSQL, JUnit, AssertJ, Mockito, Jackson, etc.). Toutes ces versions sont mutuellement testées et garanties compatibles entre elles par l'équipe Spring.

Lors du sprint 0, plusieurs versions pinées « à la main » dans la documentation initiale (CLAUDE.md, ADR-002) se sont révélées **incompatibles ou en retard** par rapport à ce que le BOM Boot 4.1 impose (ex : Flyway 11.x demandé alors que le BOM fournit 12.4.0 ; Testcontainers 1.20.x alors que le BOM fournit 2.0.5). Pinner manuellement des versions divergentes de celles du BOM expose à des conflits de classpath subtils et casse la garantie de compatibilité.

Trois approches étaient possibles :
1. **Tout pinner à la main** dans le `pom.xml` — contrôle maximal mais fragile, casse la cohésion garantie par le BOM, fastidieux à maintenir aux upgrades.
2. **Tout laisser au BOM en silence** (ne déclarer aucune version) — robuste mais opaque : une lecture du `pom.xml` ne dit pas quelle version est réellement utilisée, et un upgrade mineur de Boot peut changer une version sans trace visible.
3. **Approche hybride tracée** — hériter du BOM, mais figer explicitement dans `<properties>` les versions des libs sensibles, avec un commentaire indiquant qu'elles proviennent du BOM.

## Décision

Le projet adopte une **stratégie hybride à trois règles** :

**Règle 1 — Écosystème Spring : hériter du BOM.**
Pour toutes les libs gérées par `spring-boot-dependencies` (Hibernate, Flyway, Testcontainers, driver PostgreSQL, JUnit, AssertJ, Mockito, Jackson, etc.), on **ne redéfinit jamais une version divergente**. On fait confiance au BOM pour la cohésion. Les starters (`spring-boot-starter-*`) sont déclarés sans `<version>`.

**Règle 2 — Libs hors écosystème Spring : pinner explicitement.**
Pour les libs non gérées par le BOM, on pinne explicitement la version :
- **Spring Modulith** : importé via son **propre BOM** (`spring-modulith-bom`), version alignée sur la ligne Boot (2.1.x pour Boot 4.1).
- **springdoc-openapi** : pinné explicitement (ligne 3.0.x pour Spring Framework 7).
- **MapStruct** : pinné explicitement (1.6.3), ainsi que son annotation processor.

**Règle 3 — Traçabilité : figer les versions héritées dans `<properties>`.**
Pour les libs de l'écosystème Spring jugées **structurellement importantes** (base de données, migrations, tests d'intégration), on **re-déclare la version héritée du BOM dans une `<properties>`**, à l'identique, avec un commentaire `<!-- hérité du BOM Boot X.Y -->`. Spring Boot lit ces propriétés (`flyway.version`, `hibernate.version`, `testcontainers.version`, `postgresql.version`, …) et les respecte. L'effet est neutre sur le comportement (même version), mais :
- une lecture du `pom.xml` révèle immédiatement la version effective ;
- lors d'un upgrade mineur de Boot, le diff montre explicitement quelles versions changent (la propriété figée devient un point de revue conscient) ;
- on documente l'intention plutôt que de subir un changement silencieux.

À chaque upgrade de Spring Boot, on **revérifie** les versions figées contre le nouveau BOM et on met à jour les `<properties>` consciemment.

## Conséquences

**Positives**
- Cohésion de l'écosystème Spring garantie par le BOM, sans lutter contre lui.
- `pom.xml` lisible : les versions des libs critiques sont visibles d'un coup d'œil.
- Upgrades Spring Boot maîtrisés : les changements de versions sensibles sont des points de revue explicites, pas des surprises.
- Pinnage minimal et justifié pour les libs hors BOM.

**Négatives**
- Petite redondance : les versions figées dans `<properties>` dupliquent ce que le BOM fournit déjà. Mitigation : limité aux quelques libs structurelles, avec commentaire explicatif.
- Discipline requise : penser à revérifier les versions figées à chaque upgrade de Boot. Mitigation : checklist d'upgrade.

**Neutres**
- L'effet runtime de la règle 3 est nul tant que la version figée == version du BOM. C'est un choix de **documentation et de traçabilité**, pas de comportement.

## Références

- ADR-002 (révisé sprint 0) — stack technique et corrections de versions appliquées.
- ADR-008 — PostgreSQL + Flyway + Testcontainers (libs concernées par la règle 3).
- Spring Boot Reference — *Dependency Management* et *Customizing managed versions via properties*.
