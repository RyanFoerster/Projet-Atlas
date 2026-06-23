package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.ScoutedCandidateNotUsableException;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ScoutedCandidateTest {

    private static final Instant CREATED = Instant.parse("2026-06-23T10:00:00Z");
    private static final Instant EXPIRES = CREATED.plus(Duration.ofHours(1));
    private final AthleteCandidate candidate = new ProceduralAthleteGenerator().generateCandidate(1L, Rarity.GENERIC);

    private ScoutedCandidate issued() {
        return ScoutedCandidate.issue(ScoutedCandidateId.generate(), candidate, CREATED, EXPIRES);
    }

    @Test
    void issue_creates_an_unconsumed_candidate() {
        ScoutedCandidate sc = issued();
        assertThat(sc.isConsumed()).isFalse();
        assertThat(sc.candidate()).isEqualTo(candidate);
        assertThat(sc.canBeConsumed(CREATED.plusSeconds(60))).isTrue();
    }

    @Test
    void issue_rejects_expiry_not_after_creation() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                ScoutedCandidate.issue(ScoutedCandidateId.generate(), candidate, CREATED, CREATED));
    }

    @Test
    void is_expired_at_and_after_expiry() {
        ScoutedCandidate sc = issued();
        assertThat(sc.isExpired(EXPIRES.minusSeconds(1))).isFalse();
        assertThat(sc.isExpired(EXPIRES)).isTrue();
    }

    @Test
    void consume_returns_a_consumed_instance_and_leaves_original_untouched() {
        ScoutedCandidate sc = issued();
        ScoutedCandidate consumed = sc.consume(CREATED.plusSeconds(120));

        assertThat(consumed.isConsumed()).isTrue();
        assertThat(consumed.consumedAt()).contains(CREATED.plusSeconds(120));
        assertThat(sc.isConsumed()).isFalse(); // immutabilité
        assertThat(consumed).isEqualTo(sc);    // même identité
    }

    @Test
    void consume_rejects_expired_then_double_consumption() {
        assertThatExceptionOfType(ScoutedCandidateNotUsableException.class)
                .isThrownBy(() -> issued().consume(EXPIRES.plusSeconds(1)))
                .withMessageContaining("expiré");

        ScoutedCandidate consumed = issued().consume(CREATED.plusSeconds(60));
        assertThatExceptionOfType(ScoutedCandidateNotUsableException.class)
                .isThrownBy(() -> consumed.consume(CREATED.plusSeconds(90)))
                .withMessageContaining("déjà");
    }
}
