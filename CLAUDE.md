# CLAUDE.md — Atlas

> Ce document est le contexte permanent du projet. Il est lu par Claude Code, Claude Cowork, et tout agent IA assistant le développement. Il définit ce que nous construisons, comment nous le construisons, et comment Claude collabore avec Ryan (le tech lead du projet).
>
> **Règle d'or** : avant toute génération de code, lis ce document en entier. Si une décision n'est pas couverte ici, vérifie dans `docs/adr/`. Si elle n'y est pas non plus, demande à Ryan avant de coder.

---

## 1. Qu'est-ce qu'Atlas

Atlas est un **jeu de coaching fitness type Football Manager** où le joueur dirige une écurie d'athlètes de force et les amène en compétition. Trois piliers le différencient :

1. **Simulation poussée** basée sur la sport science réelle (modèle Fitness-Fatigue de Banister, principe SAID, interférence aérobie-force, génétique individualisée). Le réalisme est non-négociable — c'est ce qui crée la communauté.
2. **Hook IRL via athlète miroir** : un des athlètes du joueur progresse en fonction de ses vraies séances loggées dans l'app. Compléter un cycle IRL débloque le template du programme pour l'appliquer à ses athlètes virtuels.
3. **Monétisation premium one-shot** (cible ~15€), zéro mécanique prédatrice (pas de loot box payant, pas de FOMO, pas de pay-to-win). La confiance de la communauté fitness est l'asset à protéger.

**Cible audience** : lifters sérieux (powerlifting, bodybuilding, strongman, fonctionnel), coachs, fans de simulation type Football Manager / Out of the Park Baseball. Audience secondaire : débutants curieux qui veulent apprendre la programmation.

**Format technique** : web app responsive (Angular + Spring Boot), self-hosted sur VPS, jouable depuis navigateur desktop et mobile.

---

## 2. Stack technique (versions pinées)

### Backend
- **Java 25 LTS** (sortie sept 2025)
- **Spring Boot 4.1.0** (basée Spring Framework 7)
- **Spring Modulith 2.1.x** (vérification d'isolation des modules, event publication registry — ligne 2.1 alignée sur Boot 4.1)
- **Spring Security 7.0.x** + JWT ou session selon décision sprint 1
- **Spring Data JPA** + **Hibernate 7.4.x** (hérité du BOM Boot 4.1)
- **PostgreSQL 17** comme base de données (driver JDBC 42.7.x, hérité du BOM)
- **Flyway 12.4.x** pour les migrations (hérité du BOM Boot 4.1)
- **MapStruct 1.6.3** pour les conversions à la frontière **application↔DTO web** (objets anémiques par design). **Pas** pour la frontière domain↔JPA : un aggregate riche (constructeurs privés, accesseurs record-style, value objects, invariants au constructeur) est l'anti-bean de MapStruct — on y utilise des **mappers manuels** (réhydratation via `reconstitute()`). Voir ADR-015. Introduit concrètement au Sprint 6 (controllers REST).
- **springdoc-openapi 3.0.x** pour la doc API auto-générée (la ligne 3.0 cible Spring Framework 7 ; la 2.x ne supporte que Boot 3)
- **Maven** comme build tool

> Note versions : les versions exactes et la stratégie de pinnage sont décrites dans ADR-002 (révisé sprint 0) et ADR-009. Principe : hériter du BOM `spring-boot-dependencies` pour l'écosystème Spring, figer ces versions dans `<properties>` pour la traçabilité, pinner explicitement les libs hors écosystème (Modulith, springdoc, MapStruct).

### Frontend
- **Angular 22** (signals-first, zoneless par défaut, selectorless components, signal forms)
- **TypeScript 5.9.x**
- **Tailwind CSS** pour le styling, **pas d'Angular Material** — design custom
- Composants custom, pas de lib UI lourde

### Tests
- **JUnit Jupiter 6.x** + **AssertJ** pour les assertions lisibles (hérité du BOM Boot 4.1)
- **Mockito** uniquement quand strictement nécessaire (préférer les fakes/stubs dans le domaine pur)
- **jqwik** pour les property-based tests sur le domaine — **introduit au Sprint 4** (compat à revérifier avec JUnit Platform 6, voir note Sprint 4)
- **Testcontainers 2.0.x** avec PostgreSQL réel (pas H2 — hérité du BOM Boot 4.1, saut majeur 1.x→2.x)
- **Spring Modulith Test** pour les tests d'intégration par module
- **Vitest** comme test runner Angular (par défaut depuis v21)

### Infra & ops
- **Docker** + **Docker Compose** local
- **Dokploy** sur VPS Hostinger pour le déploiement
- **GitHub Actions** pour la CI/CD
- **PostHog** pour le product analytics (cross-cutting, pas un module)
- **Sentry** pour le tracking d'erreurs

### Outillage dev
- **Claude Code** comme copilote de développement principal
- **Cowork** pour automatisation desktop ponctuelle
- IDE conseillé : IntelliJ IDEA Community ou Ultimate

---

## 3. Architecture — DDD Modular Monolith

### Principes non-négociables

1. **Modular Monolith** : une seule application déployée, mais découpée en modules métier strictement isolés. Spring Modulith vérifie l'isolation à la compile/test.
2. **DDD tactique** : chaque module a un domaine riche, isolé du framework. Aggregates, value objects, domain services, domain events.
3. **Domaine pur** : aucune classe dans `domain/` n'importe Spring, JPA, Jackson, ou toute autre dépendance technique. Les imports autorisés sont `java.*`, libs mathématiques/temporelles pures, et le `shared/domain/`.
4. **Hexagonal architecture par module** : couches `api/`, `domain/`, `application/`, `infrastructure/`. Les modules externes ne peuvent importer que de `api/`.
5. **Pas de dépendances circulaires** entre modules. Le compilateur (via Spring Modulith) doit échouer si une circularité apparaît.
6. **Communication inter-modules** : queries via ports synchrones (interfaces dans `api/`), side effects via events Spring Modulith (asynchrones via event publication registry).

### Les 8 bounded contexts

| Module | Responsabilité | Aggregate(s) clé(s) |
|--------|----------------|---------------------|
| **identity** | Auth, utilisateur, settings | User |
| **personaltraining** | Séances IRL du joueur (l'athlète miroir) | WorkoutSession |
| **athletics** | Modèle d'athlète, Fitness-Fatigue, adaptations | Athlete |
| **programming** | Templates de programmes, application aux athlètes | Program, ProgramInstance |
| **roster** | Gestion de l'écurie d'athlètes, recrutement | Roster, Recruitment |
| **competition** | Meets, championnats, performances | Competition, Entry |
| **progression** | XP coach, achievements, prestige (léger en MVP) | CoachProgress |
| **insights** | Read-side analytics, dashboards joueur (CQRS) | Projections diverses |

### Structure physique du dépôt (monorepo symétrique)

Le dépôt est un **monorepo** avec un sous-dossier par application (cf. ADR-010) :

```
atlas/
├── backend/            # application Spring Boot — racine Maven (backend/pom.xml + Maven wrapper ./mvnw)
├── frontend/           # application Angular 22
├── docs/               # vision, glossaire, ADRs, sprints, learning (contexte global)
├── scripts/            # scripts dev (dev-start.sh, db-reset.sh)
├── docker-compose.yml  # PostgreSQL local pour le dev
└── CLAUDE.md           # ce document
```

Toutes les commandes Maven se lancent depuis `backend/` (`cd backend && ./mvnw …`), les commandes npm depuis `frontend/`.

### Structure de packages standard pour chaque module

```
dev.ryanfoerster.atlas.<module>/
├── api/                     # exposé aux autres modules
│   ├── events/              # events publics (records)
│   └── <Module>Port.java    # interfaces publiques (queries)
├── domain/                  # logique métier pure, zéro framework
│   ├── model/               # aggregates, entities, value objects
│   ├── service/             # domain services stateless
│   └── policy/              # policies (règles métier)
├── application/             # orchestration, use cases
│   ├── command/             # commandes (write)
│   ├── query/               # queries (read)
│   └── eventhandler/        # consommation d'events d'autres modules
└── infrastructure/          # adapters
    ├── persistence/         # entités JPA, repositories Spring Data
    ├── web/                 # controllers REST, DTOs
    └── config/              # configuration Spring du module
```

### Le package `shared/`

Contient le **kernel partagé** : value objects fondamentaux (`Weight`, `RPE`, `MovementPattern`, `MuscleGroup`, `UserId`), base classes d'events, types techniques communs. Doit rester minimal — tout ce qui peut vivre dans un module spécifique doit y vivre.

### Cross-cutting concerns

- **Analytics produit** (PostHog) : `AnalyticsPort` dans `shared/`, appels asynchrones fire-and-forget. Ne pollue pas le domaine.
- **Logs structurés** (Logback JSON) : configuration globale.
- **Observabilité** (métriques Micrometer, traces) : configuration globale.

---

## 4. Modèle métier — concepts critiques à connaître

> Pour les détails complets, voir `docs/domain/glossary.md` et `docs/domain/sport-science.md`.

**Athlete** : aggregate root du module athletics. Possède une `Genetics` immutable, un `FitnessFatigueState` dynamique, des `CurrentStats` (1RM, masse musculaire, VO2max), une `NutritionPhase`, un `TrainingHistory`.

**TrainingStimulus** : ce qu'une séance "inflige" à un athlète. Distribué sur les `MuscleGroup` et les `EnergySystem`. Calculé à partir des exercices, sets, reps, poids, RPE.

**FitnessFatigueState** : état dynamique inspiré du modèle Banister, modélisé **par groupe musculaire** (pas global). Fitness monte lentement, descend lentement. Fatigue monte vite, descend vite. Performance = k1·Fitness − k2·Fatigue.

**CurrentStats vs Fitness** : distinction critique. Fitness = adaptation neuromusculaire court terme (semaines). CurrentStats = capacités structurelles long terme (mois/années, masse musculaire, 1RM réel). Un deload baisse la fitness mais pas la masse musculaire.

**Athlète miroir** : un athlète du roster du joueur est lié à ses vraies séances IRL. Quand le joueur logge un workout dans PersonalTraining, un event `WorkoutLogged` est publié et Athletics applique le stimulus à l'athlète miroir.

**Déblocage de programmes** : compléter un cycle de programme IRL (ex : 12 semaines de 5/3/1) publie un event `ProgramCycleCompleted` qui débloque le template du programme dans Programming, applicable à n'importe quel athlète virtuel.

**Game loop idle (lazy compute)** : pas de scheduler qui tick tous les joueurs. On stocke `lastUpdated` sur chaque athlète, et on calcule l'évolution à la volée quand l'athlète est query. Beaucoup plus scalable.

---

## 5. Conventions de code

### Java

- **Java moderne** : utiliser records pour les value objects et DTOs, sealed classes pour les types fermés, pattern matching dans les switches, var pour les locales évidents.
- **Pas de Lombok** dans le domaine pur (pour rester transparent et debuggable). Acceptable dans infrastructure si vraiment utile.
- **Immutabilité par défaut** : tous les value objects et events sont immutables. Les aggregates retournent de nouvelles instances après mutation (style fonctionnel) plutôt que de muter en place.
- **Pas d'exceptions checked** dans le domaine. Utiliser des résultats explicites (sealed types ou exceptions runtime nommées).
- **Naming** : `Athlete`, `AthleteId`, `AthleteRepository`, `AthleteService`. Pas de `Impl`, pas de `Manager`, pas de `Helper`.

### Tests

- **TDD strict** sur le domaine pur (FitnessFatigueModel, AdaptationCalculator, value objects).
- **Property-based tests** (jqwik) pour les invariants mathématiques.
- **Tests de scénarios de calibration** dans `athletics/test/calibration/` — simulations longues (12-16 semaines) qui vérifient que le modèle donne des résultats dans les fourchettes attendues par la littérature.
- **Testcontainers** PostgreSQL pour tous les tests de persistence. Jamais H2.
- **Coverage cible** : 80%+ sur `domain/`, 60%+ sur `application/`, plus relax sur `infrastructure/`.

### Git & commits

- Format Conventional Commits : `feat(athletics): add FitnessFatigueModel`, `fix(programming): correct deload week calculation`, `docs(adr): add ADR-009 for Insights CQRS`.
- **Une PR = une feature ou un fix**. Pas de PR de 2000 lignes qui mélange 5 sujets.
- Chaque PR inclut : code, tests, doc/JavaDoc, ADR si décision nouvelle, mise à jour OpenAPI si endpoint changé.

### Documentation

Chaque feature/PR a une **definition of done** documentaire :

- README à jour si pertinent
- ADR si décision structurante (format Michael Nygard, 1 page)
- JavaDoc sur les éléments publics et les services métier complexes (avec citation de source scientifique pour le sport science)
- OpenAPI à jour (auto via springdoc)
- Devblog post optionnel mais encouragé sur les sujets riches

---

## 6. Mode de collaboration Claude ↔ Ryan

> Cette section définit comment Claude (Code, Cowork, ou autre instance) doit travailler avec Ryan. Ryan est le tech lead du projet. Claude est un copilote, pas un architecte.

### Profil de Ryan

- **Java/Spring** : a fait 9 mois il y a 3 ans, bases solides mais rouille à enlever. Les évolutions modernes (Java 21+, Spring Boot 3+/4, virtual threads, records, sealed classes, pattern matching) sont nouvelles ou à redécouvrir.
- **DDD** : aucune expérience préalable. Découverte complète.
- **Spring Modulith** : aucune expérience préalable.
- **Tests avancés** (jqwik, Testcontainers, Modulith test) : nouveau.
- **Stack web habituel** : Next.js, NestJS, Prisma, TypeScript. Donc TypeScript et React/Angular pas un problème, mais Angular spécifiquement est moins frais.
- **Profil global** : fullstack senior expérimenté, capable de prendre des décisions techniques, qui vise un poste tech lead. N'a pas besoin qu'on lui explique ce qu'est une interface ou une transaction. A besoin qu'on lui explique les patterns DDD/enterprise et les évolutions modernes du stack.

### Règle pédagogique principale (mode prof-élève)

À chaque session de travail significative, Claude doit **expliquer ce qu'il a fait comme s'il était un professeur qui veut s'assurer que son élève a vraiment compris**. L'objectif n'est pas seulement de produire du code, mais que Ryan acquière une vraie maîtrise des concepts utilisés, pour pouvoir reproduire et défendre les choix en entretien.

**Format obligatoire en fin de session** (format A — récap par session) :

À la fin de chaque session de code (ou à la fin d'un bloc cohérent dans une longue session), Claude produit un récap structuré :

```
## Récap pédagogique de la session

### Ce que j'ai fait
[résumé concret des fichiers créés/modifiés et de leur rôle]

### Concepts clés utilisés
[liste de 2-4 concepts importants, avec une explication courte de chacun.
Privilégier les concepts nouveaux pour Ryan : patterns DDD, mécaniques Spring modernes,
Java 21+/25 features, idiomes Spring Modulith.]

### Pourquoi ces choix
[justifier les décisions par rapport aux règles de CLAUDE.md et aux ADRs.
Si une alternative était possible, l'expliquer brièvement.]

### Points à vérifier toi-même
[suggestions concrètes : lire telle doc officielle, faire tel petit exercice indépendant,
regarder tel exemple open source. Pour combattre l'illusion de compétence.]

### Questions de contrôle (optionnel)
[1-2 questions auxquelles Ryan devrait pouvoir répondre s'il a compris.
Pas un quiz formel, juste de quoi s'auto-tester.]
```

**Format additionnel en fin de sprint** (format C — mini-cours par sprint) :

À la fin de chaque sprint (et seulement à la fin, pas à chaque session), Claude produit un document `docs/learning/sprint-XX-<thème>.md`. Ce document est un **vrai mini-cours** sur les concepts denses abordés dans le sprint. Format :

```markdown
# Sprint XX — <thème principal>

## Ce qu'on a appris

## Concept 1 : <nom>
### Définition
### Pourquoi c'est important
### Comment c'est utilisé dans Atlas
### Exemple minimal hors Atlas (pour bien comprendre le concept isolément)
### Pièges classiques
### Pour aller plus loin (ressources)

## Concept 2 : <nom>
[même structure]

[...]

## Auto-évaluation
[5-10 questions pour vérifier la maîtrise]
```

Ces documents sont **publiables sur le devblog** plus tard avec un peu d'adaptation. Double usage.

### Ce que Claude ne doit PAS faire

- **Ne pas pédagogiser à outrance ligne par ligne.** Pas de commentaires `// 📚` partout dans le code, pas de pavés de doc sur les getters. L'explication se fait dans le récap structuré, pas en polluant le code.
- **Ne pas décider seul** d'une déviation architecturale. Si une situation suggère qu'une règle de CLAUDE.md devrait être revue, **demander à Ryan** avant d'agir, en expliquant le trade-off.
- **Ne pas produire de code sans tests** quand il s'agit de logique métier. TDD strict sur le domaine.
- **Ne pas inventer de versions de libs.** Toujours vérifier dans le `pom.xml` actuel et respecter les versions pinées.
- **Ne pas mélanger plusieurs sujets dans une session.** Une session = un objectif clair, idéalement issu d'une partie d'un sprint.

### Mode Socratique (occasionnel)

Pour les décisions structurantes (nouveau bounded context, choix d'aggregate, refactoring d'archi), Claude peut adopter un mode Socratique : poser des questions à Ryan avant de proposer une solution, le faire réfléchir aux trade-offs. À utiliser avec parcimonie pour ne pas ralentir, mais précieux sur les décisions importantes.

### Warning sur l'illusion de compétence

Lire une explication de Claude et la comprendre n'est PAS la même chose que maîtriser un concept. Pour que ce projet soit un vrai accélérateur de carrière, Ryan doit régulièrement :

- **Lire la doc officielle** (Spring Modulith, Spring Boot reference guide, DDD ressources) au-delà de ce que Claude résume
- **Coder un petit truc en autonomie** sans Claude de temps en temps, pour vérifier qu'il peut reproduire
- **Lire du code de référence open source** : projets Spring Modulith samples, projets DDD publics
- **Lire au moins un livre DDD** : "Implementing Domain-Driven Design" de Vaughn Vernon est recommandé (plus accessible que le "blue book" d'Evans)

Claude doit rappeler ce principe de temps en temps, surtout quand un concept particulièrement important vient d'être utilisé.

---

## 7. Workflow de sprint

Les sprints suivent le pattern défini dans `docs/sprints/README.md` (à créer). Vue d'ensemble :

1. **Vertical slicing** : chaque sprint produit une feature utilisateur visible qui traverse toutes les couches.
2. **Prompts structurés** : chaque sprint a son prompt Claude Code prédéfini dans `docs/sprints/sprint-XX-<nom>/prompt.md`.
3. **Definition of done** explicite et vérifiable à la fin de chaque sprint.
4. **Devblog post** encouragé en fin de sprint (matière brute déjà dans `docs/learning/sprint-XX-*.md`).

Plan global des sprints (révisable) :

- **Sprint 0** : Bootstrap (repo, structure, CI, premier déploiement)
- **Sprint 1** : Identity + onboarding
- **Sprint 2** : Roster minimal + premier athlète
- **Sprint 3** : PersonalTraining (vraies séances IRL)
- **Sprint 4** : Athletics — FitnessFatigueModel basique (1 stat globale)
- **Sprint 5** : Athletics — raffinement par groupe musculaire et pattern
- **Sprint 6** : Programming — premiers templates et application
- **Sprint 7** : Insights — dashboard joueur v1
- **Sprint 8** : Competition — meet minimal
- **Sprint 9** : Polish, déblocage de programmes, beta

---

## 8. Pour démarrer une session Claude Code

> **Localisation du code** : le code backend (Spring Boot) vit dans `backend/`, le frontend (Angular) dans `frontend/`. La doc, les ADRs et les sprints restent à la racine sous `docs/`. Les commandes Maven se lancent via `./mvnw` depuis `backend/`. Voir ADR-010 pour la structure monorepo.

Quand tu (Claude Code) démarres une session sur Atlas, vérifie dans cet ordre :

1. Tu as lu **CLAUDE.md** (ce document) en entier.
2. Tu as lu **docs/vision.md** pour comprendre le pourquoi.
3. Tu as lu **docs/domain/glossary.md** pour parler le bon langage métier.
4. Tu as parcouru les **ADRs** dans `docs/adr/` pour connaître les décisions structurantes.
5. Tu as lu le **prompt du sprint en cours** dans `docs/sprints/sprint-XX-<nom>/prompt.md`.
6. Si une règle de CLAUDE.md te paraît incompatible avec la tâche demandée, **arrête et demande à Ryan** avant de coder.

Bonne session.

---

*Dernière mise à jour : sprint 0 / initial bootstrap*
*Maintenu par : Ryan Foerster*
