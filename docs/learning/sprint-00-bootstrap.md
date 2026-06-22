# Sprint 00 — Bootstrap : mini-cours

> Mini-cours sur les concepts denses abordés pendant le Sprint 0 (bootstrap d'Atlas). Double usage : consolidation pour Ryan, et matière brute pour le devblog. Chaque concept suit la même structure : définition, pourquoi c'est important, comment c'est utilisé dans Atlas, exemple minimal hors Atlas, pièges classiques, pour aller plus loin.
>
> **Avertissement anti-illusion de compétence** : lire et comprendre ce document n'est PAS la même chose que maîtriser ces concepts. Pour chaque section, le vrai test est de pouvoir reproduire et expliquer sans le document sous les yeux. L'auto-évaluation finale sert à ça.

## Ce qu'on a appris

Le Sprint 0 n'a produit aucune logique métier — c'était volontaire. Tout l'apprentissage est dans les **fondations** : comment structurer un modular monolith, comment l'outiller pour qu'il reste discipliné, comment câbler une persistence testée sur une vraie base, comment monter un frontend moderne et une CI propre. Onze concepts structurants, du plus architectural au plus opérationnel.

---

## Concept 1 : Modular Monolith avec Spring Modulith

### Définition
Un **modular monolith** est une seule application déployable (un seul process, un seul artefact) mais découpée en **modules métier strictement isolés**, chacun avec sa frontière et son API publique. C'est un point milieu entre le monolithe « boule de boue » (tout couplé) et les microservices (tout distribué). **Spring Modulith** est l'outillage Spring qui *vérifie* cette isolation : il analyse statiquement le code et fait échouer le build si un module viole les règles.

### Pourquoi c'est important
La discipline architecturale qui repose sur la seule volonté humaine s'érode toujours sous la pression du temps. Spring Modulith transforme « on s'est mis d'accord pour ne pas faire ça » en « le build casse si tu fais ça ». C'est la différence entre une convention et une contrainte. Et contrairement aux microservices, on garde la simplicité opérationnelle d'un seul déploiement, d'une seule base de données, de transactions locales — tout en gardant la porte ouverte à une extraction future en services séparés, module par module.

### Comment c'est utilisé dans Atlas
- Les 8 bounded contexts (`identity`, `personaltraining`, `athletics`, `programming`, `roster`, `competition`, `progression`, `insights`) sont des sous-packages directs de `dev.ryanfoerster.atlas`. Spring Modulith les détecte automatiquement comme modules (vérifié au sprint 0 : `Athletics, Competition, Identity, …, Shared`).
- Le test `AtlasApplicationModulesTest.verifiesModuleIsolation()` appelle `ApplicationModules.of(AtlasApplication.class).verify()`. Il tourne à **chaque build** (donc dans la CI). Toute dépendance illégale (importer le `domain/` d'un autre module au lieu de son `api/`) ou tout cycle entre modules fait échouer ce test.
- On génère aussi la **doc C4** des modules (`Documenter.writeDocumentation()` → diagrammes PlantUML + canvases) comme artefact tangible de la structure (ADR-001).
- Communication inter-modules prévue : queries via ports synchrones (interfaces dans `api/`), side-effects via events Spring Modulith.

### Exemple minimal hors Atlas
Imagine une app e-commerce avec deux modules `orders` et `inventory` :
```
com.shop/
├── orders/        OrderService (api), logique interne (domain)
└── inventory/     StockService (api), logique interne (domain)
```
Si `orders` importe une classe **interne** d'`inventory` (pas son `api`), `ApplicationModules.of(ShopApp.class).verify()` lève une `Violations` et le build échoue. Pour communiquer légalement, `orders` n'utilise que `inventory.api.StockService`, ou écoute un event `StockDepleted`.

### Pièges classiques
- **Croire que `verify()` qui passe = isolation prouvée.** Sur des modules vides (comme au sprint 0), `verify()` passe trivialement. La vraie preuve, c'est un test qui **échoue quand on introduit une violation** (reporté au sprint 1, cf. `sprint-01-identity/NOTES.md`).
- **Confondre module Modulith et package Java.** Un module = sous-package direct du package racine. Les sous-sous-packages (`domain`, `infrastructure`…) sont des *internals* du module, pas des modules.
- **Vouloir tout exposer.** Plus l'`api/` d'un module est large, moins l'isolation a de valeur. L'API doit être le strict minimum.

### Pour aller plus loin
- Doc officielle Spring Modulith (reference guide) — sections *Application Modules*, *Verifying Module Structure*, *Documenting*.
- Spring Modulith samples (GitHub `spring-projects/spring-modulith`).
- Livre : *Building Modular Monoliths* / conférences de Oliver Drotbohm (l'auteur de Modulith).

---

## Concept 2 : DDD tactique et structure hexagonale par module

### Définition
Le **DDD tactique** est l'ensemble des patterns d'implémentation du Domain-Driven Design : aggregates, entities, value objects, domain services, domain events, policies. L'**architecture hexagonale** (ports & adapters) isole le cœur métier (le domaine) des détails techniques (base de données, web, framework) : le domaine définit des *ports* (interfaces), l'infrastructure fournit des *adapters* (implémentations). Dans Atlas, chaque module a sa propre structure hexagonale.

### Pourquoi c'est important
Le cœur d'Atlas (simulation Fitness-Fatigue, calculs d'adaptation) est complexe et critique pour la crédibilité du jeu. On veut pouvoir le tester en millisecondes, le lire comme un livre de game design, et le faire évoluer sans casser l'infrastructure. La règle d'or (ADR-003) : **le `domain/` n'importe AUCUN framework** (ni Spring, ni JPA, ni Jackson). Imports autorisés : `java.*`, maths/temps purs, et le `shared/domain/`. Conséquence directe : les tests de domaine ne démarrent ni Spring ni base de données.

### Comment c'est utilisé dans Atlas
- Structure par module : `api/` (exposé), `domain/` (pur), `application/` (orchestration : commands, queries, event handlers), `infrastructure/` (adapters : JPA, REST, config).
- Les **entités JPA vivent dans `infrastructure/persistence/`**, séparées des aggregates du domaine ; le mapping domaine ↔ JPA passe par MapStruct (introduit au sprint 1).
- Les aggregates sont **immutables** : ils retournent une nouvelle instance après « mutation » (style fonctionnel). Les domain services sont **stateless** : `(état + commande) → nouvel état`, sans effet de bord.
- Au sprint 0, les packages sont créés vides (squelette) — la règle s'appliquera au code des sprints suivants.

### Exemple minimal hors Atlas
Un value object `Money` pur, sans framework :
```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.signum() < 0) throw new IllegalArgumentException("amount must be >= 0");
    }
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }
}
```
Testable sans rien démarrer ; aucune annotation. L'`OrderEntity` JPA (avec `@Entity`, `@Column`) vit ailleurs, en infrastructure, et un mapper convertit l'un en l'autre.

### Pièges classiques
- **Laisser fuiter JPA dans le domaine** « pour gagner du temps » (mettre `@Entity` sur l'aggregate). C'est le raccourci qui tue tout le bénéfice : le domaine devient intestable sans base, et couplé au cycle de vie Hibernate.
- **Anémie du domaine** : mettre toute la logique dans les services et laisser les aggregates n'être que des sacs de getters/setters. Le DDD veut des aggregates *riches* qui protègent leurs invariants.
- **Sur-ingénierie** : tout n'est pas un aggregate. Un value object suffit souvent.

### Pour aller plus loin
- Vaughn Vernon, *Implementing Domain-Driven Design* (plus accessible que le « blue book » d'Evans).
- Alistair Cockburn, article original *Hexagonal Architecture (Ports & Adapters)*.
- jMolecules (annotations qui rendent les concepts DDD explicites et vérifiables, compatibles Modulith).

---

## Concept 3 : Anatomie du pom.xml moderne

### Définition
Le `pom.xml` est le descripteur de build Maven. Quatre mécanismes en sont le cœur, souvent confondus : le **parent**, le **dependencyManagement**, les **dependencies**, et l'import de **BOM**.

### Pourquoi c'est important
Comprendre qui décide de quelle version, et où, est la clé pour ne pas subir des conflits de classpath obscurs. La confusion classique : « j'ai déclaré la lib, pourquoi la version n'est-elle pas celle que je veux ? » ou « pourquoi je n'ai pas à mettre de version ici ? ».

### Comment c'est utilisé dans Atlas (les 4 mécanismes)
- **`<parent>`** = héritage. Notre parent est `spring-boot-starter-parent` 4.1.0. On hérite de sa configuration de plugins, de ses defaults, et surtout de sa **gestion de versions** (un `dependencyManagement` géant). `<relativePath/>` **vide** dit à Maven : « ne cherche pas ce parent sur le disque local, va directement au dépôt distant ».
- **`<dependencyManagement>`** = *alignement de versions sans déclaration*. Lister une dépendance ici n'ajoute RIEN au classpath ; ça dit juste « si quelqu'un utilise cette lib, voici la version ». C'est là qu'on importe le **BOM Spring Modulith**.
- **`<dependencies>`** = *déclaration effective*. C'est ce qui ajoute réellement la lib au classpath. Si sa version est gérée (par le parent ou un BOM), on l'**omet**.
- **BOM import scope** : `<type>pom</type><scope>import</scope>` agrège tout le `dependencyManagement` d'un autre POM dans le nôtre. Différence cruciale avec le parent : on n'a **qu'un seul** parent, mais on peut importer **plusieurs** BOMs. C'est pourquoi Spring Modulith (qui a son propre BOM) est *importé*, pas hérité.
- **Starter** (`spring-boot-starter-*`) = méta-package d'opinion : il agrège un ensemble cohérent de dépendances + l'auto-configuration associée. `spring-boot-starter-web` ≠ juste Spring MVC : c'est MVC + Tomcat + Jackson + validation, cohérents entre eux.

### Exemple minimal hors Atlas
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.0</version>
  <relativePath/>           <!-- vide → parent récupéré du dépôt distant -->
</parent>

<dependencyManagement>     <!-- aligne, n'ajoute rien -->
  <dependencies>
    <dependency>
      <groupId>org.springframework.modulith</groupId>
      <artifactId>spring-modulith-bom</artifactId>
      <version>2.1.0</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>             <!-- ajoute au classpath -->
  <dependency>            <!-- version omise : gérée par le parent -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
</dependencies>
```

### Pièges classiques
- **Redéclarer une version déjà gérée** par le parent/BOM avec une valeur divergente → on casse la cohésion testée par Spring. (Sauf intention explicite et tracée, cf. concept 6.)
- **Croire que `dependencyManagement` ajoute la dépendance** : non, il faut aussi la déclarer dans `<dependencies>`.
- **Oublier `<relativePath/>`** quand on n'a pas de parent local : Maven perd du temps (ou échoue) à chercher un parent sur le disque.

### Pour aller plus loin
- Maven : *Introduction to the Dependency Mechanism* (doc officielle).
- Spring Boot Reference — *Dependency Management*.

---

## Concept 4 : Modularisation de l'autoconfigure dans Spring Boot 4 (la rupture vs Boot 3)

### Définition
En Spring Boot 3.x, presque toutes les **auto-configurations** vivaient dans un seul jar monolithique `spring-boot-autoconfigure`. En **Spring Boot 4.x, ce jar a été éclaté** en de nombreux modules par technologie : `spring-boot-flyway`, `spring-boot-tomcat`, `spring-boot-health`, `spring-boot-jdbc`, etc.

### Pourquoi c'est important
C'est **la** rupture structurante de Boot 4 qui va piéger énormément de devs en migration 3→4. La conséquence est contre-intuitive : avoir une bibliothèque sur le classpath ne suffit plus à déclencher son auto-configuration. Il faut le **module d'auto-config dédié** (généralement tiré par le starter correspondant).

### Comment c'est utilisé dans Atlas (le bug vécu au sprint 0)
On avait ajouté `flyway-core` directement. La lib était présente, **aucune erreur**, mais Flyway ne s'exécutait pas : la table `flyway_schema_history` n'était jamais créée. Diagnostic : le rapport `--debug` ne mentionnait même pas `FlywayAutoConfiguration`. La classe d'auto-config n'était pas sur le classpath — elle vit désormais dans le module `spring-boot-flyway`, tiré par **`spring-boot-starter-flyway`**. Remplacer `flyway-core` par le starter a tout débloqué (`Migrating schema "public" to version "001"`).

### Exemple minimal hors Atlas
Symptôme typique en migration Boot 3→4 : « j'avais `flyway-core` (ou `micrometer`, ou autre) et tout marchait en Boot 3 ; après upgrade en Boot 4, plus rien ne se configure tout seul ». Réflexe : remplacer la dépendance « nue » par le **starter** Boot correspondant (`spring-boot-starter-xxx`), qui ramène le module d'auto-config.

### Pièges classiques
- **Diagnostiquer dans son propre code** alors que le problème est l'absence du module d'auto-config. Le bon réflexe : lancer avec `--debug` (ou `-Ddebug=true` en test) et chercher si la `XxxAutoConfiguration` apparaît dans le *Conditions Evaluation Report*. Si elle est **absente** (et pas juste « did not match »), c'est qu'elle n'est pas sur le classpath.
- **Ajouter la lib nue par habitude Boot 3** au lieu du starter.

### Pour aller plus loin
- Spring Boot 4.0 *Release Notes* et *Migration Guide* (section sur la modularisation).
- Lancer une app avec `--debug` et lire le *Conditions Evaluation Report* : exercice formateur pour comprendre l'auto-config.

---

## Concept 5 : `@ServiceConnection` + singleton container (Testcontainers moderne)

### Définition
**Testcontainers** lance de vrais services (ici PostgreSQL) dans des containers Docker pour les tests d'intégration — jamais de H2 ou de mock (ADR-008). **`@ServiceConnection`** (Spring Boot 3.1+) auto-configure la `DataSource` à partir du container. Le **singleton container pattern** réutilise un container unique entre toutes les classes de test au lieu d'en démarrer un par classe.

### Pourquoi c'est important
Tester sur une vraie base élimine les bugs de prod silencieux (dialectes, types, comportements spécifiques à PostgreSQL). Mais démarrer un container coûte quelques secondes — multiplié par chaque classe de test, ça devient pénalisant. `@ServiceConnection` supprime le boilerplate de câblage, et le singleton supprime le coût répété de démarrage. Beaucoup de devs Spring ignorent encore `@ServiceConnection` (réflexe `@DynamicPropertySource` hérité d'avant 3.1).

### Comment c'est utilisé dans Atlas
`AbstractIntegrationTest` :
```java
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    private static final String POSTGRES_VERSION = System.getProperty("postgres.image.version", "17");

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + POSTGRES_VERSION));

    static { POSTGRES.start(); }   // démarré une fois, partagé par héritage, jamais arrêté (Ryuk nettoie)
}
```
Le `PersistenceSmokeIntegrationTest` en hérite et prouve que Flyway tourne sur un vrai Postgres. La version de l'image vient d'une propriété Maven (cf. concept 6 / la synchro de version).

### Avant / après (`@DynamicPropertySource` → `@ServiceConnection`)
```java
// AVANT (Boot < 3.1) : câblage manuel, verbeux, à refaire pour chaque service
@DynamicPropertySource
static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
}
// APRÈS : @ServiceConnection sur le champ container, et c'est tout.
```

### Exemple minimal hors Atlas
Un test qui a juste besoin d'un Redis réel : un `@ServiceConnection static GenericContainer<?> redis = …;` et Spring Boot configure tout seul `spring.data.redis.*`. Pas une ligne de propriété à écrire.

### Pièges classiques
- **Démarrer un container par classe** (via `@Container` + `@Testcontainers`) sans s'en rendre compte → tests lents. Le singleton (champ `static` démarré une fois, partagé par héritage) règle ça.
- **Confondre isolation et recreate** : entre deux tests, on isole les données par **truncate** des tables, PAS en recréant le container. Recréer serait correct mais ruinerait la perf.
- **Oublier que `127.0.0.1` peut être en `trust`** dans le `pg_hba.conf` de l'image : un test d'auth qui « passe » en local peut être un faux positif (cf. concept transverse : le diagnostic du port 5432, dans la rétrospective).

### Pour aller plus loin
- Doc Testcontainers (Java) + doc Spring Boot *Testcontainers support* / *Service Connections*.
- Article « Singleton Containers » sur le site Testcontainers.

---

## Concept 6 : Stratégie de gestion des versions (BOM-first + pinnage hors-écosystème)

### Définition
Une politique explicite de *qui décide des versions*. Formalisée dans **ADR-009** en trois règles : (1) écosystème Spring → **hériter du BOM** `spring-boot-dependencies` ; (2) libs hors écosystème → **pinner explicitement** ; (3) versions héritées sensibles → **figer dans `<properties>`** pour la traçabilité.

### Pourquoi c'est important
Le BOM Spring teste mutuellement des centaines de versions de libs. Lutter contre lui (pinner des versions divergentes) crée des conflits subtils. Mais tout laisser implicite rend le `pom.xml` opaque : on ne sait pas quelle version tourne, et un upgrade mineur de Boot peut changer une version sans trace. La stratégie hybride combine cohésion (BOM) et traçabilité (figeage).

### Comment c'est utilisé dans Atlas
- **Hérité du BOM** (pas de version) : `spring-boot-starter-*`, Hibernate, JUnit, AssertJ, Mockito.
- **Pinné explicitement** (hors écosystème) : Spring Modulith (via son BOM, 2.1.0), springdoc-openapi (3.0.3 — la ligne pour Spring Framework 7), MapStruct (1.6.3).
- **Figé dans `<properties>`** : `flyway.version`, `postgresql.version`, `testcontainers.version` — re-déclarées à la valeur exacte du BOM (Spring Boot lit ces propriétés), pour qu'un `git diff` montre tout changement de version à un upgrade.
- Cas vécu : la doc initiale pinnait springdoc 2.7.x (Boot 3 uniquement), Flyway 11.x, Testcontainers 1.20.x — tous incompatibles avec Boot 4.1. Vérification sur Maven Central → corrections (ADR-002 révisé).

### Exemple minimal hors Atlas
```xml
<properties>
  <!-- hérité du BOM Boot, figé ici pour traçabilité -->
  <flyway.version>12.4.0</flyway.version>
</properties>
```
Tant que `12.4.0` == la version du BOM, l'effet runtime est **nul**. C'est purement de la documentation : au prochain upgrade de Boot, cette ligne devient un point de revue conscient.

### Pièges classiques
- **Pinner une version « parce que la doc le dit »** sans vérifier la compat réelle avec le BOM courant. Toujours vérifier sur Maven Central / les release notes.
- **Tout figer** : la règle 3 ne concerne que les libs *structurellement importantes* (base, migrations, tests), pas les centaines de libs du BOM.

### Pour aller plus loin
- Lire le `pom` de `spring-boot-dependencies-<version>.pom` sur Maven Central : c'est la source de vérité de ce qui est géré.
- ADR-009 du projet.

---

## Concept 7 : Maven wrapper

### Définition
Le **Maven wrapper** (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) est un petit script versionné dans le dépôt qui **télécharge et utilise une version précise de Maven**, sans dépendre d'un Maven installé sur la machine.

### Pourquoi c'est important
Reproductibilité : tout le monde (devs, CI) build avec **exactement** la même version de Maven, pinnée dans le dépôt. Onboarding zéro friction : un `git clone` puis `./mvnw verify` suffit, pas besoin d'installer Maven. C'est le standard des projets Spring Boot sérieux en 2026 (généré par défaut par Spring Initializr).

### Comment c'est utilisé dans Atlas
- Généré pendant le refactor monorepo (`mvn -N wrapper:wrapper -Dmaven=3.9.16`), il vit dans `backend/`.
- `maven-wrapper.properties` contient l'URL de distribution : `…/apache-maven-3.9.16-bin.zip`. Au premier `./mvnw`, le wrapper télécharge cette version dans `~/.m2/wrapper` et l'utilise.
- Toutes les commandes backend passent par `./mvnw` (scripts, CI). La CI GitHub Actions n'a donc **pas besoin de `setup-maven`** : elle utilise le wrapper.

### Exemple minimal hors Atlas
```bash
git clone <repo> && cd repo/backend
./mvnw clean verify     # télécharge Maven 3.9.16 si absent, puis build — aucune install préalable
```

### Pièges classiques
- **Ne pas committer le wrapper** : `mvnw`, `mvnw.cmd` et `.mvn/wrapper/maven-wrapper.properties` DOIVENT être versionnés (notre `backend/.gitignore` y veille). Sans eux, plus de reproductibilité.
- **Confondre la version de Maven (wrapper) et la version de Java (`JAVA_HOME`)** : le wrapper pinne Maven, pas le JDK. Le JDK reste fourni par l'environnement (`JAVA_HOME` → Temurin 25).

### Pour aller plus loin
- Doc *Apache Maven Wrapper*.
- Regarder ce que Spring Initializr génère (start.spring.io) : le wrapper y est par défaut.

---

## Concept 8 : Features Java 25 utiles pour le projet

### Définition
Java 25 (LTS, sept. 2025) consolide une série de features modernes. Quatre nous serviront directement : **records**, **sealed classes**, **pattern matching**, **virtual threads**.

### Pourquoi c'est important
Ces features réduisent drastiquement la verbosité du Java « à l'ancienne » et rendent le code de domaine (DDD) plus expressif et plus sûr. Un value object immuable en 1 ligne, un type fermé qui force l'exhaustivité, un `switch` qui déstructure — c'est exactement ce dont un domaine riche a besoin.

### Comment c'est utilisé dans Atlas (à venir, dès le sprint 1+)
- **Records** pour les value objects et events immuables : `record Weight(BigDecimal value, Unit unit) {}`. Compact, `equals`/`hashCode`/`toString` gratuits, validable dans le constructeur compact.
- **Sealed classes/interfaces** pour les types fermés : `sealed interface NutritionPhase permits Bulk, Cut, Maintenance, Recomp`. Le compilateur connaît toutes les variantes → `switch` exhaustif sans `default`.
- **Pattern matching** (switch patterns, record patterns) pour traiter ces types proprement : `switch (phase) { case Bulk b -> …; case Cut c -> …; }`.
- **Virtual threads** (stables depuis 21) : threads légers gérés par la JVM, idéaux pour des charges I/O massivement concurrentes. Atlas est I/O-bound (DB, web) — pertinent pour la scalabilité future sans réécrire en réactif.

### Exemple minimal hors Atlas
```java
sealed interface Shape permits Circle, Square {}
record Circle(double radius) implements Shape {}
record Square(double side) implements Shape {}

double area(Shape s) {
    return switch (s) {                 // exhaustif : pas de default nécessaire
        case Circle(double r) -> Math.PI * r * r;   // record pattern : déstructure r
        case Square(double a) -> a * a;
    };
}
```
Ajouter une variante `Triangle` à `permits` → le `switch` ne compile plus tant qu'on ne l'a pas traitée. Le compilateur devient un filet de sécurité.

### Pièges classiques
- **Mettre de la logique lourde ou de la mutabilité dans un record** : un record est fait pour des données immuables. Pas de setters, pas d'état caché.
- **Croire que les virtual threads accélèrent le CPU-bound** : ils brillent sur l'I/O concurrent, pas sur le calcul pur.
- **Sealed + modules** : toutes les permitted classes doivent être dans le même module/package (selon le mode), à anticiper côté structure.

### Pour aller plus loin
- JEPs : Records (395), Sealed Classes (409), Pattern Matching for switch (441), Virtual Threads (444).
- *Java in a Nutshell* (édition récente) ou la doc OpenJDK pour Java 21→25.

---

## Concept 9 : Angular 22 zoneless + signals (la rupture vs Angular 14-16)

### Définition
**Zoneless** = Angular sans `zone.js`. Historiquement, Angular détectait les changements en interceptant *tout* (timers, events, XHR) via zone.js, puis re-vérifiait l'arbre de composants. En **zoneless**, la détection de changement est pilotée explicitement par les **signals** : un signal qui change notifie Angular de re-render, de façon ciblée.

### Pourquoi c'est important
C'est une rupture de mental model par rapport à Angular 14-16. Moins de magie, plus de précision : on sait *pourquoi* et *quand* un composant se met à jour. Performance améliorée (pas de re-vérification globale), bundles plus légers (plus de zone.js), et un modèle réactif explicite proche de ce que font Solid/Svelte.

### Comment c'est utilisé dans Atlas
- App créée avec `ng new --zoneless` → **zéro `zone.js`** dans le projet.
- `app.config.ts` ajoute explicitement `provideZonelessChangeDetection()` (non ambigu).
- La page Hello world appelle `/actuator/health` via `HttpClient` et stocke le résultat dans un **signal** :
```ts
protected readonly apiStatus = signal<ApiStatus>('loading');
ngOnInit() {
  this.http.get<{status: string}>(HEALTH_URL).subscribe({
    next: r => this.apiStatus.set(r.status === 'UP' ? 'UP' : 'DOWN'),
    error: () => this.apiStatus.set('unreachable'),
  });
}
```
Le template lit `apiStatus()` dans un bloc `@switch` ; quand le signal change, Angular re-render ce qu'il faut, sans zone.js. Tailwind v4 (`@import 'tailwindcss'`) pour le style.

### Exemple minimal hors Atlas
```ts
const count = signal(0);
// dans le template : {{ count() }}
increment() { count.update(n => n + 1); }   // le set/update notifie Angular → re-render ciblé
```
Pas de `ngZone`, pas de `ChangeDetectorRef.detectChanges()` : le signal *est* le déclencheur.

### Pièges classiques
- **Attendre un re-render après une mutation hors signal** : en zoneless, muter un champ « normal » ne déclenche rien. L'état réactif doit passer par des signals.
- **Oublier `provideZonelessChangeDetection()`** ou laisser traîner du code qui suppose zone.js (certaines vieilles libs).
- **Tests** : un composant zoneless avec `HttpClient` nécessite `provideZonelessChangeDetection()` + `provideHttpClientTesting()` dans le `TestBed` (sinon erreurs d'injection).

### Pour aller plus loin
- Doc Angular : *Signals*, *Zoneless*, *Control flow (@if/@for/@switch)*.
- Comparer avec le mental model de SolidJS/Svelte pour ancrer la notion de réactivité fine.

---

## Concept 10 : Monorepo symétrique vs structures alternatives

### Définition
Un **monorepo** héberge plusieurs projets dans un seul dépôt git. La variante **symétrique** met chaque application dans son propre sous-dossier de même niveau (`backend/`, `frontend/`), la racine ne gardant que le transverse (docs, scripts, orchestration). Formalisé dans **ADR-010**.

### Pourquoi c'est important
La structure de dépôt conditionne la CI, le déploiement et la lisibilité pour des années. Une structure asymétrique (Maven à la racine, Angular dans un sous-dossier) mélange les conventions et complique tout outillage qui doit raisonner « où est quoi ». La symétrie rend le projet immédiatement compréhensible et l'outillage trivial.

### Comment c'est utilisé dans Atlas
```
atlas/
├── backend/    # Spring Boot (racine Maven + wrapper)
├── frontend/   # Angular 22
├── docs/ scripts/ docker-compose.yml   # transverse
```
- `.gitignore` par niveau (racine léger + un par sous-projet).
- Les scripts lancent `./mvnw` depuis `backend/` et `docker compose` depuis la racine.
- La CI exploite directement la symétrie : 2 jobs avec `working-directory` distincts et caches indépendants (concept 11).

### Alternatives écartées
- **Repos séparés** : découplage maximal mais double CI, synchro des versions, double cérémonie — surdimensionné pour un projet solo.
- **Statu quo asymétrique** : zéro travail mais conserve la confusion des conventions.
- **Monorepo symétrique (choisi)** : lisible, outillage simple, et migration future vers des repos séparés possible sans réorganisation interne.

### Exemple minimal hors Atlas
Un produit web classique : `repo/{api, web, docs}` avec `api/` (backend), `web/` (frontend), la racine pour la CI et le `docker-compose`. N'importe quel nouveau venu sait en 2 secondes où regarder.

### Pièges classiques
- **Laisser un artefact de build à la racine** après un déplacement (on a dû nettoyer un `target/` résiduel).
- **Oublier d'ajuster les chemins** dans scripts/CI/docs après le move.
- **Mélanger refactor structurel et changement fonctionnel dans un même commit** : on a strictement séparé `refactor:` (déplacement pur, 43 renames `R100`) et `feat:` (la feature). Cf. concept transverse ci-dessous.

### Pour aller plus loin
- ADR-010 du projet.
- Lectures sur « monorepo vs polyrepo » (trade-offs outillage : Nx, Turborepo, Bazel pour les gros monorepos — non nécessaires ici, mais la structure les permettrait).

---

## Concept 11 : CI vs CD, et GitHub Actions multi-job avec caches indépendants

### Définition
- **CI (Continuous Integration)** : à chaque push/PR, on **build et teste** automatiquement pour détecter les régressions tôt. C'est ce qu'on a fait au sprint 0.
- **CD (Continuous Delivery/Deployment)** : on **livre/déploie** automatiquement un build validé. Pas encore au sprint 0 (déploiement Dokploy manuel pour l'instant).
- **GitHub Actions** : la plateforme d'automatisation de GitHub ; un *workflow* (YAML) décrit des *jobs* (qui tournent sur des *runners*), composés de *steps*.

### Pourquoi c'est important
La distinction CI/CD est conceptuelle mais structurante : on automatise d'abord la **confiance** (CI : « le code compile et passe les tests »), avant d'automatiser la **livraison** (CD). Mélanger les deux trop tôt, c'est risquer de déployer du cassé. Sur un monorepo, savoir paralléliser les jobs et isoler les caches accélère le feedback.

### Comment c'est utilisé dans Atlas
Workflow `.github/workflows/ci.yml`, déclenché `on: push` et `on: pull_request` :
- **Job `backend`** : `working-directory: backend`, `setup-java` (Temurin 25 + cache Maven), `./mvnw clean verify` (compile + Modulith verify + Testcontainers). Les runners ubuntu ayant Docker, **pas besoin de service `postgres`** : Testcontainers gère son propre container.
- **Job `frontend`** : `working-directory: frontend`, `setup-node` (Node 24 + cache npm), `npm ci` + `npm run build` + `npm test`.
- Les deux jobs sont **indépendants → parallèles** (bénéfice direct du monorepo symétrique), avec **caches indépendants** (`~/.m2` vs `frontend/node_modules`).
- Un job `ci-success` (`needs: [backend, frontend]`) agrège les deux : point unique pour la future *branch protection* sur `main`.
- Résultat réel : run #1 vert en 1m00 (backend 48s, frontend 23s, en parallèle).

### Exemple minimal hors Atlas
```yaml
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '24', cache: npm }
      - run: npm ci
      - run: npm test
```
À chaque push, GitHub démarre un runner propre, installe Node (avec cache), et lance les tests. Rouge = on a cassé quelque chose ; vert = on peut merger.

### Pièges classiques
- **Ajouter un `services: postgres`** alors qu'on utilise Testcontainers : double base, configuration inutile. Testcontainers se suffit à lui-même sur un runner avec Docker.
- **Cache mal keyé** : un `cache-dependency-path` qui ne pointe pas le bon lockfile → cache jamais réutilisé (lent) ou périmé.
- **Confondre CI et CD** : croire qu'« avoir une CI » signifie « déployer automatiquement ». Ce sont deux étapes distinctes.
- **Pas de point d'agrégation** pour la branch protection : sans job `ci-success`, protéger `main` oblige à lister chaque job à la main.

### Pour aller plus loin
- Doc GitHub Actions (workflow syntax, caching, `needs`, `concurrency`).
- `actionlint` (lint local des workflows) — utilisé dans ce sprint pour valider le YAML avant push.

---

## Auto-évaluation

Réponds sans relire le document. Si tu sèches, c'est exactement le concept à retravailler.

1. Quelle est la différence entre un modular monolith et un monolithe layered classique ? Qu'est-ce que Spring Modulith **ajoute** concrètement ?
2. Pourquoi un `verify()` Modulith qui passe sur des modules vides ne prouve-t-il (presque) rien ?
3. Quels imports sont **interdits** dans un package `domain/` selon ADR-003, et pourquoi cette contrainte accélère-t-elle les tests ?
4. Explique la différence entre `<parent>`, `<dependencyManagement>` et `<dependencies>`. Pourquoi peut-on importer plusieurs BOMs mais n'avoir qu'un seul parent ?
5. En Spring Boot 4, pourquoi `flyway-core` seul ne déclenche-t-il plus la migration au démarrage ? Comment l'aurais-tu diagnostiqué from scratch ?
6. Quelle est la différence entre `@ServiceConnection` et `@DynamicPropertySource` ? Et entre « isolation par truncate » et « isolation par recreate » ?
7. Dans la stratégie de versions (ADR-009), quand hérite-t-on du BOM, quand pinne-t-on, et pourquoi figer dans `<properties>` une version pourtant déjà fournie par le BOM ?
8. Que pinne le Maven wrapper, et que ne pinne-t-il PAS ? Quels fichiers doivent être committés ?
9. Donne un value object Atlas plausible en `record` Java 25, avec validation. Quand utiliserais-tu une `sealed interface` plutôt qu'une enum ?
10. En Angular zoneless, qu'est-ce qui déclenche un re-render ? Que se passe-t-il si tu mutes un champ « normal » au lieu d'un signal ?
11. Cite trois alternatives à la structure monorepo symétrique et un inconvénient de chacune.
12. Distingue CI et CD. Pourquoi nos deux jobs CI tournent-ils en parallèle, et à quoi sert le job `ci-success` ?
13. (Bonus méthode) On a passé beaucoup de temps sur un « port 5432 squatté ». Quelle est la **leçon de méthode** de ce diagnostic, applicable à n'importe quel bug d'intégration ?

---

*Mini-cours du Sprint 0 — maintenu par Ryan Foerster. Publiable sur le devblog après adaptation.*
