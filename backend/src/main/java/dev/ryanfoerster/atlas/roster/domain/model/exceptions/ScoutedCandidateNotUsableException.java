package dev.ryanfoerster.atlas.roster.domain.model.exceptions;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;

/**
 * Tentative de recruter un candidat scouté expiré ou déjà recruté. Violation métier → 400/409.
 * Même esprit que {@code MagicLinkNotUsableException} (objet temporaire à usage unique, ADR-022).
 */
public final class ScoutedCandidateNotUsableException extends DomainException {
    public ScoutedCandidateNotUsableException(String message) {
        super(message);
    }
}
