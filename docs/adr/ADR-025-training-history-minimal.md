# ADR-025 : TrainingHistory minimal au sprint 3, enrichissement Banister au sprint 4

**Statut** : Proposé — finalisation au GATE C du sprint 3 (côté Roster)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte (esquisse — à compléter au gate)

Quand Roster consomme `WorkoutLogged`, il doit enregistrer que des séances ont eu lieu sur le miroir.
La tentation serait de calculer dès maintenant un état fitness/fatigue — mais c'est le périmètre
d'Athletics au sprint 4. Au sprint 3 on **enregistre seulement**.

## Décision (déjà tranchée)

- Ajout d'un VO `TrainingHistory` (immutable) sur l'entité `Athlete` : `workoutCount`, `lastWorkoutAt`
  (nullable), `lastPatternsCovered` (`Set<MovementPattern>` de la dernière séance).
- **Compteur passif** : aucun calcul fitness/fatigue au sprint 3. Au sprint 4, Athletics consommera ces
  données (et l'event) pour piloter le `FitnessFatigueState` par `MuscleGroup` (modèle Banister, ADR-004).
- Stockage JSONB sur la table `athletes` (cohérent avec `genetics`/`current_stats`, ADR-019), migration V009.

> Corps complet rédigé au GATE C.
