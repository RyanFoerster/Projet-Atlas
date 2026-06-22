# Notes brutes pour le mini-cours `sprint-00-bootstrap.md`

> Capture rapide en cours de sprint (3-5 lignes par point) pour ne rien perdre.
> À rédiger proprement en fin de sprint dans `docs/learning/sprint-00-bootstrap.md`.
> Voir aussi la structure des sections demandée dans `TODO-fin-sprint.md`.

## 1. Modularisation de l'autoconfigure dans Spring Boot 4 (LA rupture structurante)
- En Boot 3.x, presque toutes les auto-configurations vivaient dans un seul jar monolithique `spring-boot-autoconfigure`. Avoir une lib (ex : flyway-core) sur le classpath suffisait à déclencher son auto-config.
- En Boot 4.x, l'autoconfigure est **éclaté en modules par techno** : `spring-boot-flyway`, `spring-boot-tomcat`, `spring-boot-health`, etc. La présence de `flyway-core` seul **ne déclenche plus** l'auto-config Flyway.
- Conséquence pratique : il faut le **starter dédié** (`spring-boot-starter-flyway`) qui tire le module d'auto-config. Symptôme typique du piège : la lib est là, aucune erreur, mais le comportement (migration, endpoint…) ne se produit pas et la classe `XxxAutoConfiguration` est absente du rapport `--debug`.
- C'est LE piège n°1 des migrations Boot 3→4. À mettre en avant.

## 2. `@ServiceConnection` vs `@DynamicPropertySource` (Testcontainers moderne)
- Avant (Boot < 3.1) : on câblait la DataSource à la main avec `@DynamicPropertySource` en recopiant `container.getJdbcUrl()`, `getUsername()`, `getPassword()` dans des propriétés Spring. Verbeux, fragile, à refaire pour chaque type de service.
- Depuis Boot 3.1 : `@ServiceConnection` sur le champ container → Spring Boot lit les détails de connexion directement depuis le container et auto-configure la DataSource. Zéro chaîne de propriété, type-safe, couvre plus que url/user/pass.
- Beaucoup de devs l'ignorent encore (réflexe `@DynamicPropertySource` hérité). Montrer le avant/après côte à côte.

## 3. Singleton container pattern (Testcontainers)
- Problème : démarrer un container Postgres par classe de test coûte cher (secondes × N classes).
- Pattern : container `static` démarré une seule fois (bloc statique), partagé par héritage d'une base `AbstractIntegrationTest`, jamais arrêté explicitement → Ryuk le nettoie à la fin de la JVM.
- Notre implémentation : `static final PostgreSQLContainer` + `@ServiceConnection` dans `AbstractIntegrationTest`, réutilisé par toutes les classes de test qui en héritent.
- Limite / trade-off : l'isolation des données entre tests se fait par **truncate** des tables (rapide), PAS par recreate du container (lent). À comprendre : un container partagé = état partagé, donc nettoyage applicatif entre tests.

## 4. Synchro de version cross-systèmes (une source, deux propagations)
- Problème : la version Postgres existe à 2 endroits (docker-compose pour le runtime, Testcontainers pour les tests). Risque de dérive silencieuse (compose 17 / tests 16).
- Solution : **source de vérité unique = propriété pom `postgres.image.version`**.
- Propagation 1 (tests) : pom → `surefire systemPropertyVariables` → `System.getProperty()` → `DockerImageName`. Synchro réelle.
- Propagation 2 (compose) : pom → scripts `mvn help:evaluate` → variable d'env `$POSTGRES_VERSION` → `image: postgres:${POSTGRES_VERSION:-17}`. + commentaires croisés compose↔test comme garde-fou pour le `docker compose up` manuel.

## 5. Diagnostic « port 5432 squatté » (méthode, pas juste l'anecdote)
- Leçon de méthode : **prouver d'abord que le problème est externe à Atlas** avant de fouiller dans le code/la config.
- Démarche suivie : (a) Testcontainers vert (port aléatoire) → la config Atlas est bonne ; (b) auth directe container→container OK → le container est bon ; (c) `docker exec psql` interne marchait mais externe non → suspicion réseau ; (d) `pg_hba.conf` montrait `127.0.0.1 trust` (le test interne passait même avec un mauvais mot de passe = faux positif !) ; (e) stop du container → quelque chose répondait ENCORE sur host:5432 = fantôme ; (f) `ps aux | grep postgres` → PostgreSQL natif EnterpriseDB `/Library/PostgreSQL/17`.
- Pièges révélés : `trust` dans pg_hba donne de faux positifs d'auth ; `lsof` sans sudo ne voit pas les listeners d'autres users ; OrbStack ne peut pas binder un port hôte déjà pris par un service natif.

## 6. Renommage des artefacts Testcontainers en 2.x
- Testcontainers 2.x a **préfixé ses modules** par `testcontainers-`.
- Exemple : `org.testcontainers:postgresql` (ligne 1.x, figée à 1.21.4) → `org.testcontainers:testcontainers-postgresql` (2.x).
- Le BOM Boot 4.1 importe `testcontainers-bom:2.0.5` qui gère les nouveaux noms ; utiliser l'ancien artefactId donne une erreur « version missing ».
- Idem pour `testcontainers-jdbc`, `testcontainers-database-commons`.
