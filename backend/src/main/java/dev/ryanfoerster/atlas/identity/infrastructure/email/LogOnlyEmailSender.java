package dev.ryanfoerster.atlas.identity.infrastructure.email;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut (dev) du port {@link EmailSender} : n'envoie rien, journalise le
 * lien. Suffit pour le développement local et les tests manuels (le lien est cliquable depuis
 * les logs).
 *
 * <p>Implémentation minimale posée en S4 pour que le contexte démarre. La stratégie complète
 * (impl Resend en prod, templates HTML, sélection par profil, ADR-013) est livrée en S5.
 */
@Component
public class LogOnlyEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogOnlyEmailSender.class);

    @Override
    public void sendMagicLink(Email recipient, MagicLinkToken token) {
        log.info("[DEV] Lien magique pour {} -> token={}", recipient.value(), token.value());
    }
}
