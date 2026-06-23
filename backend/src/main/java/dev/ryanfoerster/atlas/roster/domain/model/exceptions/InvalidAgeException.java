package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Âge d'athlète hors bornes autorisées. Violation métier → 400. */
public final class InvalidAgeException extends DomainException {
    public InvalidAgeException(String message) {
        super(message);
    }
}
