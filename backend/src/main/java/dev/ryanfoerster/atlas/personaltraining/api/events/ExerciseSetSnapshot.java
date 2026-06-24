package dev.ryanfoerster.atlas.personaltraining.api.events;

/**
 * Forme aplatie d'une série, pour l'event public {@link WorkoutLogged}. Types primitifs/wrapper
 * uniquement (aucun VO du domaine) — un event public ne doit pas exposer le domaine interne (ADR-024).
 * {@code weightKg} {@code null} = poids de corps ; {@code rpe} {@code null} = non renseigné.
 */
public record ExerciseSetSnapshot(int reps, Double weightKg, Double rpe) {
}
