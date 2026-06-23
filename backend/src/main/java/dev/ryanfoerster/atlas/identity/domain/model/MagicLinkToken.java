package dev.ryanfoerster.atlas.identity.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Jeton secret d'un {@link MagicLink}, encapsulant un {@link UUID} version 7 (RFC 9562).
 *
 * <p>Value object immutable et auto-validant. C'est la valeur transmise dans l'URL du lien
 * magique : qui la possède peut consommer le lien (dans sa fenêtre de validité). On la
 * modélise comme un type distinct de {@code UserId} pour éviter toute confusion entre un
 * identifiant d'utilisateur et un secret d'authentification — deux UUID, deux rôles métier
 * radicalement différents.
 *
 * <p>UUID v7 ici aussi : indexable efficacement (les magic links sont nettoyés par lots
 * via {@code expires_at}) et non devinable en pratique (122 bits aléatoires/temporels).
 */
public record MagicLinkToken(UUID value) {

    public MagicLinkToken {
        if (value == null) {
            throw new IllegalArgumentException("MagicLinkToken ne peut pas encapsuler un UUID null");
        }
    }

    /**
     * Génère un nouveau jeton unique (UUID v7).
     */
    public static MagicLinkToken generate() {
        return new MagicLinkToken(UuidCreator.getTimeOrderedEpoch());
    }

    /**
     * Reconstruit un jeton depuis sa représentation textuelle (ex. le paramètre d'URL reçu).
     *
     * @throws IllegalArgumentException si {@code raw} est null ou n'est pas un UUID valide
     */
    public static MagicLinkToken from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("MagicLinkToken ne peut pas être construit depuis une chaîne null");
        }
        try {
            return new MagicLinkToken(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MagicLinkToken invalide : « " + raw + " » n'est pas un UUID", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
