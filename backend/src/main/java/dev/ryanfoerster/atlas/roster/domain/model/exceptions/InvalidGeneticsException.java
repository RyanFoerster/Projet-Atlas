package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Génétique incohérente (valeur hors plage, axe manquant, fiberTypeProfile hors [0,1]).
 * Violation d'invariant du domaine → 400. En pratique, la génétique étant générée par le système,
 * cette exception protège surtout contre une génération buggée (test) ou une réhydratation corrompue.
 */
public final class InvalidGeneticsException extends DomainException {
    public InvalidGeneticsException(String message) {
        super(message);
    }
}
