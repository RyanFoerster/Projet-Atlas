package dev.ryanfoerster.atlas.identity.domain.model;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Levée quand un jeton de lien magique présenté ne correspond à aucun lien connu (jeton
 * inexistant, ou chaîne qui n'est même pas un UUID valide). Violation métier du point de vue
 * de l'utilisateur (« ce lien n'existe pas/plus ») → {@link DomainException}, réponse 400/404.
 *
 * <p>À distinguer de {@link MagicLinkNotUsableException} (le lien existe mais est expiré ou
 * déjà consommé).
 */
public final class InvalidMagicLinkException extends DomainException {

    public InvalidMagicLinkException(String message) {
        super(message);
    }
}
