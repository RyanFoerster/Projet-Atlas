package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Nom d'exercice invalide (longueur hors bornes 2–80). Violation métier → 400. */
public final class InvalidExerciseNameException extends DomainException {
    public InvalidExerciseNameException(String message) {
        super(message);
    }
}
