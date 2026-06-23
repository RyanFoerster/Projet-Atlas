package dev.ryanfoerster.atlas.identity.infrastructure.email;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Construit l'URL du lien magique et le contenu des emails. Partagé par les deux adapters
 * {@link EmailSender} (log-only en dev, Resend en prod) pour garantir un message identique.
 *
 * <p>Sprint 1 : templates en <strong>texte simple</strong> (lien brut + message court, voix
 * éditoriale Atlas). Les templates HTML riches + preview Mailhog viendront au Sprint 9 (ADR-013).
 *
 * <p>Testable sans Spring : le constructeur prend l'URL de base directement (l'annotation
 * {@code @Value} ne sert qu'au câblage Spring en production).
 */
@Component
public class EmailTemplates {

    private static final String SUBJECT = "Ton lien de connexion à Atlas";

    private final String frontendBaseUrl;

    public EmailTemplates(@Value("${atlas.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /** URL que l'utilisateur clique : la page frontend qui consommera le jeton. */
    public String magicLinkUrl(MagicLinkToken token) {
        return frontendBaseUrl + "/auth/callback?token=" + token.value();
    }

    /** Email de lien magique, en texte simple. */
    public Message magicLink(MagicLinkToken token) {
        String body = """
                Voici ton lien de connexion à Atlas. Il est valable 15 minutes et à usage unique.

                %s

                Si tu n'as pas demandé ce lien, ignore simplement cet email.""".formatted(magicLinkUrl(token));
        return new Message(SUBJECT, body);
    }

    /** Message email prêt à envoyer (sujet + corps texte). */
    public record Message(String subject, String body) {
    }
}
