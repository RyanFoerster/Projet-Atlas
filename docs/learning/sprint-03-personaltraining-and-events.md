# Sprint 03 — PersonalTraining & communication event-driven Modulith

> Premier sprint où **deux modules se parlent vraiment**. Le sprint 1 a posé le DDD, le sprint 2 a prouvé
> que le template se réutilise. Le sprint 3 répond à : *est-ce que le modular monolith tient quand un module
> doit déclencher un effet dans un autre ?* Réponse — oui, à condition de comprendre les deux moitiés de
> Modulith (events pour les side-effects, ports pour les queries) et l'outillage qui rend l'isolation réelle.

## Ce qu'on a appris

Logger une séance IRL (PersonalTraining) doit faire « vivre » l'athlète miroir (Roster). Le naïf serait un
appel direct de méthode d'un module à l'autre. Le bon réflexe DDD/Modulith : **un event**. Mais un event en
production soulève des questions que les tutoriels esquivent — durabilité, idempotence, isolation,
sérialisation. Cinq concepts (+ deux bonus) pour les traiter sérieusement.

---

## Concept 1 : Event-driven Modulith + transactional outbox

### Définition
Un module **publie** un event applicatif ; un autre le **consomme** via un listener, sans dépendance
directe. Avec l'**event publication registry** de Spring Modulith, l'event est d'abord **persisté** (table
`event_publication`) dans la transaction du producteur, puis marqué « complété » quand le listener réussit.
C'est le pattern **transactional outbox**.

### Pourquoi c'est important
Sans persistance de l'event, un crash entre « la séance est commitée » et « le listener s'exécute » perd le
side-effect : la séance existe, mais le miroir ne la verra jamais. L'outbox garantit que **l'event est aussi
durable que la donnée métier** : soit les deux sont commités ensemble, soit aucun.

### Comment c'est utilisé dans Atlas
`LogWorkoutUseCase` publie `WorkoutLogged` **à l'intérieur** de sa méthode `@Transactional` :
```java
@Transactional
public WorkoutSession logWorkout(UserId owner, LogWorkoutCommand cmd) {
    WorkoutSession saved = repository.save(WorkoutSession.log(...));
    eventPublisher.publishEvent(WorkoutSessionToEventMapper.toEvent(saved)); // DANS la tx
    return saved;
}
```
Le consumer Roster est un `@ApplicationModuleListener` (= `@TransactionalEventListener(AFTER_COMMIT)` +
`@Async` + `@Transactional(REQUIRES_NEW)`) → **cohérence éventuelle** assumée (le miroir est mis à jour juste
après le commit, en asynchrone). La config Atlas : `republish-outstanding-events-on-restart=true`.

**Comportement réel prouvé au désassemblage** (pas supposé) : on a lu les requêtes JPQL de
`JpaEventPublicationRepository` via `javap -c -p`. Le republish au démarrage utilise
`where completion_date is null` → Modulith ne rejoue **que** les publications incomplètes, jamais une
publication complétée. La livraison est **at-least-once**, pas exactly-once.

### Exemple minimal hors Atlas
Une commande e-commerce : `OrderPlaced` doit déclencher l'envoi d'un email. Publier l'event dans la
transaction qui sauve la commande (outbox) garantit qu'on n'envoie jamais d'email pour une commande qui n'a
pas commité, et qu'on n'« oublie » jamais l'email d'une commande commitée (re-livré au restart).

### Pièges classiques
- **Publier hors transaction** (après le commit) → fenêtre de perte de l'event. Toujours publier *dans* la tx.
- **Croire que c'est exactly-once** : c'est at-least-once. Le consumer **doit** être idempotent (concept 5 / bonus).
- Oublier la table `event_publication` (migration Flyway + l'entité validée au boot).

### Pour aller plus loin
Doc Spring Modulith « Working with Application Events » ; pattern *Transactional Outbox* (Chris Richardson,
microservices.io).

---

## Concept 2 : Snapshot DTO pour les events publics (isolation)

### Définition
Un event public ne transporte **pas** les value objects riches du domaine, mais une forme **aplatie** de
types primitifs / partagés (un « snapshot »).

### Pourquoi c'est important
Mettre `LoggedExercise` (ou le sealed `ExerciseCategory`, ou l'enum interne `BodyRegion`) dans l'event, c'est
exposer le **domaine interne** de PersonalTraining à tous ses consumers → fuite d'isolation. Le jour où tu
refactors `LoggedExercise`, tu casses Roster. Le snapshot **découple** le contrat public du modèle interne.

### Comment c'est utilisé dans Atlas
`WorkoutLogged` porte des `UUID` (pas les VO d'id), `MovementPattern` (kernel `shared`, donc OK), et
`LoggedExerciseSnapshot` où le sealed est aplati :
```java
record LoggedExerciseSnapshot(String name, String categoryType /* "COMPOUND_FORCE"|"ACCESSORY" */,
                              MovementPattern pattern /* nullable */, String accessoryRegion /* nullable */,
                              List<ExerciseSetSnapshot> sets) {}
```
`BodyRegion` est passé en **`String`** (son `.name()`) — surtout pas le type, qui est interne. Le mapper
domaine → snapshot (`WorkoutSessionToEventMapper`) vit dans `application/`.

### Exemple minimal hors Atlas
`OrderPlaced` ne transporte pas l'aggregate `Order` complet (avec ses méthodes, ses VO de pricing), mais un
`OrderPlacedSnapshot(orderId, customerId, totalCents, List<LineSnapshot>)`. Le consumer a tout, sans dépendre
du modèle de pricing interne.

### Pièges classiques
- Exposer un type du domaine « parce que c'est pratique » → couplage caché, refactor bloqué.
- Oublier que `shared` (OPEN) est exposable partout, mais qu'un type d'un *autre* module ne l'est pas.

### Pour aller plus loin
« Published Language » (DDD, Evans) ; la notion de *contract* vs *internal model*.

---

## Concept 3 : Aggregate autonome vs aggregate à collection

### Définition
Deux formes d'aggregate : l'**autonome** (`WorkoutSession` — une séance = un aggregate, persisté/chargé seul)
et celui **à collection d'entities** (`Roster` — un aggregate qui contient des `Athlete`).

### Pourquoi c'est important
Le choix change tout : persistance, chargement, invariants, scalabilité. Se tromper crée soit un aggregate
géant (tout dans un seul), soit une cohérence éclatée (des racines qui se marchent dessus).

### Comment c'est utilisé dans Atlas
- `WorkoutSession` est **autonome** : des milliers de séances indépendantes, chacune avec son `WorkoutSessionId`
  et son repository. On ne charge jamais « toutes les séances » comme un bloc. Pattern standard pour des
  entités à volume élevé et faiblement couplées entre elles.
- `Roster` est un aggregate **à collection** : il *possède* ses `Athlete` (entities internes, pas de repository
  propre), protège l'invariant « un seul miroir », se charge entier. Pattern pour un petit ensemble cohérent
  avec des invariants transverses.

Règle de décision : **collection** si les éléments partagent un invariant et sont peu nombreux ; **autonome**
si chaque élément a sa propre identité/cycle de vie et qu'ils sont nombreux.

### Exemple minimal hors Atlas
`Order` + `OrderLine` = aggregate à collection (invariant « total = somme des lignes »). `Invoice` = aggregate
autonome (des milliers, chacune indépendante). On ne met pas toutes les factures dans un aggregate `Ledger`.

### Pièges classiques
- Faire un `TrainingLog` géant contenant toutes les séances → ne scale pas, contention.
- Donner un repository à une entity interne « pour aller vite » → on perd la cohérence de l'aggregate.

### Pour aller plus loin
Vaughn Vernon, *Implementing DDD*, « Designing Aggregates » (règles : petit aggregate, référencer par id).

---

## Concept 4 : Tester l'event-driven (Scenario, chemin d'échec, completion_date)

### Définition
Tester qu'un event publié par A est bien consommé par B — y compris le **chemin d'échec** — malgré
l'asynchronisme.

### Pourquoi c'est important
Une assertion synchrone naïve juste après la publication échoue (le listener async n'a pas encore tourné). Et
la plupart des tests s'arrêtent au *happy path* : ils ne prouvent jamais ce qui se passe quand le consumer
échoue — précisément ce qui justifie l'outbox.

### Comment c'est utilisé dans Atlas
Trois tests, du plus important au plus subtil :
1. **End-to-end (`Scenario` API de Modulith)** : `scenario.stimulate(() -> logWorkout(...))
   .andWaitForStateChange(() -> mirrorLastWorkoutAt(owner))` — l'API *attend* la consommation asynchrone.
2. **Complétion** : après succès, on requête `event_publication` → `completion_date IS NOT NULL` (preuve
   empirique que la publication sort du republish, cohérent avec le désassemblage).
3. **Négatif** : on mocke `RosterRepository.save` pour qu'il lève → la publication reste **incomplète**
   (`completion_date IS NULL`) ET la séance reste loggée. La durabilité outbox est *prouvée*.

### Exemple minimal hors Atlas
Tester `OrderPlaced → email envoyé` : publier, attendre (Awaitility / Scenario), vérifier l'email. Puis forcer
l'échec de l'envoi et vérifier que l'event est re-tentable (pas perdu).

### Pièges classiques
- `Thread.sleep` arbitraire au lieu d'attendre une condition (flaky). Utiliser `Scenario` / Awaitility.
- Ne tester que le happy path → on ne sait pas ce qui se passe en prod quand ça casse.
- **État partagé entre classes de test** (Testcontainers singleton) : un `deleteAll()` sur une table partagée
  casse les FK d'autres tests. Règle tenue : scoper les données par owner/id unique.

### Pour aller plus loin
Doc Modulith « Testing » (Scenario API) ; Awaitility pour l'asynchrone.

---

## Concept 5 : `ExerciseCategory` sealed — un ADT comme pivot métier

### Définition
Un **sealed interface** avec des `record` imbriqués modélise « un type qui est soit A soit B, jamais autre
chose » — un *Algebraic Data Type* (ADT) en Java. Le compilateur connaît l'ensemble fermé des variantes.

### Pourquoi c'est important
La tentation était de réutiliser `MovementPattern` (les axes de force génétique de Roster) pour catégoriser
un exercice loggé. Mais un curl n'est **pas** un axe de force génétique. Mélanger les deux concepts aurait
pollué Roster. Le sealed **sépare proprement** « concept Genetics » et « concept exercice loggé ».

### Comment c'est utilisé dans Atlas
```java
sealed interface ExerciseCategory {
    record CompoundForce(MovementPattern pattern) implements ExerciseCategory {} // squat, bench…
    record Accessory(BodyRegion region)         implements ExerciseCategory {} // curl, gainage…
}
```
Records imbriqués → clause `permits` **inférée**. Le `switch` exhaustif (sans `default`) garantit la
couverture : ajouter une variante (`Cardio`) ferait **échouer la compilation** de tout traitement non mis à
jour — la dette devient une erreur de compile. C'est exactement ce que consommera Athletics au sprint 4 :
`CompoundForce` → stimulus sur le pattern, `Accessory` → stimulus sur la région.

### Exemple minimal hors Atlas
```java
sealed interface PaymentMethod {
    record Card(String last4) implements PaymentMethod {}
    record Cash() implements PaymentMethod {}
    record Voucher(String code) implements PaymentMethod {}
}
double fee(PaymentMethod m) { return switch (m) {
    case Card c -> 0.015; case Cash ignored -> 0; case Voucher v -> 0; }; } // exhaustif, pas de default
```

### Pièges classiques
- Utiliser un `enum` quand les variantes portent **des données différentes** (impossible — `CompoundForce` a un
  `MovementPattern`, `Accessory` une `BodyRegion`). L'ADT (sealed) est l'outil.
- Mettre un `default` dans le switch → on perd la sûreté (l'ajout d'une variante passe silencieusement).

### Pour aller plus loin
JEP 409 (Sealed Classes) ; JEP 441 (Pattern Matching for switch). « Make illegal states unrepresentable ».

---

## Bonus A : `@NamedInterface` — `api/` n'est public que si on le déclare

Première vraie consommation cross-module via `api/` du projet (sprints 1-2, les modules ne se parlaient pas).
Surprise au premier `verify` : *« roster depends on non-exposed type `…api.events.WorkoutLogged` »*. **Pas un
cycle** — un défaut d'**exposition**. Par défaut, Spring Modulith n'expose que le **package racine** d'un
module ; un sous-package (`api`, `api.events`) reste interne. Il faut le déclarer :
```java
@org.springframework.modulith.NamedInterface("api")
package dev.ryanfoerster.atlas.personaltraining.api; // idem api.events, même nom → fusionnés
```
C'est la matérialisation de la règle CLAUDE.md « les modules externes n'importent que de `api/` ». L'isolation
est désormais **outillée** (vérifiée par `AtlasApplicationModulesTest` + une règle ArchUnit dédiée), plus une
intention.

## Bonus B : Option D — supprimer le problème d'idempotence plutôt que le mitiger

Le consumer met à jour le miroir. Avec une livraison at-least-once, un `workoutCount += 1` naïf double sur
rejeu. Deux familles de réponses : **mitiger** (garde par id d'event, ou table inbox) ou **supprimer**. Choix
retenu (option D) : **ne pas dupliquer le compteur**. Sa source de vérité est PersonalTraining (le nombre de
lignes `workout_sessions`), exposé via `PersonalTrainingQueryPort` et composé côté backend dans la fiche
miroir. Côté Roster, on ne garde que la *dernière* séance, mise à jour par **écrasement monotone** (on n'écrit
que si `performedAt` est strictement plus récent) → idempotent **et** robuste au désordre, sans mémoriser
d'id. Leçon : **events pour les side-effects, ports synchrones pour les queries** — les deux moitiés de
Modulith, chacune à sa place. Un compteur qu'on ne duplique pas ne peut pas diverger.

---

## Auto-évaluation

1. Pourquoi publier l'event **dans** la transaction `@Transactional` du use case, et pas après le commit ?
2. La livraison Modulith est-elle exactly-once ou at-least-once ? Qu'est-ce que le republish rejoue, exactement ?
3. Pourquoi `WorkoutLogged` transporte-t-il `BodyRegion` en `String` mais `MovementPattern` en type ?
4. `WorkoutSession` (autonome) vs `Roster` (à collection) : donne le critère de décision en une phrase.
5. Que prouve le **test négatif** que le test end-to-end ne prouve pas ?
6. Pourquoi un `enum` ne pouvait pas remplacer le sealed `ExerciseCategory` ?
7. Qu'est-ce que `@NamedInterface` règle, et pourquoi le problème n'était-il jamais apparu avant le sprint 3 ?
8. En quoi l'option D « supprime » le problème d'idempotence au lieu de le mitiger ?
