package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.InvalidEmailException;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.EmailSender;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.MagicLinkJpaEntity;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.MagicLinkJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests d'intégration de {@link RequestMagicLinkUseCase} sur Testcontainers. Le port
 * {@link EmailSender} est remplacé par un <em>fake capturant</em> (pas Mockito — on préfère un
 * vrai stub lisible, CLAUDE.md §5) pour vérifier ce qui est envoyé.
 */
@Import(RequestMagicLinkUseCaseIntegrationTest.CapturingEmailSenderConfig.class)
class RequestMagicLinkUseCaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RequestMagicLinkUseCase useCase;

    @Autowired
    private MagicLinkJpaRepository magicLinkJpaRepository;

    @Autowired
    private CapturingEmailSender emailSender;

    @BeforeEach
    void cleanUp() {
        emailSender.sent.clear();
    }

    @Test
    void issues_persists_and_sends_a_magic_link() {
        useCase.request(new RequestMagicLinkCommand("Ryan@Example.com", "127.0.0.1", "JUnit"));

        List<MagicLinkJpaEntity> links = magicLinkJpaRepository.findAll();
        assertThat(links).hasSize(1);
        MagicLinkJpaEntity link = links.getFirst();
        assertThat(link.getUserEmail()).isEqualTo("ryan@example.com"); // normalisé
        assertThat(link.getConsumedAt()).isNull();
        assertThat(link.getExpiresAt()).isAfter(link.getCreatedAt());

        assertThat(emailSender.sent).hasSize(1);
        Sent sent = emailSender.sent.getFirst();
        assertThat(sent.recipient().value()).isEqualTo("ryan@example.com");
        assertThat(sent.token().value()).isEqualTo(link.getToken()); // le jeton envoyé == le jeton stocké
    }

    @Test
    void rejects_an_invalid_email_and_persists_nothing() {
        assertThatExceptionOfType(InvalidEmailException.class)
                .isThrownBy(() -> useCase.request(new RequestMagicLinkCommand("not-an-email", null, null)));

        assertThat(magicLinkJpaRepository.findAll()).isEmpty();
        assertThat(emailSender.sent).isEmpty();
    }

    @Test
    void behaves_identically_regardless_of_account_existence() {
        // Anti-énumération : la demande ne consulte pas les users, donc même comportement
        // qu'il existe un compte ou non. On vérifie juste qu'aucune erreur ne fuit l'info.
        useCase.request(new RequestMagicLinkCommand("known@example.com", null, null));
        useCase.request(new RequestMagicLinkCommand("unknown@example.com", null, null));

        assertThat(magicLinkJpaRepository.findAll()).hasSize(2);
        assertThat(emailSender.sent).hasSize(2);
    }

    record Sent(Email recipient, MagicLinkToken token) {
    }

    static class CapturingEmailSender implements EmailSender {
        final List<Sent> sent = new CopyOnWriteArrayList<>();

        @Override
        public void sendMagicLink(Email recipient, MagicLinkToken token) {
            sent.add(new Sent(recipient, token));
        }
    }

    @TestConfiguration
    static class CapturingEmailSenderConfig {
        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }
}
