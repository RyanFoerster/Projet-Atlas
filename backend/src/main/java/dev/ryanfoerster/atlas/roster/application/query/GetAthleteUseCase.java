package dev.ryanfoerster.atlas.roster.application.query;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteId;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case de lecture : un athlète du roster du Player. On charge le roster du Player et on navigue
 * vers l'athlète (Athlete est une entity interne, ADR-019) → un athlète qui n'est pas dans le roster
 * du Player demandeur renvoie {@code empty} (→ 404), sécurité naturelle.
 */
@Service
public class GetAthleteUseCase {

    private final RosterRepository rosterRepository;

    public GetAthleteUseCase(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Athlete> forOwner(UserId ownerId, AthleteId athleteId) {
        return rosterRepository.findByOwnerId(ownerId).flatMap(roster -> roster.findAthlete(athleteId));
    }
}
