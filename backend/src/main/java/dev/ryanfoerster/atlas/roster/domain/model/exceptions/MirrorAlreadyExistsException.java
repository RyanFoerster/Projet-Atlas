package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Tentative de créer un second athlète miroir dans un Roster qui en a déjà un. Invariant de
 * l'aggregate Roster (au plus un miroir). Violation métier → 409 Conflict côté web.
 */
public final class MirrorAlreadyExistsException extends DomainException {
    public MirrorAlreadyExistsException(String message) {
        super(message);
    }
}
