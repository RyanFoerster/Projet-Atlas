package dev.ryanfoerster.atlas.roster.domain.port;

import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.model.RosterId;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.util.Optional;

/**
 * Port secondaire : persistance de l'aggregate {@link Roster}. Le Roster est chargé/sauvé
 * <strong>en entier</strong> (avec ses athlètes — entities internes), conformément à son statut
 * d'aggregate root (ADR-019). Implémenté en infrastructure.
 */
public interface RosterRepository {

    Roster save(Roster roster);

    Optional<Roster> findById(RosterId id);

    /** Le roster d'un Player (au plus un, owner_id unique). */
    Optional<Roster> findByOwnerId(UserId ownerId);
}
