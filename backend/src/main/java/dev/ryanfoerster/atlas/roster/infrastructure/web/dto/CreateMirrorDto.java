package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Corps de POST /api/roster/mirror. {@code oneRepMaxes} : clé = nom du pattern (SQUAT, BENCH_PRESS,
 * DEADLIFT, OVERHEAD_PRESS), valeur = 1RM en kg.
 */
public record CreateMirrorDto(
        String name,
        int age,
        BigDecimal bodyWeightKg,
        int bodyHeightCm,
        String gender,
        Map<String, BigDecimal> oneRepMaxes) {
}
