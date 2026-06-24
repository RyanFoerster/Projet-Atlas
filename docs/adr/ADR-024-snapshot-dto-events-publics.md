# ADR-024 : Pattern snapshot DTO pour les events publics (isolation du domaine)

**Statut** : Proposé — finalisation au GATE B du sprint 3 (contrat REST + event)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte (esquisse — à compléter au gate)

Un event public vit dans `api/events/` et peut être consommé par n'importe quel module. S'il transporte
un type du domaine interne (ex. `LoggedExercise`, ou le sealed `ExerciseCategory`), il **fuit** le domaine
de PersonalTraining dans l'API publique → violation de l'isolation Modulith.

## Décision (déjà tranchée)

- L'event `WorkoutLogged` ne transporte que des **snapshots aplatis** (`LoggedExerciseSnapshot`,
  `ExerciseSetSnapshot`) dans `api/events/`, jamais les VO riches du domaine.
- Le sealed `ExerciseCategory` est aplati en **discriminant + champs nullables**
  (`categoryType` + `pattern` (shared, OK) + `accessoryRegion` en `String`).
- `BodyRegion` (domaine interne) est représenté en **`String`** dans le snapshot, pour ne pas l'exposer.
- Le mapper domaine → snapshot vit dans `application/` (`WorkoutSessionToEventMapper`).

> Corps complet rédigé au GATE B (avec le contrat curl prouvé).
