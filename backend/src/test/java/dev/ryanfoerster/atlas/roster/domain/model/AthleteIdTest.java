package dev.ryanfoerster.atlas.roster.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AthleteIdTest {

    @Test
    void generate_produces_a_v7_uuid() {
        assertThat(AthleteId.generate().value().version()).isEqualTo(7);
    }

    @Test
    void from_round_trips_and_rejects_invalid() {
        AthleteId id = AthleteId.generate();
        assertThat(AthleteId.from(id.value().toString())).isEqualTo(id);
        assertThat(id.toString()).isEqualTo(id.value().toString());
        assertThatIllegalArgumentException().isThrownBy(() -> AthleteId.from("nope"));
        assertThatIllegalArgumentException().isThrownBy(() -> AthleteId.from(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new AthleteId(null));
    }

    @Test
    void equality_is_by_value() {
        UUID raw = AthleteId.generate().value();
        assertThat(new AthleteId(raw)).isEqualTo(new AthleteId(raw));
    }
}
