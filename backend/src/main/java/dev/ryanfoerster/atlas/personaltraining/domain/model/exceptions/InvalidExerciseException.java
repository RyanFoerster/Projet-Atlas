package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Exercice loggé invalide (notamment : aucune série). Violation métier → 400. */
public final class InvalidExerciseException extends DomainException {
    public InvalidExerciseException(String message) {
        super(message);
    }
}
