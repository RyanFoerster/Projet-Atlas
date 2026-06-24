# Sprint 3 — PersonalTraining (les vraies séances IRL)

> Prompt complet à coller dans Claude Code pour exécuter le Sprint 3. Premier sprint event-driven, premier test pratique de la communication inter-modules Modulith.

---

## Contexte projet (lecture préalable obligatoire)

Avant d'exécuter ce sprint, relis dans cet ordre :

1. `CLAUDE.md` à la racine — contexte général, conventions, mode de collaboration
2. `docs/vision.md` — le concept "athlète miroir" est central pour ce sprint
3. `docs/domain/glossary.md` — vocabulaire métier officiel, notamment WorkoutSession, TrainingStimulus, MirrorAthlete
4. Tous les ADRs dans `docs/adr/` (001 à 022) — au moins parcourir l'index, en lecture attentive sur ADR-005 (mirror athlete + program unlock), ADR-007 (CQRS Insights, contexte event-driven), ADR-009 (postgresql + flyway + testcontainers), ADR-015 (mappers manuels), ADR-019 (aggregate roster + JSONB)
5. `docs/learning/sprint-01-identity-and-ddd.md` ET `docs/learning/sprint-02-roster-and-procgen.md` — les patterns posés à réutiliser
6. `docs/sprints/sprint-02-roster/RETROSPECTIVE.md` — leçons concrètes du sprint 2
7. `docs/design-system/atlas-design-system.md` et `preview.html` — pour toute production frontend
8. Le code des modules Identity ET Roster — **les templates à copier** (notamment l'aggregate Roster qui sert maintenant de référence aux côtés d'Identity)
9. **Documentation Spring Modulith sur les Application Events** : https://docs.spring.io/spring-modulith/reference/events.html — concept central pour ce sprint, à parcourir au moins une fois si pas encore familier

Si une de ces lectures est obsolète ou que tu identifies une contradiction avec ce prompt, **arrête et signale à Ryan** avant de coder.

---

## Objectif du Sprint 3

À la fin de ce sprint, un Player peut :

1. **Logger une séance d'entraînement** via un tableau dynamique style Football Manager
2. Pour chaque séance : date, durée, exercices catégorisés par MovementPattern (squat, bench, deadlift, OHP, row, pull, accessory) avec sets/reps/poids/RPE
3. Voir la **liste de ses séances** chronologique
4. Consulter le **détail d'une séance** loggée
5. Voir sur la **fiche de son athlète miroir** un compteur "X séances loggées" et la date de la dernière

Et techniquement, le projet aura :

- Un **module PersonalTraining** complet avec aggregate WorkoutSession
- Une **communication event-driven Modulith** prouvée : événement `WorkoutLogged` publié par PersonalTraining, consommé par Roster
- L'**event publication registry** Modulith activé (garantie at-least-once)
- Un **TrainingHistory** sur l'aggregate Roster (compteur séances + lastWorkoutAt sur le miroir)
- La **catégorisation par MovementPattern** réutilisée du kernel shared (sprint 2)
- Un **mini-cours sprint-03** focalisé sur event-driven et intégration inter-module
- Un **devblog post #4** "Premier event Modulith en production" — sujet rare et précieux en entretien

---

## Décisions prises en amont (à respecter, ne pas re-débattre)

| Décision | Choix retenu | Justification courte |
|----------|--------------|---------------------|
| Périmètre | B — Sprint complet avec event vers Roster | Pose le pattern event-driven proprement sans déborder |
| Modèle séance | C — Hybride catégorisé par `ExerciseCategory` (composé→MovementPattern, accessoire→BodyRegion) | Anti-dette : permet calcul TrainingStimulus au sprint 4 sans rétro-fit ; sépare force génétique et catégorie d'exercice (ADR-026, tension #1 option B) |
| UX logger | B — Tableau dynamique style Football Manager | Cohérent avec l'audience cible (lifters sérieux veulent saisir vite) |
| Qui logge | C — Joueur uniquement, impact auto sur miroir | Cohérent avec concept "miroir = projection du joueur IRL". Séances athlètes virtuels = sprint 6+ (Programming) |
| Aggregate central | A — WorkoutSession (une séance = un aggregate) | Plus scalable que TrainingLog géant. Pattern standard pour entités autonomes. |
| Event WorkoutLogged | B — Autosuffisant (userId, sessionId, date, exercises complets) | Pattern Modulith standard : consumer ne doit pas re-query |

---

## Modélisation métier

### L'aggregate WorkoutSession (root)

```
WorkoutSession (aggregate root)
├── id: WorkoutSessionId (UUID v7)
├── ownerId: UserId
├── performedAt: Instant (date/heure de la séance)
├── durationMinutes: Integer (optionnel — null si non renseigné)
├── exercises: List<LoggedExercise> (immutable, copie défensive)
├── notes: Optional<String> (max 500 chars, optionnel)
├── createdAt: Instant
└── Behaviors:
    + static log(ownerId, performedAt, exercises, durationMinutes, notes, now): WorkoutSession
    + totalSets(): int (computed)
    + totalReps(): int (computed)
    + estimatedVolume(): double (Σ sets × reps × poids, computed)
    + patternsCovered(): Set<MovementPattern> (computed)
```

**Invariants** :
- Au moins 1 exercice (pas de séance vide)
- `performedAt` ne peut pas être dans le futur
- Tous les exercices sont valides individuellement (validation au constructeur)
- WorkoutSession immutable (style fonctionnel comme les aggregates précédents)

### Le value object LoggedExercise (entity-like value object)

```
LoggedExercise {
  name: ExerciseName (VO 2-80 caractères, libre)
  category: ExerciseCategory (sealed — voir ci-dessous, remplace le MovementPattern direct)
  sets: List<ExerciseSet> (immutable, au moins 1)
}
```

### Le sealed interface ExerciseCategory (tension #1 tranchée — option B, ADR-026)

`MovementPattern` (shared) reste **pur** : 6 axes de force génétique uniquement, zéro pollution Roster.
La catégorisation d'un exercice loggé est un **concept distinct**, propre à PersonalTraining :

```java
sealed interface ExerciseCategory permits CompoundForce, Accessory {
    record CompoundForce(MovementPattern pattern) implements ExerciseCategory {}  // squat, bench, DL, OHP, row, chin-up
    record Accessory(BodyRegion region) implements ExerciseCategory {}            // curl, extension, élévations, gainage…
}

// BodyRegion : nouvel enum simple dans personaltraining.domain (PAS shared au sprint 3 —
// anti-dette ADR-017 : on le promeut à shared au sprint 4 SI Athletics en a besoin).
// Valeurs : BICEPS, TRICEPS, SHOULDERS, CHEST, BACK, FOREARMS, CORE, GLUTES, HAMSTRINGS, QUADS, CALVES
enum BodyRegion { ... }
```

**Conséquences** :
- Au sprint 4, Athletics consomme l'event et fait le matching : `CompoundForce` → stimulus sur ce pattern, `Accessory` → stimulus sur la région musculaire.
- Extensible : ajouter `Cardio`, `Mobility`… = nouvelle variante du sealed interface, sans toucher le reste.
- `WorkoutSession.patternsCovered()` ne retourne que les `MovementPattern` des `CompoundForce` (les accessoires n'ont pas de pattern de force).

### Le value object ExerciseSet

```
ExerciseSet {
  reps: Integer (1-100)
  weightKg: Weight (shared.domain) ou null si bodyweight
  rpe: RPE (VO 1.0-10.0, optionnel)
}
```

**Le concept RPE** (Rate of Perceived Exertion) est introduit ici. C'est un VO du module ou du shared selon usage. Vu qu'on n'en a besoin que dans PersonalTraining au sprint 3 (Athletics au sprint 4 aussi probablement), **mets-le dans PersonalTraining.domain au sprint 3**, on le promouvra à shared au sprint 4 si Athletics en a besoin (critère ADR-017 : "transverse à 2+ modules ET fondamental"). Anti-dette appliquée à l'envers : ne pas sur-anticiper.

### Kernel shared — décision : NE PAS toucher `MovementPattern` (tension #1, option B)

`MovementPattern` (shared) reste **inchangé** : `SQUAT, BENCH_PRESS, DEADLIFT, OVERHEAD_PRESS, ROW, CHIN_UP` — 6 axes de force génétique, point. **Pas d'ajout d'`ACCESSORY` dans shared** : un accessoire n'est pas un axe génétique (cf. `ExerciseCategory` ci-dessus). « PULL » du prompt = `CHIN_UP` existant, simple label UI.

La catégorisation des exercices loggés (composé vs accessoire) vit dans `personaltraining.domain` via `ExerciseCategory` + `BodyRegion`. **Aucune migration sur l'enum partagé, aucune régression Roster.** (Confirmé en lecture du code : l'enum actuel a bien ces 6 valeurs, et Roster s'en sert comme axes génétiques.)

### Côté Roster — ajouter TrainingHistory

L'aggregate Roster (ou plus précisément l'entité Athlete miroir) doit recevoir le stimulus. Ajout minimal :

```
Athlete {
  ... existing fields ...
  + trainingHistory: TrainingHistory (nouveau VO, immutable)
}

TrainingHistory {
  workoutCount: int
  lastWorkoutAt: Instant (nullable)
  lastPatternsCovered: Set<MovementPattern> (la dernière séance — ce que le miroir a "fait")
}
```

**Attention scope** : pas de calcul fitness/fatigue ici, ce sera Athletics au sprint 4. Au sprint 3, on **enregistre seulement** que les séances ont eu lieu. Tu peux résumer en JavaDoc : "TrainingHistory au sprint 3 est un compteur passif. Au sprint 4, Athletics va consommer ces données pour piloter le FitnessFatigueState par MuscleGroup."

### Communication inter-modules (event-driven)

**PersonalTraining publie** :
```java
package dev.ryanfoerster.atlas.personaltraining.api.events;

public record WorkoutLogged(
    UserId ownerId,
    WorkoutSessionId sessionId,
    Instant performedAt,
    Integer durationMinutes,  // nullable
    List<LoggedExerciseSnapshot> exercises  // forme aplatie pour l'event
) {}
```

**Pourquoi `LoggedExerciseSnapshot` et pas `LoggedExercise` directement** :
- Les events publics doivent vivre dans `api/events/`
- Les types métier riches (LoggedExercise) vivent dans `domain/model/`
- Si on expose LoggedExercise dans l'event, on expose le domain interne dans l'API → violation de l'isolation Modulith
- Pattern propre : un snapshot aplati DTO-like dans `api/events/`, le mapper PersonalTraining → snapshot vit dans `application/`

```java
package dev.ryanfoerster.atlas.personaltraining.api.events;

public record LoggedExerciseSnapshot(
    String name,
    String categoryType,        // "COMPOUND_FORCE" | "ACCESSORY" — discriminant aplati du sealed ExerciseCategory
    MovementPattern pattern,    // non-null si COMPOUND_FORCE (MovementPattern vit dans shared, OK dans api/events), null sinon
    String accessoryRegion,     // non-null si ACCESSORY = BodyRegion.name() — String pour NE PAS fuiter BodyRegion (domaine interne)
    List<ExerciseSetSnapshot> sets
) {}
// Note : aplatir le sealed ExerciseCategory en discriminant + champs nullables, et représenter
// BodyRegion en String, est exactement l'illustration de pourquoi les snapshots existent (ADR-024) :
// l'event public ne dépend que de shared, jamais du domaine interne de PersonalTraining.

public record ExerciseSetSnapshot(
    int reps,
    Double weightKg,  // nullable
    Double rpe  // nullable
) {}
```

**Roster consomme** : un event handler dans `roster/application/eventhandler/WorkoutLoggedHandler.java` qui :
1. Récupère le Roster du `ownerId`
2. Trouve l'athlète miroir
3. Met à jour son `TrainingHistory` (compteur +1, lastWorkoutAt = performedAt, lastPatternsCovered = patterns de la séance)
4. Sauvegarde le Roster

### Event publication registry Modulith

**Activer le registry** est important pour ce sprint. Sans registry, si Roster lance une exception en consommant l'event, l'event est perdu (et la séance reste loggée). Avec registry, Modulith garantit la livraison at-least-once : si la consommation échoue, l'event est ré-essayé.

Configuration à activer :

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-jpa</artifactId>
</dependency>
```

```sql
-- V00X migration Flyway pour la table event_publication
-- Le script SQL est fourni par Modulith, à inclure dans la migration
```

Vérifie sur https://docs.spring.io/spring-modulith/reference/events.html la migration exacte pour PostgreSQL.

**À documenter dans ADR-023** (sémantique précisée — tension #2) : durabilité transactionnelle au commit (table `event_publication`) + republication **au démarrage**. **PAS de retry runtime automatique** — le défaut Modulith ne réessaie pas en cours d'exécution ; une publication échouée reste *incomplète et durable*, re-livrée au restart. Config Atlas par défaut à activer dans `application.yml` :

```yaml
spring:
  modulith:
    events:
      republish-outstanding-events-on-restart: true   # gratuit, protège des redémarrages app pendant un handler
```

Cohérence éventuelle assumée (tension #3 — `@ApplicationModuleListener` = async + REQUIRES_NEW + AFTER_COMMIT) : la séance commit immédiatement, le miroir est mis à jour juste après, de façon asynchrone. Acceptable pour Atlas (pas de criticité temporelle). Options futures si besoin : scheduler de resoumission des incomplets, ou `ExternalizedEvents` + broker (Kafka/RabbitMQ) si multi-instance.

---

## Réservation des numéros d'ADR (leçon rétro sprint 1)

**Réservés à l'avance pour ce sprint** : ADR-023, ADR-024, ADR-025, ADR-026.

| Numéro | Sujet | Quand le rédiger |
|--------|-------|------------------|
| ADR-023 | Event publication registry Modulith — durabilité au commit + republication au restart, **PAS de retry runtime auto** ; cohérence éventuelle assumée (async) ; config Atlas par défaut `republish-outstanding-events-on-restart=true` | Sprint 3 — GATE C |
| ADR-024 | Pattern snapshot DTO pour events publics — aplatissement du sealed `ExerciseCategory` (discriminant + champs nullables), `BodyRegion` en String pour ne pas fuiter le domaine | Sprint 3 — GATE B |
| ADR-025 | TrainingHistory minimal sprint 3, enrichissement sprint 4 (Banister) | Sprint 3 — GATE C |
| ADR-026 | **Catégorisation des exercices loggés via sealed interface `ExerciseCategory`** — séparation nette entre concept force génétique (`MovementPattern`) et concept exercice loggé (catégorie). Décision de modélisation distincte (tension #1, option B). | Sprint 3 — GATE A |

Si un autre sujet émerge, prends 027+. Si un sujet s'avère trivial, supprime-le et signale.

---

## Portée technique

### Module PersonalTraining — structure complète

```
backend/src/main/java/dev/ryanfoerster/atlas/personaltraining/
├── api/
│   ├── events/
│   │   ├── WorkoutLogged.java
│   │   ├── LoggedExerciseSnapshot.java
│   │   └── ExerciseSetSnapshot.java
│   └── PersonalTrainingQueryPort.java  # interface publique pour query depuis autres modules si besoin
├── domain/
│   ├── model/
│   │   ├── WorkoutSession.java         # aggregate root
│   │   ├── WorkoutSessionId.java
│   │   ├── LoggedExercise.java         # VO interne riche
│   │   ├── ExerciseCategory.java       # sealed interface (CompoundForce | Accessory) — ADR-026
│   │   ├── BodyRegion.java             # enum régions accessoires — interne au module au sprint 3
│   │   ├── ExerciseName.java           # VO
│   │   ├── ExerciseSet.java            # VO
│   │   ├── RPE.java                    # VO (1.0-10.0, .5 increments OK)
│   │   └── exceptions/
│   │       ├── EmptyWorkoutSessionException.java
│   │       ├── InvalidPerformedAtException.java
│   │       ├── InvalidExerciseException.java
│   │       └── InvalidRPEException.java
│   └── port/
│       └── WorkoutSessionRepository.java
├── application/
│   ├── command/
│   │   └── LogWorkoutUseCase.java      # crée WorkoutSession + publie event
│   ├── query/
│   │   ├── GetWorkoutHistoryUseCase.java
│   │   └── GetWorkoutSessionUseCase.java
│   └── mapper/
│       └── WorkoutSessionToEventMapper.java  # domain → snapshot pour l'event
└── infrastructure/
    ├── persistence/
    │   ├── WorkoutSessionJpaEntity.java
    │   ├── WorkoutSessionJpaRepository.java
    │   ├── WorkoutSessionPersistenceAdapter.java
    │   └── json/
    │       └── ExercisesJson.java       # DTO JSON pour les exercices (JSONB)
    ├── web/
    │   ├── WorkoutSessionController.java
    │   └── dto/
    │       ├── WorkoutSessionDto.java
    │       ├── LogWorkoutDto.java
    │       └── WorkoutHistoryItemDto.java
    └── config/
        └── PersonalTrainingModuleConfig.java
```

### Modifications dans le module Roster

```
backend/src/main/java/dev/ryanfoerster/atlas/roster/
├── domain/
│   └── model/
│       ├── Athlete.java                  # ajouter trainingHistory
│       └── TrainingHistory.java          # nouveau VO
├── application/
│   └── eventhandler/
│       └── WorkoutLoggedHandler.java     # nouveau handler @ApplicationModuleListener
└── infrastructure/
    ├── persistence/
    │   ├── AthleteJpaEntity.java         # ajouter colonnes/JSONB pour TrainingHistory
    │   └── json/
    │       └── TrainingHistoryJson.java  # DTO JSON (si stocké en JSONB)
    └── ...
```

**Migration nécessaire pour Roster** : V00X qui ajoute les colonnes ou JSONB de TrainingHistory sur la table `athletes`. Si JSONB cohérent avec le pattern sprint 2.

### Migration Flyway

- **V007 (ou prochain numéro libre)** : `create_workout_sessions_table.sql` + `add_training_history_to_athletes.sql`
- **V008** : script Modulith pour la table `event_publication` (à récupérer de la doc Modulith)
- **V009** : potentiel ajout MovementPattern.ACCESSORY si nécessaire (en réalité ce sera un changement d'enum, pas une migration Flyway — mais à valider)

**Décision sur le stockage des exercices** : JSONB Postgres. Justification : `LoggedExercise` est une structure imbriquée (exercise → list of sets), pas besoin de query SQL fine au sprint 3. Cohérent avec le pattern sprint 2 pour Genetics/CurrentStats. À documenter dans ADR-024 ou ailleurs.

### Index nécessaires

- `workout_sessions.owner_id` indexed (historique par user)
- `workout_sessions.performed_at` indexed DESC (ordre chronologique)
- Composite `(owner_id, performed_at DESC)` pour la pagination de l'historique

### Endpoints REST

```
POST /api/personal-training/sessions
  Body: {
    "performedAt": "2026-06-23T18:30:00Z",
    "durationMinutes": 75,
    "exercises": [
      {
        "name": "Back Squat",
        "pattern": "SQUAT",
        "sets": [
          { "reps": 5, "weightKg": 140, "rpe": 7.5 },
          { "reps": 5, "weightKg": 140, "rpe": 8 },
          { "reps": 5, "weightKg": 140, "rpe": 8.5 }
        ]
      },
      ...
    ],
    "notes": "Bonne forme, sensations propres"
  }
  Response: 201 Created + WorkoutSessionDto
  Erreurs: 400 si invariants violés (séance vide, performedAt futur, etc.), 401 si non auth

GET /api/personal-training/sessions
  Query params: ?page=0&size=20 (défaut)
  Response: 200 + paginated list of WorkoutHistoryItemDto (id, performedAt, totalSets, patternsCovered)
  Ordre : performedAt DESC

GET /api/personal-training/sessions/:id
  Response: 200 + WorkoutSessionDto complet, ou 404
  Si la session n'appartient pas au user authentifié, 404 (sécurité naturelle comme sprint 2)
```

### Modification du module Roster côté API

Le `GET /api/roster/athletes/:id` retourne maintenant `trainingHistory` dans le payload de l'athlète miroir (null ou TrainingHistory vide pour les athlètes virtuels — leur historique viendra avec Programming au sprint 6+).

### Frontend Angular

**Pages à créer** :

- **`/training`** : historique des séances (liste paginée chronologique). Bouton "Logger une séance" qui ouvre `/training/log`
- **`/training/log`** : formulaire de logging (tableau dynamique d'exercices)
- **`/training/sessions/:id`** : détail d'une séance loggée

**Modifications de `/roster/athletes/:id`** : afficher la section "Historique d'entraînement" pour le miroir avec `workoutCount`, `lastWorkoutAt`, derniers patterns travaillés.

**Modifications de la navigation principale (header)** : ajouter un lien "Entraînement" (ou icône) pour accéder à `/training` depuis n'importe où.

**Composants design system probablement nouveaux** :
- **`ExerciseLogRow`** : ligne du tableau dynamique avec nom + pattern (dropdown) + sets (sub-rows) + actions (supprimer)
- **`ExerciseSetRow`** : sub-ligne avec reps / weight / rpe inputs
- **`WorkoutSessionCard`** : carte de séance dans l'historique (date relative, durée, patterns travaillés)

Si un composant manque, applique la procédure §6 (proposer la spec, ajouter au design system, mettre à jour preview.html, puis coder).

**UX critique du tableau dynamique** :
- Densité Football Manager : afficher beaucoup d'info dans peu d'espace, monospace pour les chiffres
- Saisie rapide au clavier (Tab entre champs, Enter pour ajouter une nouvelle set)
- "Ajouter exercice" → ajoute une ligne avec un set vide par défaut
- "Ajouter set" sur un exercice → duplique le dernier set saisi (UX standard des trackers fitness)
- Validation client + serveur (au moins 1 exercice, au moins 1 set par exercice)

### Tests requis

**Tests unitaires domaine** :
- `WorkoutSessionTest` : invariants (séance non vide, performedAt valide), création via log(), méthodes computed (totalSets, totalReps, estimatedVolume, patternsCovered)
- `LoggedExerciseTest`, `ExerciseCategoryTest`, `ExerciseSetTest`, `ExerciseNameTest`, `RPETest` : value objects auto-validants (dont le pattern matching sur le sealed `ExerciseCategory`)
- `WorkoutSessionToEventMapperTest` : la conversion vers le snapshot DTO est correcte et préserve les valeurs

**Tests d'intégration** :
- `LogWorkoutUseCaseIntegrationTest` : flux nominal + publication d'event
- `WorkoutSessionPersistenceAdapterTest` : round-trip avec JSONB des exercices
- `WorkoutLoggedHandlerTest` (côté Roster) : event reçu → TrainingHistory du miroir mis à jour
- `WorkoutSessionControllerIntegrationTest` : endpoints REST avec MockMvc

**Tests Modulith — important sur ce sprint** :
- Mise à jour du test `ModuleViolationDetectionTest` si besoin
- Vérifier que Roster importe bien `WorkoutLogged` depuis `personaltraining.api.events.*` uniquement (pas depuis `personaltraining.domain.*`)
- ArchUnit (déjà en place) : vérifier que le pattern handler est bien dans `application/eventhandler/`

**Test event-driven end-to-end** :
- Test d'intégration qui : 1) logge une séance via le use case PersonalTraining 2) attend la consommation de l'event 3) vérifie que le Roster a bien été mis à jour
- Spring Modulith fournit `Scenario` API pour ce genre de test : https://docs.spring.io/spring-modulith/reference/testing.html

### Coverage cible

Maintenir 80%+ sur `personaltraining.domain.*` (JaCoCo enforced).

---

## Règles d'architecture applicables (rappel — non-négociables)

- **DDD tactique strict** : domaine pur dans `domain/`, zéro Spring/JPA/Jackson (ADR-003)
- **Aggregate WorkoutSession immutable** : méthodes qui retournent de nouvelles instances
- **Value objects auto-validants** : RPE, ExerciseName, ExerciseSet, LoggedExercise
- **Mappers manuels** à la frontière domain ↔ JPA (ADR-015), pas MapStruct
- **Snapshots pour events publics** : les events vivent dans `api/events/`, le domain riche reste interne
- **Isolation Modulith stricte** : Roster importe `WorkoutLogged` depuis `personaltraining.api.events.*` UNIQUEMENT
- **Event publication registry** activé pour at-least-once
- **Tests sur Testcontainers**, jamais H2
- **Erreur technique vs violation métier** : DomainException pour violations métier (400)
- **Conventional Commits**

---

## Leçons rétro sprint 2 à appliquer

(à compléter par Claude Code en lisant la rétro sprint 2, mais voici ce qu'on en attend par défaut)

1. **Réserver les numéros d'ADR à l'avance** : fait ci-dessus (023, 024, 025)
2. **Simuler la topologie réelle (navigateur) tôt** : dès que le backend `/api/personal-training/sessions` répond, teste-le aussi dans le navigateur même si le frontend est en cours
3. **Prouver le contrat backend en curl avant le frontend** : avant de coder le tableau dynamique Angular, fais un curl du endpoint avec un payload riche pour confirmer le shape exact
4. **Test event-driven en priorité** : ne pas découvrir tard que la publication ou la consommation a un bug
5. **Lecture critique du prompt avant exécution** : comme au sprint 2, Claude Code doit signaler les tensions du prompt avant de coder

---

## Definition of Done

Le sprint 3 est considéré terminé quand TOUS ces critères sont vérifiés :

- [ ] `./mvnw clean verify` passe
- [ ] Test Modulith verify passe (régression)
- [ ] Test ModuleViolationDetectionTest passe (régression)
- [ ] Test d'isolation event-driven : Roster importe `WorkoutLogged` uniquement depuis `personaltraining.api.events.*`
- [ ] Coverage domain PersonalTraining ≥ 80% (JaCoCo enforced)
- [ ] Migrations Flyway pour workout_sessions, modification athletes, table event_publication Modulith
- [ ] Endpoint POST /api/personal-training/sessions fonctionne
- [ ] Endpoint GET /api/personal-training/sessions (paginé) fonctionne
- [ ] Endpoint GET /api/personal-training/sessions/:id fonctionne
- [ ] Event WorkoutLogged publié à chaque session loggée (vérifié en test)
- [ ] Event WorkoutLogged consommé par Roster : trainingHistory du miroir mis à jour
- [ ] Test event-driven end-to-end : log session → vérif que Roster reçoit
- [ ] Fiche athlète miroir frontend affiche le TrainingHistory
- [ ] Flow complet en navigateur : login → /training → log session → voir dans historique → voir compteur sur fiche miroir
- [ ] CI GitHub Actions verte
- [ ] ADR-023, 024, 025 rédigés et commités (ou justification d'absence si abandonnés)
- [ ] `docs/learning/sprint-03-personaltraining-and-events.md` rédigé (mini-cours, focus event-driven)
- [ ] Devblog `docs/blog/04-premier-event-modulith.md` rédigé
- [ ] `docs/sprints/sprint-03-personaltraining/RETROSPECTIVE.md` rédigé
- [ ] CLAUDE.md mis à jour si nouvelles conventions émergent
- [ ] Récap pédagogique format A
- [ ] Glossary mis à jour (WorkoutSession, LoggedExercise, RPE, TrainingHistory, WorkoutLogged event, etc.)

---

## Contraintes à respecter

- **Pas de raccourcis sur le DDD.** Les templates Identity ET Roster sont tes guides.
- **Pas d'invention de pattern.** Si tu hésites, regarde Identity ou Roster.
- **Pas de logique métier dans les use cases** : orchestration uniquement. Tout calcul = domaine.
- **Pas d'optimisation prématurée des events** : on commence simple (event sync via Modulith registry), on optimisera si besoin.
- **Décisions de design métier non triviales** → confirme avec Ryan avant de trancher (notamment : que se passe-t-il si l'event handler échoue ? Le registry réessaie combien de fois ? Quels comportements en cas de timeout ?).

---

## Contexte métier (le pourquoi)

PersonalTraining est le **pont fitness IRL ↔ jeu**. C'est ce qui transforme Atlas d'un "jeu de simulation pure" en "outil qui valorise mes vraies séances". Le hook utilisateur central.

Au sprint 3 on pose le mécanisme : tu logges, l'event part, le miroir reçoit. Au sprint 4 (Athletics), le miroir va vraiment **progresser** en fonction de ces séances (Banister). Au sprint 6+ (Programming), compléter un cycle de programme IRL débloquera le template du programme dans le jeu.

L'objectif clé du sprint 3 c'est de **valider l'architecture event-driven Modulith** sur un cas concret. Si l'isolation tient (Roster ignore tout du domain interne de PersonalTraining sauf via l'event), c'est un signal très fort pour la suite. Si elle ne tient pas, on va devoir refactorer avant le sprint 4.

Deuxième objectif : **livrer une UX de logger qui ne fait pas pleurer**. Beaucoup de trackers fitness ont des loggers infâmes. Le tien doit être dense, rapide, clavier-friendly — du Football Manager pour saisir une séance.

---

## Format de récap attendu

À la fin de ce sprint :

**Récap pédagogique de session** (format A, CLAUDE.md §7).

**Mini-cours `sprint-03-personaltraining-and-events.md`** (format C). Concepts à couvrir :

1. **Event-driven entre modules avec Spring Modulith** : publish/subscribe, isolation préservée, garantie at-least-once via event publication registry
2. **Pattern snapshot DTO pour events publics** : pourquoi exposer le domain riche dans un event est une fuite d'isolation, comment le snapshot DTO résout le problème
3. **Aggregate autonome vs collection d'entités** : différence entre WorkoutSession (chaque séance = un aggregate autonome) et Roster (un aggregate contenant des entités internes). Quand préférer l'un ou l'autre.
4. **Tests event-driven** : Scenario API de Modulith, comment tester que A publie et B consomme
5. **MovementPattern comme pivot métier** : comment une catégorisation simple permet de joindre des concepts (séance → muscles → génétique) au sprint 4

Plus court que le mini-cours sprint 1 (5 concepts au max). Auto-évaluation 5-8 questions.

**Devblog post #4** "Premier event Modulith en production" — sujet rare et précieux. Beaucoup de devs connaissent Modulith de loin, peu ont écrit un vrai use case event-driven et l'ont testé end-to-end. Format récit avec extraits de code, ~1500-2000 mots. Publiable Reddit r/java + LinkedIn.

---

## Première instruction concrète

Quand tu commences l'exécution :

1. Lis toutes les sources préalables (section "Contexte projet"). Notamment la doc Modulith Application Events si pas encore familière.
2. Confirme à Ryan que tu as bien tout lu, et fait une lecture critique : où penses-tu copier les templates Identity/Roster directement, où vois-tu des adaptations nécessaires ?
3. Propose un plan séquencé en sous-étapes (S0 à SN), 6-8 étapes pour ce sprint, avec paliers de validation. Inclue **un palier dédié au test event-driven** parce que c'est le risque structurel principal.
4. Pose les questions de clarification si besoin (notamment : comportement du registry en cas d'échec consumer ? configuration timeout ? @ApplicationModuleListener sync ou async ?).
5. Attends la validation du plan avant de coder.
6. Exécute étape par étape avec paliers comme au sprint 2.

---

*Sprint 3 — PersonalTraining + event-driven Modulith. Estimé 1.5-2 semaines à 10-15h/semaine. Comparable au sprint 2 en charge (template DDD réutilisé, mais nouveauté event-driven).*
