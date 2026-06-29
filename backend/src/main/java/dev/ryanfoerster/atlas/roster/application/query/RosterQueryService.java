package dev.ryanfoerster.atlas.roster.application.query;

import dev.ryanfoerster.atlas.roster.api.AthleteLoadProfile;
import dev.ryanfoerster.atlas.roster.api.AthleteStrengthCeiling;
import dev.ryanfoerster.atlas.roster.api.GeneticProfile;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.StrengthStandards;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implémente le port public {@link RosterQueryPort}. Adapter applicatif vers le repository du domaine —
 * orchestration pure, pas de logique métier.
 */
@Service
public class RosterQueryService implements RosterQueryPort {

    private final RosterRepository rosterRepository;

    public RosterQueryService(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @Override
    public Optional<AthleteId> findMirrorAthleteId(UserId owner) {
        return rosterRepository.findByOwnerId(owner)
                .flatMap(roster -> roster.mirrorAthlete())
                .map(Athlete::id);
    }

    @Override
    public Optional<GeneticProfile> findGeneticProfile(AthleteId athleteId) {
        return rosterRepository.findByAthleteId(athleteId)
                .flatMap(roster -> roster.findAthlete(athleteId))
                .map(Athlete::genetics)
                .map(RosterQueryService::toProfile);
    }

    @Override
    public Optional<AthleteLoadProfile> findLoadProfile(AthleteId athleteId) {
        return rosterRepository.findByAthleteId(athleteId)
                .flatMap(roster -> roster.findAthlete(athleteId))
                .map(RosterQueryService::toLoadProfile);
    }

    @Override
    public Optional<AthleteStrengthCeiling> findStrengthCeiling(AthleteId athleteId) {
        return rosterRepository.findByAthleteId(athleteId)
                .flatMap(roster -> roster.findAthlete(athleteId))
                .map(RosterQueryService::toStrengthCeiling);
    }

    private static GeneticProfile toProfile(Genetics g) {
        return new GeneticProfile(g.baseRecoveryRate(), g.trainingResponseSensitivity(), g.fiberTypeProfile());
    }

    private static AthleteLoadProfile toLoadProfile(Athlete athlete) {
        Map<MovementPattern, Double> oneRepMaxes = new EnumMap<>(MovementPattern.class);
        athlete.currentStats().oneRepMaxByPattern().forEach((pattern, oneRepMax) ->
                oneRepMaxes.put(pattern, oneRepMax.weight().toKilograms().doubleValue()));
        return new AthleteLoadProfile(athlete.bodyWeight().toKilograms().doubleValue(), oneRepMaxes);
    }

    /**
     * Plafond génétique par pattern = {@code bodyweight × ratio_élite × strengthAffinity}, pour les seuls
     * grands lifts dotés d'un standard ({@link StrengthStandards#hasStandard}). Roster est le seul à connaître
     * les ratios élite (T3) ; Athletics ne lit que le résultat.
     */
    private static AthleteStrengthCeiling toStrengthCeiling(Athlete athlete) {
        double bodyWeightKg = athlete.bodyWeight().toKilograms().doubleValue();
        Map<MovementPattern, Double> ceilings = new EnumMap<>(MovementPattern.class);
        for (MovementPattern pattern : MovementPattern.values()) {
            if (!StrengthStandards.hasStandard(pattern)) {
                continue;
            }
            double ceiling = bodyWeightKg
                    * StrengthStandards.eliteRatio(pattern, athlete.gender())
                    * athlete.genetics().strengthAffinity(pattern);
            ceilings.put(pattern, ceiling);
        }
        return new AthleteStrengthCeiling(ceilings);
    }
}
