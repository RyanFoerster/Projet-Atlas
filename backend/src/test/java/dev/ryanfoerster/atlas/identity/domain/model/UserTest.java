package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTest {

    private static final Email EMAIL = Email.of("ryan@example.com");
    private static final DisplayName NAME = DisplayName.of("Ryan");
    private static final Locale LOCALE = Locale.FRENCH;
    private static final ZoneId TZ = ZoneId.of("Europe/Brussels");
    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00Z");

    @Test
    void register_creates_a_player_with_a_v7_id_and_no_login_yet() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);

        assertThat(user.id().value().version()).isEqualTo(7);
        assertThat(user.email()).isEqualTo(EMAIL);
        assertThat(user.displayName()).isEqualTo(NAME);
        assertThat(user.locale()).isEqualTo(LOCALE);
        assertThat(user.timezone()).isEqualTo(TZ);
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.lastLoginAt()).isEmpty();
    }

    @Test
    void register_generates_distinct_ids() {
        User a = User.register(EMAIL, NAME, LOCALE, TZ, NOW);
        User b = User.register(EMAIL, NAME, LOCALE, TZ, NOW);

        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void recordLogin_returns_a_new_instance_with_lastLogin_and_leaves_original_untouched() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);
        Instant loginTime = NOW.plusSeconds(3600);

        User loggedIn = user.recordLogin(loginTime);

        assertThat(loggedIn.lastLoginAt()).contains(loginTime);
        assertThat(loggedIn.id()).isEqualTo(user.id());     // même identité
        assertThat(user.lastLoginAt()).isEmpty();           // immutabilité : l'original n'a pas bougé
        assertThat(loggedIn).isNotSameAs(user);
    }

    @Test
    void recordLogin_rejects_a_time_before_creation() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);

        assertThatIllegalArgumentException().isThrownBy(() -> user.recordLogin(NOW.minusSeconds(1)));
    }

    @Test
    void updateDisplayName_returns_a_new_instance_and_keeps_the_rest() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);
        DisplayName newName = DisplayName.of("Ryan Coach");

        User updated = user.updateDisplayName(newName);

        assertThat(updated.displayName()).isEqualTo(newName);
        assertThat(updated.id()).isEqualTo(user.id());
        assertThat(updated.email()).isEqualTo(user.email());
        assertThat(user.displayName()).isEqualTo(NAME); // original inchangé
    }

    @Test
    void updateLocale_returns_a_new_instance() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);

        User updated = user.updateLocale(Locale.ENGLISH);

        assertThat(updated.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(user.locale()).isEqualTo(LOCALE);
    }

    @Test
    void equality_is_by_identity_not_by_state() {
        User user = User.register(EMAIL, NAME, LOCALE, TZ, NOW);
        User afterLogin = user.recordLogin(NOW.plusSeconds(60));

        // même id, état différent → mêmes au sens de l'aggregate
        assertThat(afterLogin).isEqualTo(user);
        assertThat(afterLogin).hasSameHashCodeAs(user);
    }

    @Test
    void two_distinct_players_are_not_equal() {
        User a = User.register(EMAIL, NAME, LOCALE, TZ, NOW);
        User b = User.register(Email.of("other@example.com"), NAME, LOCALE, TZ, NOW);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void register_rejects_null_required_fields() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> User.register(null, NAME, LOCALE, TZ, NOW));
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> User.register(EMAIL, NAME, LOCALE, TZ, null));
    }
}
