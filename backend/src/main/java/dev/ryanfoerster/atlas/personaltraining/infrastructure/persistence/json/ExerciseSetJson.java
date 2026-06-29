package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import java.math.BigDecimal;

/**
 * Forme sérialisable (JSONB) d'une série. DTO d'infrastructure : touché par Jackson, contrairement au
 * domaine pur (ADR-015).
 *
 * <p>Le sealed {@code Load} est aplati en discriminant {@code loadType} + valeur {@code weightKg} (sprint 6,
 * ADR-035) : {@code BODYWEIGHT} → {@code weightKg} {@code null} ; {@code WEIGHTED} → charge ajoutée (kg) ;
 * {@code EXTERNAL} → charge externe (kg). {@code rpe} {@code null} si non renseigné. Les lignes antérieures
 * au sprint 6 reçoivent leur {@code loadType} via la migration {@code V013}.
 */
public record ExerciseSetJson(int reps, String loadType, BigDecimal weightKg, Double rpe) {
}
