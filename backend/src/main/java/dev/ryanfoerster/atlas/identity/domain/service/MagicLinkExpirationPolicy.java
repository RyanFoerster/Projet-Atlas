package dev.ryanfoerster.atlas.identity.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Politique de domaine qui décide de la durée de validité d'un lien magique.
 *
 * <p>Stateless et pure : à partir de l'instant d'émission, elle calcule l'instant
 * d'expiration. La durée de vie (TTL) par défaut est de 15 minutes (cf. décision sprint :
 * fenêtre courte = surface d'attaque réduite), mais reste paramétrable pour les tests ou
 * une éventuelle configuration future.
 *
 * <p>Isoler cette règle dans une <em>policy</em> plutôt que de coder « +15 min » en dur dans
 * l'entity ou le use case, c'est rendre la décision métier explicite, nommée et testable
 * indépendamment.
 */
public final class MagicLinkExpirationPolicy {

    /** TTL par défaut d'un lien magique : 15 minutes. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final Duration ttl;

    public MagicLinkExpirationPolicy() {
        this(DEFAULT_TTL);
    }

    public MagicLinkExpirationPolicy(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Le TTL doit être strictement positif : " + ttl);
        }
        this.ttl = ttl;
    }

    /** Calcule l'instant d'expiration d'un lien émis à {@code issuedAt}. */
    public Instant expiresAt(Instant issuedAt) {
        Objects.requireNonNull(issuedAt, "issuedAt");
        return issuedAt.plus(ttl);
    }

    public Duration ttl() {
        return ttl;
    }
}
