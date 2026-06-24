package dev.ryanfoerster.atlas.personaltraining.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WorkoutSessionIdTest {

    @Test
    void generate_produces_a_non_null_id() {
        assertThat(WorkoutSessionId.generate().value()).isNotNull();
    }

    @Test
    void from_parses_a_valid_uuid_string() {
        UUID uuid = UUID.randomUUID();
        assertThat(WorkoutSessionId.from(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void from_rejects_a_malformed_string() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WorkoutSessionId.from("not-a-uuid"));
    }

    @Test
    void from_rejects_null() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> WorkoutSessionId.from(null));
    }

    @Test
    void rejects_a_null_uuid() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new WorkoutSessionId(null));
    }

    @Test
    void toString_is_the_uuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(WorkoutSessionId.from(uuid.toString())).hasToString(uuid.toString());
    }
}
