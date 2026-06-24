package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Champ de séance hors bornes au niveau de l'aggregate (durée ≤ 0 ou aberrante, notes trop longues).
 * Violation métier → 400.
 */
public final class InvalidWorkoutSessionException extends DomainException {
    public InvalidWorkoutSessionException(String message) {
        super(message);
    }
}
