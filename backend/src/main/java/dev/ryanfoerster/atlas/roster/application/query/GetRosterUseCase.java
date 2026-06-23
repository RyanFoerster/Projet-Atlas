package dev.ryanfoerster.atlas.roster.application.query;

import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Use case de lecture : l'écurie d'un Player (vide si jamais créée → la couche web fera un 404/redirect). */
@Service
public class GetRosterUseCase {

    private final RosterRepository rosterRepository;

    public GetRosterUseCase(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Roster> forOwner(UserId ownerId) {
        return rosterRepository.findByOwnerId(ownerId);
    }
}
