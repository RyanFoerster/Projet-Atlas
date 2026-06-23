package dev.ryanfoerster.atlas.identity.application.command;

/**
 * Commande de demande de lien magique. {@code email} est brut (validé par le use case via
 * {@code Email.of}). {@code ipAddress}/{@code userAgent} sont du contexte de sécurité optionnel
 * capturé par la couche web ({@code null} accepté).
 */
public record RequestMagicLinkCommand(String email, String ipAddress, String userAgent) {
}
