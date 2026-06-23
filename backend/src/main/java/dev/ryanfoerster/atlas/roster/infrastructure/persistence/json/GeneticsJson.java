package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Map;

/**
 * Forme sérialisable (JSONB) de {@code Genetics}. DTO d'infrastructure : il peut être touché par
 * Jackson (Hibernate l'écrit en jsonb), contrairement au domaine qui reste pur (ADR-003, ADR-015).
 * Les clés enum sont sérialisées par Jackson sous leur nom.
 */
public record GeneticsJson(
        Map<MuscleGroup, Double> hypertrophyPotentialByMuscleGroup,
        Map<MovementPattern, Double> strengthAffinityByPattern,
        double baseRecoveryRate,
        double fiberTypeProfile,
        double trainingResponseSensitivity) {
}
