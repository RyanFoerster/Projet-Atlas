package dev.ryanfoerster.atlas.identity.domain.model;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Levée quand une chaîne ne peut pas former un {@link Email} valide (null, vide,
 * trop longue, ou ne respectant pas le format attendu). Exception métier nommée :
 * elle exprime une règle du domaine, pas un détail technique.
 */
public final class InvalidEmailException extends DomainException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
