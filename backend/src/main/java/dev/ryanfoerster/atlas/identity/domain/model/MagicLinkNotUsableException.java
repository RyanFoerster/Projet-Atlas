package dev.ryanfoerster.atlas.identity.domain.model;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Levée quand on tente de consommer un {@link MagicLink} qui n'est plus utilisable :
 * expiré, ou déjà consommé. Violation de règle métier (un humain clique un vieux lien
 * ou un lien déjà utilisé) → {@link DomainException}, donc réponse 400 + message clair,
 * pas une erreur technique.
 */
public final class MagicLinkNotUsableException extends DomainException {

    public MagicLinkNotUsableException(String message) {
        super(message);
    }
}
