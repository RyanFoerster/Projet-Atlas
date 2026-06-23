package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Taille corporelle hors bornes plausibles. Violation métier → 400. */
public final class InvalidHeightException extends DomainException {
    public InvalidHeightException(String message) {
        super(message);
    }
}
