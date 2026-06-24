package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import java.math.BigDecimal;

/**
 * Forme sérialisable (JSONB) d'une série. DTO d'infrastructure : touché par Jackson, contrairement au
 * domaine pur (ADR-015). {@code weightKg} normalisé en kg, {@code null} pour le poids de corps ;
 * {@code rpe} {@code null} si non renseigné.
 */
public record ExerciseSetJson(int reps, BigDecimal weightKg, Double rpe) {
}
