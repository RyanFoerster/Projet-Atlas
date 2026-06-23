package dev.ryanfoerster.atlas.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class WeightTest {

    @Test
    void of_kilograms_builds_a_kg_weight() {
        Weight w = Weight.ofKilograms(80);

        assertThat(w.unit()).isEqualTo(Weight.Unit.KG);
        assertThat(w.value()).isEqualByComparingTo("80");
        assertThat(w.toKilograms()).isEqualByComparingTo("80");
    }

    @Test
    void converts_pounds_to_kilograms() {
        Weight w = new Weight(new BigDecimal("100"), Weight.Unit.LB);

        assertThat(w.toKilograms()).isEqualByComparingTo("45.359237");
    }

    @Test
    void rejects_a_negative_weight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Weight(new BigDecimal("-1"), Weight.Unit.KG));
    }

    @Test
    void rejects_null_value_or_unit() {
        // requireNonNull → NullPointerException (erreur technique, pas métier)
        assertThatNullPointerException().isThrownBy(() -> new Weight(null, Weight.Unit.KG));
        assertThatNullPointerException().isThrownBy(() -> new Weight(BigDecimal.ONE, null));
    }

    @Test
    void equality_is_by_value() {
        assertThat(Weight.ofKilograms(80)).isEqualTo(Weight.ofKilograms(80));
    }
}
