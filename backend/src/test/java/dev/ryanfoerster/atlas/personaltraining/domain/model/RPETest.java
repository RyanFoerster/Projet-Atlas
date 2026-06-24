package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidRPEException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RPETest {

    @Test
    void accepts_a_half_increment() {
        assertThat(RPE.of(7.5).value()).isEqualTo(7.5);
    }

    @Test
    void accepts_the_boundaries() {
        assertThat(RPE.of(1.0).value()).isEqualTo(1.0);
        assertThat(RPE.of(10.0).value()).isEqualTo(10.0);
    }

    @Test
    void rejects_below_minimum() {
        assertThatExceptionOfType(InvalidRPEException.class).isThrownBy(() -> RPE.of(0.5));
    }

    @Test
    void rejects_above_maximum() {
        assertThatExceptionOfType(InvalidRPEException.class).isThrownBy(() -> RPE.of(10.5));
    }

    @Test
    void rejects_a_non_half_increment() {
        assertThatExceptionOfType(InvalidRPEException.class).isThrownBy(() -> RPE.of(7.3));
    }

    @Test
    void equality_is_by_value() {
        assertThat(RPE.of(8.0)).isEqualTo(RPE.of(8.0));
    }
}
