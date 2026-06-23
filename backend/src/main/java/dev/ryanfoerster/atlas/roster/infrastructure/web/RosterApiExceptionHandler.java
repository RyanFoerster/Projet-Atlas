package dev.ryanfoerster.atlas.roster.infrastructure.web;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.MirrorAlreadyExistsException;
import dev.ryanfoerster.atlas.roster.domain.model.exceptions.ScoutedCandidateNotUsableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Codes HTTP spécifiques au module roster, plus fins que le {@code DomainException → 400} global
 * (identity). Toutes ces exceptions héritent de {@code DomainException} ; pour que ces mappings
 * précis gagnent, ce handler a la <strong>précédence la plus haute</strong> (sinon le handler global
 * intercepterait {@code DomainException} en premier et renverrait 400).
 *
 * <ul>
 *   <li>Miroir déjà existant → <strong>409 Conflict</strong></li>
 *   <li>Candidat scouté introuvable / expiré / déjà recruté → <strong>404 Not Found</strong></li>
 * </ul>
 *
 * Les autres violations métier roster (nom/âge/taille/génétique invalides) retombent sur le global → 400.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RosterApiExceptionHandler {

    @ExceptionHandler(MirrorAlreadyExistsException.class)
    ResponseEntity<ApiError> handleMirrorAlreadyExists(MirrorAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(ScoutedCandidateNotUsableException.class)
    ResponseEntity<ApiError> handleScoutedCandidateNotUsable(ScoutedCandidateNotUsableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    record ApiError(String error) {
    }
}
