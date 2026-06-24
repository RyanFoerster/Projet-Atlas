package dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** RPE invalide (hors bornes 1.0–10.0 ou pas un incrément de 0.5). Violation métier → 400. */
public final class InvalidRPEException extends DomainException {
    public InvalidRPEException(String message) {
        super(message);
    }
}
