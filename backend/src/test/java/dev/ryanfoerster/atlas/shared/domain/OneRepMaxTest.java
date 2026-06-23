package dev.ryanfoerster.atlas.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class OneRepMaxTest {

    @Test
    void measured_and_estimated_carry_their_source() {
        Weight w = Weight.ofKilograms(140);

        assertThat(OneRepMax.measured(w).source()).isEqualTo(OneRepMax.Source.MEASURED);
        assertThat(OneRepMax.estimated(w).source()).isEqualTo(OneRepMax.Source.ESTIMATED);
        assertThat(OneRepMax.measured(w).weight()).isEqualTo(w);
    }

    @Test
    void rejects_null_weight_or_source() {
        assertThatNullPointerException().isThrownBy(() -> OneRepMax.measured(null));
        assertThatNullPointerException().isThrownBy(() -> new OneRepMax(Weight.ofKilograms(100), null));
    }

    @Test
    void equality_is_by_value() {
        Weight w = Weight.ofKilograms(140);

        assertThat(OneRepMax.measured(w)).isEqualTo(OneRepMax.measured(w));
        assertThat(OneRepMax.measured(w)).isNotEqualTo(OneRepMax.estimated(w));
    }
}
