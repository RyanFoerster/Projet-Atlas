package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.CurrentStats;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.EnumMap;
import java.util.Map;

/**
 * Conversions domaine ↔ DTO JSON (le volet « sérialisation » du mapping manuel, extension d'ADR-015).
 * Le domaine reste pur : c'est ici, en infrastructure, qu'on traduit vers/depuis des structures que
 * Jackson saura écrire en jsonb. Poids normalisés en kg.
 */
public final class RosterJsonConverter {

    private RosterJsonConverter() {
    }

    public static GeneticsJson toJson(Genetics g) {
        return new GeneticsJson(g.hypertrophyPotentialByMuscleGroup(), g.strengthAffinityByPattern(),
                g.baseRecoveryRate(), g.fiberTypeProfile(), g.trainingResponseSensitivity());
    }

    public static Genetics fromJson(GeneticsJson j) {
        return new Genetics(j.hypertrophyPotentialByMuscleGroup(), j.strengthAffinityByPattern(),
                j.baseRecoveryRate(), j.fiberTypeProfile(), j.trainingResponseSensitivity());
    }

    public static CurrentStatsJson toJson(CurrentStats stats) {
        return new CurrentStatsJson(oneRepMaxesToJson(stats.oneRepMaxByPattern()));
    }

    public static CurrentStats fromJson(CurrentStatsJson j) {
        return new CurrentStats(oneRepMaxesFromJson(j.oneRepMaxByPattern()));
    }

    public static AthleteCandidateJson toJson(AthleteCandidate c) {
        return new AthleteCandidateJson(c.name().value(), c.age(), c.bodyWeight().toKilograms(),
                c.bodyHeight().centimeters(), c.gender().name(), toJson(c.genetics()),
                oneRepMaxesToJson(c.baseOneRepMaxes()), c.rarity().name());
    }

    public static AthleteCandidate fromJson(AthleteCandidateJson j) {
        return new AthleteCandidate(AthleteName.of(j.name()), j.age(), Weight.ofKilograms(j.bodyWeightKg()),
                Height.ofCentimeters(j.bodyHeightCm()), Gender.valueOf(j.gender()), fromJson(j.genetics()),
                oneRepMaxesFromJson(j.baseOneRepMaxes()), Rarity.valueOf(j.rarity()));
    }

    private static Map<MovementPattern, OneRepMaxJson> oneRepMaxesToJson(Map<MovementPattern, OneRepMax> map) {
        Map<MovementPattern, OneRepMaxJson> result = new EnumMap<>(MovementPattern.class);
        map.forEach((pattern, orm) ->
                result.put(pattern, new OneRepMaxJson(orm.weight().toKilograms(), orm.source().name())));
        return result;
    }

    private static Map<MovementPattern, OneRepMax> oneRepMaxesFromJson(Map<MovementPattern, OneRepMaxJson> map) {
        Map<MovementPattern, OneRepMax> result = new EnumMap<>(MovementPattern.class);
        map.forEach((pattern, j) ->
                result.put(pattern, new OneRepMax(Weight.ofKilograms(j.weightKg()), OneRepMax.Source.valueOf(j.source()))));
        return result;
    }
}
