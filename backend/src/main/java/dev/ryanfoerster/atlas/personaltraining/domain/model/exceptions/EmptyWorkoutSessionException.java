package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Une séance doit contenir au moins un exercice. Violation métier → 400. */
public final class EmptyWorkoutSessionException extends DomainException {
    public EmptyWorkoutSessionException(String message) {
        super(message);
    }
}
