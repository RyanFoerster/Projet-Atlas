# ADR-026 : Catégorisation des exercices loggés via sealed interface `ExerciseCategory`

**Statut** : Proposé — finalisation au GATE A du sprint 3 (modélisation du domaine)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte (esquisse — à compléter au gate)

`MovementPattern` (kernel `shared`) sert déjà d'**axes de force génétique** dans Roster (`Genetics`).
Le sprint 3 a besoin de catégoriser un exercice loggé (squat, mais aussi curl, gainage…). Réutiliser
`MovementPattern` directement obligerait à y ajouter `ACCESSORY` — or un accessoire **n'est pas un axe
de force génétique**. L'ajouter polluerait Roster (générateur, JSONB, panel génétique).

## Décision (déjà tranchée — tension #1, option B)

- `MovementPattern` reste **inchangé** (6 axes de force, zéro pollution Roster).
- La catégorisation d'un exercice loggé est un **concept distinct** dans `personaltraining.domain` :

  ```java
  sealed interface ExerciseCategory permits CompoundForce, Accessory {
      record CompoundForce(MovementPattern pattern) implements ExerciseCategory {}
      record Accessory(BodyRegion region) implements ExerciseCategory {}
  }
  ```

- `BodyRegion` : enum interne au module au sprint 3 (anti-dette ADR-017 ; promotion à `shared` au sprint 4
  si Athletics en a besoin).
- `WorkoutSession.patternsCovered()` ne retourne que les `MovementPattern` des `CompoundForce`.
- Extensible : `Cardio`, `Mobility`… = nouvelles variantes du sealed interface.

> Corps complet (contexte détaillé, conséquences, alternatives a/c rejetées) rédigé au GATE A.
