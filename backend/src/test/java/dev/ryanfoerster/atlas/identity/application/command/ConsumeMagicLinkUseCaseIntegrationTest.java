package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.api.events.PlayerLoggedIn;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.InvalidMagicLinkException;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkNotUsableException;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.MagicLinkJpaRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RecordApplicationEvents
class ConsumeMagicLinkUseCaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConsumeMagicLinkUseCase useCase;

    @Autowired
    private MagicLinkRepository magicLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MagicLinkJpaRepository magicLinkJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ApplicationEvents events;

    private MagicLinkToken issueLink(String email, Instant createdAt, Instant expiresAt) {
        MagicLinkToken token = MagicLinkToken.generate();
        magicLinkRepository.save(MagicLink.issue(token, Email.of(email), createdAt, expiresAt, null, null));
        return token;
    }

    @Test
    void existing_player_logs_in_and_event_is_published() {
        Instant now = Instant.now();
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), now.minus(Duration.ofDays(1))));
        MagicLinkToken token = issueLink("ryan@example.com", now, now.plus(Duration.ofMinutes(15)));

        ConsumeResult result = useCase.consume(token.value().toString());

        assertThat(result.newPlayer()).isFalse();
        assertThat(result.playerIdIfExisting()).isPresent();
        assertThat(userRepository.findByEmail(Email.of("ryan@example.com")).orElseThrow().lastLoginAt())
                .isPresent();
        assertThat(events.stream(PlayerLoggedIn.class)).hasSize(1);
        // lien marqué consommé
        assertThat(magicLinkRepository.findByToken(token).orElseThrow().isConsumed()).isTrue();
    }

    @Test
    void unknown_email_yields_pending_signup_without_creating_a_user() {
        Instant now = Instant.now();
        MagicLinkToken token = issueLink("newbie@example.com", now, now.plus(Duration.ofMinutes(15)));

        ConsumeResult result = useCase.consume(token.value().toString());

        assertThat(result.newPlayer()).isTrue();
        assertThat(result.verifiedEmail()).isEqualTo(Email.of("newbie@example.com"));
        assertThat(result.playerIdIfExisting()).isEmpty();
        assertThat(userJpaRepository.count()).isZero();             // pas de compte créé (flow A)
        assertThat(events.stream(PlayerLoggedIn.class)).isEmpty();  // pas encore connecté
        assertThat(magicLinkRepository.findByToken(token).orElseThrow().isConsumed()).isTrue();
    }

    @Test
    void rejects_an_expired_link() {
        Instant now = Instant.now();
        MagicLinkToken token = issueLink("ryan@example.com",
                now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(5)));

        assertThatExceptionOfType(MagicLinkNotUsableException.class)
                .isThrownBy(() -> useCase.consume(token.value().toString()));
    }

    @Test
    void rejects_a_second_consumption() {
        Instant now = Instant.now();
        MagicLinkToken token = issueLink("ryan@example.com", now, now.plus(Duration.ofMinutes(15)));
        useCase.consume(token.value().toString());

        assertThatExceptionOfType(MagicLinkNotUsableException.class)
                .isThrownBy(() -> useCase.consume(token.value().toString()));
    }

    @Test
    void rejects_an_unknown_token() {
        assertThatExceptionOfType(InvalidMagicLinkException.class)
                .isThrownBy(() -> useCase.consume(MagicLinkToken.generate().value().toString()));
    }

    @Test
    void rejects_a_malformed_token_string() {
        assertThatExceptionOfType(InvalidMagicLinkException.class)
                .isThrownBy(() -> useCase.consume("definitely-not-a-uuid"));
    }
}
