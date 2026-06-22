# Sprint 0 — Bootstrap du projet Atlas

> Ce document est le **prompt complet** à coller dans Claude Code (ou Cowork) pour exécuter le Sprint 0. Il contient tout le contexte nécessaire et la liste précise des artefacts à produire.

---

## Contexte projet (lecture préalable obligatoire)

Avant d'exécuter ce sprint, lis dans cet ordre :

1. `CLAUDE.md` à la racine — contexte général, conventions, mode de collaboration
2. `docs/vision.md` — pourquoi on construit Atlas
3. `docs/domain/glossary.md` — vocabulaire métier officiel
4. Tous les ADRs dans `docs/adr/` (ADR-001 à ADR-008)

Si l'un de ces documents est absent ou incomplet, **arrête et signale-le à Ryan** avant de continuer.

---

## Objectif du Sprint 0

À la fin de ce sprint, le projet doit avoir :

- Un **repo Git initialisé** avec une structure de packages propre pour les 8 modules
- Un **build Maven qui passe** avec toutes les dépendances pinées aux bonnes versions
- **Spring Modulith opérationnel** avec vérification d'isolation activée
- Un **endpoint `/health`** qui répond
- Une **CI GitHub Actions** qui build et test à chaque push
- Une **base PostgreSQL locale** lancée via Docker Compose
- Une **migration Flyway V001** initiale (peut être vide pour l'instant)
- Une **app Angular 22** qui boot avec une page "Hello Atlas" Tailwind
- Un **premier déploiement** sur le VPS via Dokploy (optionnel si infra pas prête, peut attendre fin de sprint 1)
- Un **devblog post #1** "Démarrage d'Atlas" rédigé en markdown
- Un **récap pédagogique** des concepts clés introduits dans ce sprint

---

## Portée technique

### Backend

**Structure de packages à créer** (vide pour les modules non encore implémentés, mais structure en place) :

```
src/main/java/dev/ryanfoerster/atlas/
├── AtlasApplication.java
├── shared/
│   ├── domain/
│   └── events/
├── identity/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── personaltraining/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── athletics/
│   ├── api/
│   ├── domain/
│   │   ├── model/
│   │   ├── service/
│   │   └── policy/
│   ├── application/
│   └── infrastructure/
├── programming/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── roster/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── competition/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
├── progression/
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
└── insights/
    ├── api/
    ├── domain/
    │   └── projection/
    ├── application/
    │   └── eventhandler/
    └── infrastructure/
```

Chaque sous-package peut contenir un `package-info.java` minimal pour le moment.

**`pom.xml`** avec les versions suivantes pinées explicitement :

- Java 25 (parent `<java.version>25</java.version>`)
- Spring Boot 4.1.0
- Spring Modulith (dernière version compatible Spring Boot 4.1)
- Spring Data JPA, Spring Web, Spring Security (via starters)
- PostgreSQL driver
- Flyway 11.x
- MapStruct 1.6.x
- springdoc-openapi 2.7.x
- Lombok (interdit dans `domain/` mais autorisé infrastructure, à minimiser)
- JUnit 5, AssertJ, Mockito, Testcontainers PostgreSQL, jqwik, Spring Modulith Test

**Configuration Spring Modulith** :
- Activer la vérification d'isolation via test (`ApplicationModules.of(AtlasApplication.class).verify()`).
- Ce test doit être dans `AtlasApplicationModulesTest` et tourner à chaque build.

**`AtlasApplication.java`** : la classe `@SpringBootApplication` minimale.

**`application.yml`** avec :
- Profil `local`, `test`, `prod`
- DataSource pointant sur localhost:5432/atlas en local
- Flyway activé
- springdoc OpenAPI sur `/swagger-ui`
- Logs structurés JSON (au moins en prod)

**Endpoint `/health`** : peut être l'actuator Spring (`/actuator/health`) avec exposition explicite, ou un controller custom dans `shared/`. Pour le sprint 0, le actuator suffit.

**Migration Flyway `V001__initial_schema.sql`** : peut être vide ou contenir juste une table `schema_version` de garde. L'objectif est que Flyway tourne et soit câblé.

### Frontend

**App Angular 22 dans `frontend/`** :
- Bootstrap avec `ng new atlas-frontend --routing --style=css --strict --ssr=false` (ou équivalent avec les conventions Angular 22 actuelles).
- Mode **zoneless** activé (par défaut Angular 22 mais à vérifier).
- **Tailwind CSS** configuré.
- Une page "Atlas — Hello world" avec un design minimal mais propre (Tailwind utility classes, palette neutre, aucune dépendance UI lourde).
- Une appel HTTP vers le backend `/actuator/health` qui vérifie que l'API répond, affiché en bas de page.
- Configuration CORS côté Spring Boot pour autoriser le frontend en dev.

### Infrastructure de dev

**`docker-compose.yml` à la racine** avec un service `postgres:17` exposant le port 5432, volume persistant.

**Scripts utilitaires dans `scripts/`** :
- `dev-start.sh` : lance docker-compose et l'app Spring Boot en parallèle
- `db-reset.sh` : drop/recreate la DB locale
- (Optionnel) `seed.sh` pour les données de test

### CI/CD

**`.github/workflows/ci.yml`** qui :
- Tourne à chaque push et chaque PR
- Build Maven (`mvn clean verify`)
- Lance les tests (incluant le test d'isolation Spring Modulith)
- Build le frontend (`npm ci && npm run build`)
- Cache des dépendances Maven et npm
- Ne déploie pas encore (déploiement Dokploy à configurer manuellement la première fois, puis automatisable en sprint 1 si pertinent)

---

## Règles d'architecture applicables (rappel)

- **Modular Monolith strict** : aucune dépendance circulaire, modules isolés via Spring Modulith. Le build doit échouer si l'isolation est violée.
- **DDD tactique** : domaine pur dans `domain/`, zéro Spring/JPA. Pour le sprint 0, les packages sont créés vides, donc la règle s'applique surtout au futur.
- **Conventions de naming** : pas de `Impl`, `Manager`, `Helper`. Voir CLAUDE.md section 5.
- **Pas de Lombok dans `domain/`**.
- **Tests sur PostgreSQL réel via Testcontainers**, jamais H2.
- **Versions pinées** : pas de version range, pas de `latest`.

---

## Definition of Done

Le sprint 0 est considéré terminé quand TOUS ces critères sont vérifiés :

- [ ] Le repo Git est initialisé avec un `.gitignore` propre (Java, Node, IDE, IntelliJ, VSCode)
- [ ] `mvn clean verify` passe sans erreur
- [ ] Le test `AtlasApplicationModulesTest` passe (Spring Modulith verify)
- [ ] L'app Spring Boot démarre en local (`mvn spring-boot:run`) sans erreur
- [ ] L'endpoint `/actuator/health` répond `200 OK` avec `{"status": "UP"}`
- [ ] Swagger UI accessible sur `http://localhost:8080/swagger-ui.html`
- [ ] `docker-compose up postgres` lance Postgres 17 sans erreur
- [ ] Flyway exécute la migration V001 avec succès au démarrage de l'app
- [ ] L'app Angular boot avec `ng serve` et affiche "Atlas — Hello world"
- [ ] L'app Angular affiche le status `UP` de l'API en bas de page (vérification CORS OK)
- [ ] La CI GitHub Actions passe au vert sur le commit initial
- [ ] Le devblog post `docs/blog/01-demarrage-atlas.md` est rédigé
- [ ] Le récap pédagogique de fin de session est produit (cf. format CLAUDE.md section 6)
- [ ] Le document `docs/learning/sprint-00-bootstrap.md` est créé avec les concepts clés du sprint (Spring Modulith intro, Java 25 features utiles, Angular 22 zoneless, etc.)
- [ ] Tous les commits suivent Conventional Commits
- [ ] Un fichier `docs/sprints/sprint-00-bootstrap/RETROSPECTIVE.md` résume ce qui s'est bien passé, ce qui a coincé, ce qu'on change pour la suite

---

## Contraintes à respecter

- **Pas de raccourcis sur l'archi.** Même si "ça marcherait" sans Spring Modulith, on l'installe. Même si une seule table suffit, on configure Flyway proprement.
- **Pas de déviation des versions.** Si un problème de compatibilité surgit avec Spring Boot 4.1, signale-le à Ryan, ne change pas la version unilatéralement.
- **Tests d'isolation activés dès le sprint 0.** Le test Spring Modulith doit tourner et passer.
- **Documentation à jour à chaque step.** README minimal mais à jour, ADRs déjà rédigés à intégrer dans le repo.
- **Une seule PR pour le sprint 0**, ou découpée en PR cohérentes (ex : "bootstrap maven", "bootstrap modulith", "bootstrap angular", "bootstrap ci") mais chacune mergeable indépendamment.

---

## Contexte métier

Sprint 0 est purement infrastructure. Pas de logique métier à implémenter, pas de modèle Fitness-Fatigue, pas d'aggregate. L'objectif est de poser les fondations techniques sur lesquelles tous les sprints futurs vont s'appuyer.

Le seul "contexte métier" pertinent ici est de **respecter l'ubiquitous language** dans les noms de packages et les modules. C'est `personaltraining` et pas `workouts`, `roster` et pas `team`.

---

## Format de récap attendu

À la fin du sprint, Claude doit produire :

**1. Récap pédagogique de session** (cf. CLAUDE.md section 6) :
- Ce qui a été fait
- Concepts clés introduits (Spring Modulith, structure Modular Monolith, Maven multi-module ou mono-module, Java 25 features, Angular 22 zoneless, configuration Flyway, Testcontainers basique, Conventional Commits, Spring Modulith verify)
- Pourquoi ces choix (référencer les ADRs)
- Points à vérifier toi-même (lectures recommandées : doc officielle Spring Modulith, refcard Maven, release notes Spring Boot 4.0/4.1)
- Questions de contrôle

**2. Mini-cours sprint** (`docs/learning/sprint-00-bootstrap.md`) :
- Mini-cours sur Spring Modulith (qu'est-ce que c'est, pourquoi, comment ça marche, comment l'utiliser au quotidien dans Atlas)
- Mini-cours sur la structure d'un Modular Monolith DDD-ready
- Mini-cours sur les nouveautés Java 25 utiles pour le projet (records, sealed classes, pattern matching, virtual threads en bref)
- Auto-évaluation

---

## Première instruction concrète

Quand tu commences l'exécution du sprint 0 :

1. Lis CLAUDE.md, vision.md, glossary.md, et les ADRs.
2. Confirme à Ryan que tu as bien tout lu et que tu comprends l'objectif.
3. Propose à Ryan un **plan d'exécution séquentiel** : dans quel ordre tu vas créer les artefacts, en combien d'étapes, avec des points de validation entre chaque.
4. Attends la validation de Ryan **avant de coder**.
5. Exécute étape par étape, en demandant validation à la fin de chaque étape avant de passer à la suivante.

Pas de mode "génère tout d'un coup" sur ce sprint critique. C'est la fondation : on prend le temps.

---

*Sprint 0 — Bootstrap. Estimé 1-2 semaines à 10-15h/semaine.*
