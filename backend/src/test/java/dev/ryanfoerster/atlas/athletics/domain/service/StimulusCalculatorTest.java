package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests de la formule de stimulus : {@code S = NORM × Σ (reps × effort(rpe))}, effort linéaire {@code rpe/10},
 * RPE absent = effort neutre 0.7.
 */
class StimulusCalculatorTest {

    private final StimulusCalculator calculator = new StimulusCalculator();

    @Test
    void effort_factor_is_linear_in_rpe() {
        assertThat(StimulusCalculator.effortFactor(8.0)).isEqualTo(0.8);
        assertThat(StimulusCalculator.effortFactor(10.0)).isEqualTo(1.0);
        assertThat(StimulusCalculator.effortFactor(6.0)).isEqualTo(0.6);
    }

    @Test
    void missing_rpe_uses_the_neutral_default_effort() {
        assertThat(StimulusCalculator.effortFactor(null)).isEqualTo(StimulusCalculator.DEFAULT_EFFORT_RPE / 10.0);
        assertThat(StimulusCalculator.effortFactor(null)).isEqualTo(0.7);
    }

    @Test
    void magnitude_is_the_normalized_sum_of_reps_times_effort() {
        // 5 reps @ RPE 8 (effort 0.8) = 4.0 ; 5 reps sans RPE (effort 0.7) = 3.5 ; somme = 7.5.
        List<SetEffort> sets = List.of(new SetEffort(5, 8.0), new SetEffort(5, null));

        TrainingStimulus stimulus = calculator.from(sets);

        assertThat(stimulus.magnitude()).isCloseTo(StimulusCalculator.NORMALIZATION * 7.5, within(1e-9));
    }

    @Test
    void the_double_sum_already_counts_each_set_no_separate_set_factor() {
        // 3 séries de 5 reps @ RPE 8 = 3 termes de (5 × 0.8) = 12.0, pas un facteur « séries » en plus.
        List<SetEffort> threeSets = List.of(
                new SetEffort(5, 8.0), new SetEffort(5, 8.0), new SetEffort(5, 8.0));

        TrainingStimulus stimulus = calculator.from(threeSets);

        assertThat(stimulus.magnitude()).isCloseTo(StimulusCalculator.NORMALIZATION * 12.0, within(1e-9));
    }

    @Test
    void an_empty_session_yields_no_stimulus() {
        assertThat(calculator.from(List.of()).magnitude()).isEqualTo(0.0);
    }
}
