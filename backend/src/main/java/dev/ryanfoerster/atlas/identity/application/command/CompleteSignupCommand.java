package dev.ryanfoerster.atlas.identity.application.command;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Commande de finalisation d'inscription (flow A, après vérification de l'email).
 *
 * <p>{@code verifiedEmail} provient de la session temporaire ouverte à la consommation du lien :
 * la couche web (S6) garantit qu'il a bien été vérifié. Le use case lui fait confiance — il ne
 * peut pas être fourni librement par le client. {@code locale}/{@code timezone} sont fournis par
 * l'onboarding (détectés navigateur ou choisis).
 */
public record CompleteSignupCommand(String verifiedEmail, String displayName, Locale locale, ZoneId timezone) {
}
