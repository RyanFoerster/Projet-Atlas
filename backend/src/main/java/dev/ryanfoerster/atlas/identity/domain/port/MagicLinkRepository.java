package dev.ryanfoerster.atlas.identity.domain.port;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;

import java.util.Optional;

/**
 * Port secondaire : persistance des {@link MagicLink}. Implémenté en infrastructure (S3).
 */
public interface MagicLinkRepository {

    MagicLink save(MagicLink magicLink);

    /** Retrouve un lien par son jeton (étape clé de la consommation). */
    Optional<MagicLink> findByToken(MagicLinkToken token);
}
