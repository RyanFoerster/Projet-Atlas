package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Date/heure de séance invalide (notamment : dans le futur). Violation métier → 400. */
public final class InvalidPerformedAtException extends DomainException {
    public InvalidPerformedAtException(String message) {
        super(message);
    }
}
