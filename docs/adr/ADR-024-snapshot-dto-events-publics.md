# ADR-024 : Pattern snapshot DTO pour les events publics (isolation du domaine)

**Statut** : Accepté (GATE B/C du sprint 3 validé)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte

Un event public vit dans `api/events/` et peut être consommé par n'importe quel module. S'il transporte un
type du domaine interne (ex. `LoggedExercise`, ou le sealed `ExerciseCategory`, ou l'enum interne
`BodyRegion`), il **fuit** ce domaine dans l'API publique : tout consumer en dépendrait, et l'isolation
Modulith serait violée.

## Décision

`WorkoutLogged` ne transporte que des **snapshots aplatis**, dans `api/events/`, composés exclusivement de
types **primitifs / wrapper / `shared`** :

```java
record WorkoutLogged(UUID ownerId, UUID sessionId, Instant performedAt, Integer durationMinutes,
                     List<LoggedExerciseSnapshot> exercises) {}

record LoggedExerciseSnapshot(String name, String categoryType, MovementPattern pattern,
                              String accessoryRegion, List<ExerciseSetSnapshot> sets) {}

record ExerciseSetSnapshot(int reps, Double weightKg, Double rpe) {}
```

Choix concrets :
- **Ids en `UUID` nus**, pas les VO d'identité du domaine (même convention que les events Roster du sprint 2).
- **Sealed `ExerciseCategory` aplati** : discriminant `categoryType` (`"COMPOUND_FORCE"` / `"ACCESSORY"`) +
  `pattern` (nullable) + `accessoryRegion` (nullable). C'est précisément ce dont le consumer a besoin pour
  ne compter que les `CompoundForce` (cohérent avec `patternsCovered()`).
- **`pattern` typé `MovementPattern`** : OK car `MovementPattern` vit dans le kernel `shared` (module OPEN),
  donc accessible partout.
- **`BodyRegion` en `String`** (`region.name()`), PAS le type : `BodyRegion` est interne à PersonalTraining
  (ADR-026) ; l'exposer typé serait une fuite. Le `String` est la frontière.
- Le mapper domaine → snapshot (`WorkoutSessionToEventMapper`) vit dans `application/`, via un `switch`
  exhaustif sur le sealed.

### Exposition de l'API : named interface Modulith
Corollaire découvert au GATE C : déclarer un event dans `api/events/` ne suffit pas à le rendre *consommable*
par un autre module. Par défaut, Spring Modulith n'expose que le **package racine** d'un module ; un
sous-package (`api`, `api.events`) reste interne. Il faut le déclarer **named interface** :

```java
@org.springframework.modulith.NamedInterface("api")
package dev.ryanfoerster.atlas.personaltraining.api;        // idem api.events (même nom → fusionnés)
```

C'est la matérialisation Modulith de la règle CLAUDE.md « les modules externes ne peuvent importer que de
api/ ». Convention à reproduire sur les autres modules quand leur API sera consommée cross-module.

## Conséquences

**Positives** — l'event public ne dépend que de `shared` ; le domaine interne (LoggedExercise, ExerciseSet,
BodyRegion, les VO d'id) reste invisible des consumers ; l'isolation est vérifiée par test
(`AtlasApplicationModulesTest` + une règle ArchUnit « roster ne dépend de PersonalTraining que via api »).

**Négatives** — un peu de duplication (snapshot ≈ forme aplatie du domaine) et un mapping à maintenir ;
c'est le prix de l'isolation, assumé. Le même aplatissement sert d'ailleurs côté JSONB (ADR-019/026).
