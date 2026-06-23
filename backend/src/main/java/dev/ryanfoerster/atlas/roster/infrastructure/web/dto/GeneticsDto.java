package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.LinkedHashMap;
import java.util.Map;

/** Génétique exposée (fiche athlète). Clés enum sérialisées en chaînes. */
public record GeneticsDto(
        Map<String, Double> hypertrophyPotentialByMuscleGroup,
        Map<String, Double> strengthAffinityByPattern,
        double baseRecoveryRate,
        double fiberTypeProfile,
        double trainingResponseSensitivity) {

    public static GeneticsDto from(Genetics g) {
        Map<String, Double> hyp = new LinkedHashMap<>();
        for (MuscleGroup m : MuscleGroup.values()) {
            hyp.put(m.name(), g.hypertrophyPotential(m));
        }
        Map<String, Double> str = new LinkedHashMap<>();
        for (MovementPattern p : MovementPattern.values()) {
            str.put(p.name(), g.strengthAffinity(p));
        }
        return new GeneticsDto(hyp, str, g.baseRecoveryRate(), g.fiberTypeProfile(), g.trainingResponseSensitivity());
    }
}
