package dev.ryanfoerster.atlas.roster.api.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event publié à la création de l'écurie d'un Player (au premier athlète miroir). Types primitifs
 * pour ne pas fuiter les value objects internes hors du module (cf. events d'identity).
 */
public record RosterCreated(UUID rosterId, UUID ownerId, Instant createdAt) {
}
