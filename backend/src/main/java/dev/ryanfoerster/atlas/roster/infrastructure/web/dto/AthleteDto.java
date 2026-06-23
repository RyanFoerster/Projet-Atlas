package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fiche détaillée d'un athlète (réponse de GET /api/roster/athletes/:id, POST mirror/recruit). */
public record AthleteDto(
        String id,
        String name,
        int age,
        BigDecimal bodyWeightKg,
        int bodyHeightCm,
        String gender,
        String rarity,
        boolean mirror,
        GeneticsDto genetics,
        Map<String, BigDecimal> oneRepMaxesKg,
        Instant recruitedAt) {

    public static AthleteDto from(Athlete a) {
        Map<String, BigDecimal> orms = new LinkedHashMap<>();
        a.currentStats().oneRepMaxByPattern()
                .forEach((pattern, orm) -> orms.put(pattern.name(), orm.weight().toKilograms()));
        return new AthleteDto(a.id().toString(), a.name().value(), a.age(), a.bodyWeight().toKilograms(),
                a.bodyHeight().centimeters(), a.gender().name(), a.rarity().name(), a.isMirror(),
                GeneticsDto.from(a.genetics()), orms, a.recruitedAt());
    }
}
