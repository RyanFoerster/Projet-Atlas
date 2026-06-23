package dev.ryanfoerster.atlas.identity.infrastructure.email;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implémentation production du port {@link EmailSender} : envoie réellement l'email via l'API
 * REST de Resend (ADR-013). Active uniquement en {@code prod} ; en dev/test c'est
 * {@link LogOnlyEmailSender}.
 *
 * <p>Utilise le {@link RestClient} de Spring (aucune dépendance ajoutée). Clé d'API et expéditeur
 * lus en configuration ({@code atlas.email.*}, injectés par variables d'environnement en prod).
 *
 * <p>Non exercé en dev/test (gated {@code prod}) : sa validation réelle se fera au premier
 * déploiement.
 */
@Component
@Profile("prod")
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final EmailTemplates templates;
    private final String from;
    private final RestClient restClient;

    public ResendEmailSender(EmailTemplates templates,
                             @Value("${atlas.email.from}") String from,
                             @Value("${atlas.email.resend.api-key}") String apiKey) {
        this.templates = templates;
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl(RESEND_ENDPOINT)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendMagicLink(Email recipient, MagicLinkToken token) {
        EmailTemplates.Message message = templates.magicLink(token);
        ResendPayload payload = new ResendPayload(
                from, new String[] {recipient.value()}, message.subject(), message.body());

        restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    /** Corps JSON attendu par l'API Resend (clés : from, to, subject, text). */
    private record ResendPayload(String from, String[] to, String subject, String text) {
    }
}
