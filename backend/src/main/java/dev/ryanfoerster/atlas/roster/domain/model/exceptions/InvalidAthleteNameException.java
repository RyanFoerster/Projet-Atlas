package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/** Nom d'athlète invalide (longueur hors bornes, caractères non autorisés). Violation métier → 400. */
public final class InvalidAthleteNameException extends DomainException {
    public InvalidAthleteNameException(String message) {
        super(message);
    }
}
