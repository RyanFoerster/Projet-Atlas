package dev.ryanfoerster.atlas.identity.api.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event métier publié quand un nouveau Player s'inscrit (premier login = signup implicite).
 * Consommé typiquement par roster (créer l'écurie initiale) et insights (projections).
 *
 * <p><strong>Pourquoi des types primitifs ({@link UUID}, {@link String}) et pas les value
 * objects du domaine ?</strong> Cet event vit dans {@code api/} : il franchit la frontière du
 * module. S'il portait un {@code UserId} ou un {@code Email} (qui vivent dans
 * {@code identity.domain.model}, package <em>non exporté</em>), tout module consommateur
 * devrait importer le domaine d'identity → violation d'isolation Spring Modulith. On expose
 * donc un contrat stable et autonome, découplé de la représentation interne.
 */
public record PlayerRegistered(UUID playerId, String email, String displayName, Instant registeredAt) {
}
