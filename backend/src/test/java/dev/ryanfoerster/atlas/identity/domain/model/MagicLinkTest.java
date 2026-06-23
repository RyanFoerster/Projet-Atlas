package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MagicLinkTest {

    private static final Email EMAIL = Email.of("ryan@example.com");
    private static final Instant CREATED = Instant.parse("2026-06-23T10:00:00Z");
    private static final Instant EXPIRES = CREATED.plus(Duration.ofMinutes(15));

    private static MagicLink issued() {
        return MagicLink.issue(MagicLinkToken.generate(), EMAIL, CREATED, EXPIRES, "127.0.0.1", "JUnit");
    }

    @Test
    void issue_creates_an_unconsumed_link() {
        MagicLink link = issued();

        assertThat(link.userEmail()).isEqualTo(EMAIL);
        assertThat(link.expiresAt()).isEqualTo(EXPIRES);
        assertThat(link.isConsumed()).isFalse();
        assertThat(link.consumedAt()).isEmpty();
        assertThat(link.ipAddress()).contains("127.0.0.1");
        assertThat(link.userAgent()).contains("JUnit");
    }

    @Test
    void issue_rejects_expiry_not_after_creation() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                MagicLink.issue(MagicLinkToken.generate(), EMAIL, CREATED, CREATED, null, null));
    }

    @Test
    void issue_accepts_null_security_context() {
        MagicLink link = MagicLink.issue(MagicLinkToken.generate(), EMAIL, CREATED, EXPIRES, null, null);

        assertThat(link.ipAddress()).isEmpty();
        assertThat(link.userAgent()).isEmpty();
    }

    @Test
    void isExpired_is_true_at_and_after_expiry() {
        MagicLink link = issued();

        assertThat(link.isExpired(EXPIRES.minusSeconds(1))).isFalse();
        assertThat(link.isExpired(EXPIRES)).isTrue();          // borne : expiré dès l'instant pile
        assertThat(link.isExpired(EXPIRES.plusSeconds(1))).isTrue();
    }

    @Test
    void canBeConsumed_within_the_window() {
        MagicLink link = issued();

        assertThat(link.canBeConsumed(CREATED.plusSeconds(60))).isTrue();
        assertThat(link.canBeConsumed(EXPIRES)).isFalse(); // expiré
    }

    @Test
    void consume_returns_a_new_consumed_instance_and_leaves_original_untouched() {
        MagicLink link = issued();
        Instant when = CREATED.plusSeconds(120);

        MagicLink consumed = link.consume(when);

        assertThat(consumed.isConsumed()).isTrue();
        assertThat(consumed.consumedAt()).contains(when);
        assertThat(consumed.token()).isEqualTo(link.token()); // même identité
        assertThat(link.isConsumed()).isFalse();              // immutabilité
    }

    @Test
    void consume_rejects_an_expired_link() {
        MagicLink link = issued();

        assertThatExceptionOfType(MagicLinkNotUsableException.class)
                .isThrownBy(() -> link.consume(EXPIRES.plusSeconds(1)))
                .withMessageContaining("expiré");
    }

    @Test
    void consume_rejects_double_consumption() {
        MagicLink consumed = issued().consume(CREATED.plusSeconds(60));

        assertThatExceptionOfType(MagicLinkNotUsableException.class)
                .isThrownBy(() -> consumed.consume(CREATED.plusSeconds(90)))
                .withMessageContaining("déjà");
    }

    @Test
    void equality_is_by_token_identity() {
        MagicLinkToken token = MagicLinkToken.generate();
        MagicLink a = MagicLink.issue(token, EMAIL, CREATED, EXPIRES, null, null);
        MagicLink b = MagicLink.issue(token, EMAIL, CREATED, EXPIRES, "x", "y").consume(CREATED.plusSeconds(10));

        // même token, états différents → même entity
        assertThat(b).isEqualTo(a);
        assertThat(b).hasSameHashCodeAs(a);
    }
}
