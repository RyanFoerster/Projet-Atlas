package dev.ryanfoerster.atlas.identity.domain.model;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Levée à la finalisation d'inscription si un Player existe déjà pour cet email (double
 * soumission, ou compte créé entre-temps). Protège l'invariant d'unicité au niveau applicatif,
 * avant de heurter la contrainte SQL — message clair plutôt qu'une erreur de base brute.
 */
public final class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}
