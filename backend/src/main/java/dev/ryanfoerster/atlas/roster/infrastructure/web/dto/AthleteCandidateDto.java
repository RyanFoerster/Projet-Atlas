package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Candidat scouté affiché au joueur (génétique visible, rareté indiquée), avant recrutement. */
public record AthleteCandidateDto(
        String name,
        int age,
        BigDecimal bodyWeightKg,
        int bodyHeightCm,
        String gender,
        String rarity,
        GeneticsDto genetics,
        Map<String, BigDecimal> oneRepMaxesKg) {

    public static AthleteCandidateDto from(AthleteCandidate c) {
        Map<String, BigDecimal> orms = new LinkedHashMap<>();
        c.baseOneRepMaxes().forEach((pattern, orm) -> orms.put(pattern.name(), orm.weight().toKilograms()));
        return new AthleteCandidateDto(c.name().value(), c.age(), c.bodyWeight().toKilograms(),
                c.bodyHeight().centimeters(), c.gender().name(), c.rarity().name(),
                GeneticsDto.from(c.genetics()), orms);
    }
}
