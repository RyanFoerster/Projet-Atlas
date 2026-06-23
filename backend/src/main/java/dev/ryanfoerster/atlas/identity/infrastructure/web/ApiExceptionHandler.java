package dev.ryanfoerster.atlas.identity.infrastructure.web;

import dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions métier en réponses HTTP. Concrétisation de la distinction posée dans
 * {@code DomainException} : une <strong>violation de règle métier</strong> (email invalide, lien
 * inutilisable, email déjà pris…) devient un <strong>400</strong> avec un message clair destiné à
 * l'utilisateur — jamais un 500 + alerte (réservé aux erreurs techniques, qui ne passent pas par
 * ici et retombent sur le handler par défaut de Spring).
 *
 * <p>Note : ce handler est global. Il vit dans identity pour l'instant (seul module à exposer des
 * controllers) ; il pourra migrer vers une config web partagée quand d'autres modules en exposeront.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handleDomainException(DomainException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage()));
    }

    record ApiError(String error) {
    }
}
