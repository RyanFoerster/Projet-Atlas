package dev.ryanfoerster.atlas.roster.application.query;

import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;

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
}
