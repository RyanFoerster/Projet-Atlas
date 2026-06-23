package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.api.events.PlayerLoggedIn;
import dev.ryanfoerster.atlas.identity.api.events.PlayerRegistered;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.EmailAlreadyRegisteredException;
import dev.ryanfoerster.atlas.identity.domain.model.InvalidDisplayNameException;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@RecordApplicationEvents
class CompleteSignupUseCaseIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId TZ = ZoneId.of("Europe/Brussels");

    @Autowired
    private CompleteSignupUseCase useCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void cleanUp() {
        userJpaRepository.deleteAll();
    }

    @Test
    void creates_the_player_logs_them_in_and_publishes_both_events() {
        User created = useCase.completeSignup(
                new CompleteSignupCommand("ryan@example.com", "Ryan", Locale.FRENCH, TZ));

        assertThat(created.displayName()).isEqualTo(DisplayName.of("Ryan"));
        assertThat(created.lastLoginAt()).isPresent();             // login immédiat
        assertThat(userRepository.findByEmail(Email.of("ryan@example.com"))).contains(created);
        assertThat(events.stream(PlayerRegistered.class)).hasSize(1);
        assertThat(events.stream(PlayerLoggedIn.class)).hasSize(1);
    }

    @Test
    void rejects_completion_for_an_already_registered_email() {
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, Instant.now()));

        assertThatExceptionOfType(EmailAlreadyRegisteredException.class).isThrownBy(() ->
                useCase.completeSignup(new CompleteSignupCommand("ryan@example.com", "Ryan2", Locale.FRENCH, TZ)));
    }

    @Test
    void rejects_an_invalid_display_name() {
        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() ->
                useCase.completeSignup(new CompleteSignupCommand("ryan@example.com", "x", Locale.FRENCH, TZ)));
    }
}
