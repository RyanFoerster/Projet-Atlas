package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.util.Optional;

/**
 * Résultat de la consommation d'un lien magique, dans l'optique du <strong>signup flow A</strong>
 * (page d'onboarding pour les nouveaux) :
 * <ul>
 *   <li><b>Player existant</b> → {@code newPlayer = false}, {@code playerId} présent : la couche
 *       web établit la session et redirige vers l'accueil.</li>
 *   <li><b>Nouvel email vérifié</b> → {@code newPlayer = true}, {@code playerId} absent : l'email
 *       est prouvé mais le compte n'est pas encore créé. La couche web ouvre une session
 *       temporaire et redirige vers l'onboarding pour saisir le nom (→ {@code CompleteSignupUseCase}).</li>
 * </ul>
 */
public record ConsumeResult(boolean newPlayer, Email verifiedEmail, UserId playerId) {

    public static ConsumeResult existingPlayer(UserId playerId, Email email) {
        return new ConsumeResult(false, email, playerId);
    }

    public static ConsumeResult pendingSignup(Email email) {
        return new ConsumeResult(true, email, null);
    }

    public Optional<UserId> playerIdIfExisting() {
        return Optional.ofNullable(playerId);
    }
}
