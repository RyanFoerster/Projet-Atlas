package dev.ryanfoerster.atlas.roster.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RosterIdTest {

    @Test
    void generate_produces_a_v7_uuid() {
        assertThat(RosterId.generate().value().version()).isEqualTo(7);
    }

    @Test
    void from_round_trips_and_rejects_invalid() {
        RosterId id = RosterId.generate();
        assertThat(RosterId.from(id.value().toString())).isEqualTo(id);
        assertThat(id.toString()).isEqualTo(id.value().toString());
        assertThatIllegalArgumentException().isThrownBy(() -> RosterId.from("nope"));
        assertThatIllegalArgumentException().isThrownBy(() -> RosterId.from(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new RosterId(null));
    }

    @Test
    void equality_is_by_value() {
        UUID raw = RosterId.generate().value();
        assertThat(new RosterId(raw)).isEqualTo(new RosterId(raw));
    }
}
