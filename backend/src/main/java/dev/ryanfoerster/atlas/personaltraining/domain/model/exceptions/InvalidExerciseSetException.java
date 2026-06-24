package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Série invalide (répétitions hors bornes 1–100). Violation métier → 400. */
public final class InvalidExerciseSetException extends DomainException {
    public InvalidExerciseSetException(String message) {
        super(message);
    }
}
