package dev.ryanfoerster.atlas.identity.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MagicLinkExpirationPolicyTest {

    private static final Instant ISSUED = Instant.parse("2026-06-23T10:00:00Z");

    @Test
    void default_ttl_is_15_minutes() {
        assertThat(new MagicLinkExpirationPolicy().ttl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void expiresAt_is_issued_plus_ttl() {
        MagicLinkExpirationPolicy policy = new MagicLinkExpirationPolicy();

        assertThat(policy.expiresAt(ISSUED)).isEqualTo(ISSUED.plus(Duration.ofMinutes(15)));
    }

    @Test
    void honours_a_custom_ttl() {
        MagicLinkExpirationPolicy policy = new MagicLinkExpirationPolicy(Duration.ofMinutes(5));

        assertThat(policy.expiresAt(ISSUED)).isEqualTo(ISSUED.plus(Duration.ofMinutes(5)));
    }

    @Test
    void rejects_non_positive_ttl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MagicLinkExpirationPolicy(Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new MagicLinkExpirationPolicy(Duration.ofMinutes(-1)));
    }
}
