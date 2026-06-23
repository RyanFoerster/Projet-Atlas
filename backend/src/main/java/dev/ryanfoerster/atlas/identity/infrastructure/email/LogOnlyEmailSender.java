package dev.ryanfoerster.atlas.identity.infrastructure.email;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implémentation dev/test du port {@link EmailSender} : n'envoie aucun email, journalise le lien
 * complet pour qu'il soit cliquable depuis les logs. Active hors production ({@code @Profile("!prod")}),
 * où {@link ResendEmailSender} prend le relais. Voir ADR-013.
 */
@Component
@Profile("!prod")
public class LogOnlyEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogOnlyEmailSender.class);

    private final EmailTemplates templates;

    public LogOnlyEmailSender(EmailTemplates templates) {
        this.templates = templates;
    }

    @Override
    public void sendMagicLink(Email recipient, MagicLinkToken token) {
        log.info("[DEV] Email NON envoyé à {} — lien magique : {}",
                recipient.value(), templates.magicLinkUrl(token));
    }
}
