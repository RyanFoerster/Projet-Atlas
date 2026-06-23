package dev.ryanfoerster.atlas.identity.domain.model;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Levée quand une chaîne ne peut pas former un {@link DisplayName} valide (longueur
 * hors bornes 2–50 après trim, ou caractères non autorisés).
 */
public final class InvalidDisplayNameException extends DomainException {

    public InvalidDisplayNameException(String message) {
        super(message);
    }
}
