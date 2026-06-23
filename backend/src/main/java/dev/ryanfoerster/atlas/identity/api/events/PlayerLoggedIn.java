package dev.ryanfoerster.atlas.identity.api.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event métier publié à chaque connexion réussie d'un Player. Consommé typiquement par
 * insights (suivi d'activité) et progression.
 *
 * <p>Contrat exposé en types primitifs/standard pour rester consommable sans dépendre du
 * domaine interne d'identity (cf. {@link PlayerRegistered}).
 */
public record PlayerLoggedIn(UUID playerId, Instant loggedInAt) {
}
