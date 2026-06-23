package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Forme sérialisable d'un {@code AthleteCandidate} : tout le candidat est stocké en un seul blob JSONB
 * dans {@code scouted_candidates.candidate} (ADR-022). Poids normalisés en kg.
 */
public record AthleteCandidateJson(
        String name,
        int age,
        BigDecimal bodyWeightKg,
        int bodyHeightCm,
        String gender,
        GeneticsJson genetics,
        Map<MovementPattern, OneRepMaxJson> baseOneRepMaxes,
        String rarity) {
}
