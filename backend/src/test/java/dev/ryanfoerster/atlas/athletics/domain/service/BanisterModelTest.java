package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * TDD du {@link BanisterModel} : la mécanique mathématique pure (décroissance, stimulus, performance,
 * supercompensation), indépendante de la calibration (qui est validée par la simulation longue).
 */
class BanisterModelTest {

    private static final Instant T0 = Instant.parse("2026-01-05T08:00:00Z");
    private final BanisterModel model = new BanisterModel();

    @Test
    void fitness_decays_by_factor_e_over_one_time_constant() {
        FitnessFatigueState start = new FitnessFatigueState(100.0, 0.0, T0);

        FitnessFatigueState after = model.decayedTo(start, T0.plus(Duration.ofDays((long) BanisterModel.TAU_FITNESS_DAYS)));

        // exp(-1) ≈ 0.3679 : après une constante de temps, il reste ~36,8 % de la fitness.
        assertThat(after.fitness()).isCloseTo(100.0 * Math.exp(-1), within(0.5));
    }

    @Test
    void fatigue_decays_faster_than_fitness_for_the_same_elapsed_time() {
        FitnessFatigueState start = new FitnessFatigueState(100.0, 100.0, T0);

        FitnessFatigueState after = model.decayedTo(start, T0.plus(Duration.ofDays(7)));

        // τ_fatigue (7j) ≪ τ_fitness (42j) → la fatigue tombe bien plus vite.
        assertThat(after.fatigue()).isLessThan(after.fitness());
        assertThat(after.fatigue()).isCloseTo(100.0 * Math.exp(-7.0 / BanisterModel.TAU_FATIGUE_DAYS), within(0.5));
    }

    @Test
    void no_time_elapsed_leaves_the_state_unchanged() {
        FitnessFatigueState start = new FitnessFatigueState(42.0, 17.0, T0);

        FitnessFatigueState after = model.decayedTo(start, T0);

        assertThat(after.fitness()).isEqualTo(42.0);
        assertThat(after.fatigue()).isEqualTo(17.0);
    }

    @Test
    void applying_a_stimulus_adds_the_same_magnitude_to_fitness_and_fatigue() {
        FitnessFatigueState start = FitnessFatigueState.initial(T0);

        FitnessFatigueState after = model.applyStimulus(start, new TrainingStimulus(30.0), T0);

        assertThat(after.fitness()).isEqualTo(30.0);
        assertThat(after.fatigue()).isEqualTo(30.0);
    }

    @Test
    void available_performance_weights_fitness_and_fatigue_with_k1_and_k2() {
        FitnessFatigueState state = new FitnessFatigueState(50.0, 10.0, T0);

        double performance = model.availablePerformance(state);

        assertThat(performance).isEqualTo(BanisterModel.K1 * 50.0 - BanisterModel.K2 * 10.0);
    }

    @Test
    void performance_is_negative_right_after_a_big_session_then_supercompensates() {
        // Athlète frais qui encaisse une grosse séance : fitness = fatigue → perf = (k1−k2)·S < 0 (« cuit »).
        FitnessFatigueState justTrained = model.applyStimulus(FitnessFatigueState.initial(T0), new TrainingStimulus(100.0), T0);
        assertThat(model.availablePerformance(justTrained)).isNegative();

        // Deux semaines de repos : la fatigue (τ7) s'efface bien plus que la fitness (τ42) → perf repasse positive.
        FitnessFatigueState rested = model.decayedTo(justTrained, T0.plus(Duration.ofDays(14)));
        assertThat(model.availablePerformance(rested)).isPositive();
        assertThat(rested.fitness()).isGreaterThan(rested.fatigue());
    }

    @Test
    void decaying_to_a_past_instant_is_rejected() {
        FitnessFatigueState state = FitnessFatigueState.initial(T0);

        assertThatThrownBy(() -> model.decayedTo(state, T0.minus(Duration.ofSeconds(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recule");
    }
}
