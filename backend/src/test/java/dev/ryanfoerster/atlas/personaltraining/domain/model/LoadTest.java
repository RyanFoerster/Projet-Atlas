package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/** Le sealed Load auto-validant (sprint 6, ADR-035). */
class LoadTest {

    @Test
    void bodyweight_carries_no_external_weight() {
        assertThat(Load.bodyweight().externalWeight()).isEmpty();
        assertThat(Load.bodyweight()).isEqualTo(Load.BODYWEIGHT); // singleton stateless
    }

    @Test
    void weighted_exposes_the_added_weight() {
        assertThat(Load.weighted(Weight.ofKilograms(40)).externalWeight())
                .contains(Weight.ofKilograms(40));
    }

    @Test
    void external_exposes_the_external_weight() {
        assertThat(Load.external(Weight.ofKilograms(140)).externalWeight())
                .contains(Weight.ofKilograms(140));
    }

    @Test
    void loaded_variants_reject_a_null_weight() {
        assertThatNullPointerException().isThrownBy(() -> Load.weighted(null));
        assertThatNullPointerException().isThrownBy(() -> Load.external(null));
    }
}
