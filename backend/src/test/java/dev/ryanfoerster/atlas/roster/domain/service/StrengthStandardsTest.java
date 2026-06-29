package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Source unique des standards de force (ADR-033, T3 du sprint 6). On vérifie les bornes de bande, le
 * facteur féminin et l'absence de standard pour ROW/CHIN_UP — la propriété qui fait que ces patterns ne
 * progressent pas structurellement.
 */
class StrengthStandardsTest {

    @Test
    void male_squat_band_is_intermediate_to_elite() {
        assertThat(StrengthStandards.intermediateRatio(MovementPattern.SQUAT, Gender.MALE)).isEqualTo(1.5);
        assertThat(StrengthStandards.eliteRatio(MovementPattern.SQUAT, Gender.MALE)).isEqualTo(2.3);
    }

    @Test
    void female_factors_scale_upper_and_lower_body_differently() {
        // Bas du corps (squat) : ×0.75 ; haut du corps (bench) : ×0.65.
        assertThat(StrengthStandards.eliteRatio(MovementPattern.SQUAT, Gender.FEMALE)).isCloseTo(2.3 * 0.75, within(1e-9));
        assertThat(StrengthStandards.eliteRatio(MovementPattern.BENCH_PRESS, Gender.FEMALE)).isCloseTo(1.65 * 0.65, within(1e-9));
        assertThat(StrengthStandards.eliteRatio(MovementPattern.OVERHEAD_PRESS, Gender.FEMALE)).isCloseTo(1.0 * 0.65, within(1e-9));
        assertThat(StrengthStandards.eliteRatio(MovementPattern.DEADLIFT, Gender.FEMALE)).isCloseTo(2.7 * 0.75, within(1e-9));
    }

    @Test
    void the_four_big_lifts_have_a_standard_but_row_and_chin_up_do_not() {
        assertThat(StrengthStandards.hasStandard(MovementPattern.SQUAT)).isTrue();
        assertThat(StrengthStandards.hasStandard(MovementPattern.BENCH_PRESS)).isTrue();
        assertThat(StrengthStandards.hasStandard(MovementPattern.DEADLIFT)).isTrue();
        assertThat(StrengthStandards.hasStandard(MovementPattern.OVERHEAD_PRESS)).isTrue();
        assertThat(StrengthStandards.hasStandard(MovementPattern.ROW)).isFalse();
        assertThat(StrengthStandards.hasStandard(MovementPattern.CHIN_UP)).isFalse();
    }

    @Test
    void asking_a_band_for_a_pattern_without_a_standard_throws() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StrengthStandards.ratioBand(MovementPattern.ROW, Gender.MALE));
    }
}
