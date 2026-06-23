package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserIdTest {

    @Test
    void generate_produces_a_version_7_uuid() {
        UserId id = UserId.generate();

        assertThat(id.value()).isNotNull();
        assertThat(id.value().version()).isEqualTo(7);
    }

    @Test
    void generate_produces_unique_and_time_ordered_ids() {
        UserId previous = UserId.generate();
        Set<UUID> seen = new HashSet<>();
        seen.add(previous.value());

        for (int i = 0; i < 1_000; i++) {
            UserId current = UserId.generate();
            // unicité
            assertThat(seen.add(current.value())).isTrue();
            // ordonnancement temporel : un v7 généré après n'est jamais < au précédent
            assertThat(current.value().toString()).isGreaterThanOrEqualTo(previous.value().toString());
            previous = current;
        }
    }

    @Test
    void from_parses_a_valid_uuid_string_round_trip() {
        UserId original = UserId.generate();

        UserId parsed = UserId.from(original.value().toString());

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void from_rejects_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> UserId.from(null));
    }

    @Test
    void from_rejects_malformed_string() {
        assertThatIllegalArgumentException().isThrownBy(() -> UserId.from("pas-un-uuid"));
    }

    @Test
    void constructor_rejects_null_uuid() {
        assertThatIllegalArgumentException().isThrownBy(() -> new UserId(null));
    }

    @Test
    void equality_is_by_value() {
        UUID raw = UserId.generate().value();

        assertThat(new UserId(raw)).isEqualTo(new UserId(raw));
        assertThat(new UserId(raw)).hasSameHashCodeAs(new UserId(raw));
    }

    @Test
    void toString_exposes_the_uuid() {
        UserId id = UserId.generate();

        assertThat(id.toString()).isEqualTo(id.value().toString());
    }
}
