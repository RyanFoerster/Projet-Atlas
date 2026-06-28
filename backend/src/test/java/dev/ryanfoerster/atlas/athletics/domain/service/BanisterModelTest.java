package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.MuscleCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * TDD du {@link BanisterModel} : la mécanique mathématique pure (décroissance, stimulus, performance,
 * supercompensation) <strong>par muscle</strong> (sprint 5), et son <strong>individualisation génétique</strong>
 * (Couche 3). Indépendante de la calibration (validée par la simulation longue).
 */
class BanisterModelTest {

    private static final Instant T0 = Instant.parse("2026-01-05T08:00:00Z");
    private static final GeneticModifiers NEUTRAL = GeneticModifiers.NEUTRAL;
    private final BanisterModel model = new BanisterModel();

    private static FitnessFatigueState state(MuscleGroup muscle, double fitness, double fatigue, Instant at) {
        return new FitnessFatigueState(Map.of(muscle, new MuscleCondition(fitness, fatigue)), at);
    }

    private static Map<MuscleGroup, TrainingStimulus> on(MuscleGroup muscle, double magnitude) {
        return Map.of(muscle, new TrainingStimulus(magnitude));
    }

    @Test
    void fitness_decays_by_factor_e_over_one_time_constant() {
        FitnessFatigueState start = state(MuscleGroup.QUADS, 100.0, 0.0, T0);

        FitnessFatigueState after = model.decayedTo(start, NEUTRAL,
                T0.plus(Duration.ofDays((long) BanisterModel.TAU_FITNESS_DAYS)));

        assertThat(after.condition(MuscleGroup.QUADS).fitness()).isCloseTo(100.0 * Math.exp(-1), within(0.5));
    }

    @Test
    void fatigue_decays_faster_than_fitness_for_the_same_elapsed_time() {
        FitnessFatigueState start = state(MuscleGroup.QUADS, 100.0, 100.0, T0);

        MuscleCondition after = model.decayedTo(start, NEUTRAL, T0.plus(Duration.ofDays(7))).condition(MuscleGroup.QUADS);

        assertThat(after.fatigue()).isLessThan(after.fitness());
        assertThat(after.fatigue()).isCloseTo(100.0 * Math.exp(-7.0 / BanisterModel.TAU_FATIGUE_DAYS), within(0.5));
    }

    @Test
    void decay_is_independent_per_muscle() {
        FitnessFatigueState start = new FitnessFatigueState(Map.of(
                MuscleGroup.QUADS, new MuscleCondition(100.0, 0.0),
                MuscleGroup.CHEST, new MuscleCondition(0.0, 50.0)), T0);

        FitnessFatigueState after = model.decayedTo(start, NEUTRAL, T0.plus(Duration.ofDays(7)));

        assertThat(after.condition(MuscleGroup.QUADS).fitness()).isCloseTo(100.0 * Math.exp(-7.0 / 42.0), within(0.5));
        assertThat(after.condition(MuscleGroup.QUADS).fatigue()).isZero();
        assertThat(after.condition(MuscleGroup.CHEST).fatigue()).isCloseTo(50.0 * Math.exp(-1.0), within(0.5));
    }

    @Test
    void no_time_elapsed_leaves_the_state_unchanged() {
        FitnessFatigueState start = state(MuscleGroup.QUADS, 42.0, 17.0, T0);

        MuscleCondition after = model.decayedTo(start, NEUTRAL, T0).condition(MuscleGroup.QUADS);

        assertThat(after.fitness()).isEqualTo(42.0);
        assertThat(after.fatigue()).isEqualTo(17.0);
    }

    @Test
    void applying_a_distributed_stimulus_adds_the_same_magnitude_to_each_targeted_muscle() {
        FitnessFatigueState start = FitnessFatigueState.initial(T0);

        FitnessFatigueState after = model.applyStimulus(start, Map.of(
                MuscleGroup.QUADS, new TrainingStimulus(30.0),
                MuscleGroup.CHEST, new TrainingStimulus(12.0)), NEUTRAL, T0);

        assertThat(after.condition(MuscleGroup.QUADS)).isEqualTo(new MuscleCondition(30.0, 30.0));
        assertThat(after.condition(MuscleGroup.CHEST)).isEqualTo(new MuscleCondition(12.0, 12.0));
    }

    @Test
    void a_stimulus_on_a_new_muscle_decays_existing_ones_and_creates_the_new_one() {
        FitnessFatigueState start = state(MuscleGroup.QUADS, 20.0, 20.0, T0);

        FitnessFatigueState after = model.applyStimulus(start, on(MuscleGroup.BICEPS, 5.0), NEUTRAL,
                T0.plus(Duration.ofDays(7)));

        assertThat(after.condition(MuscleGroup.QUADS).fitness())
                .isGreaterThan(after.condition(MuscleGroup.QUADS).fatigue());
        assertThat(after.condition(MuscleGroup.BICEPS)).isEqualTo(new MuscleCondition(5.0, 5.0));
    }

    @Test
    void aggregate_available_performance_weights_summed_fitness_and_fatigue_with_k1_and_k2() {
        FitnessFatigueState state = new FitnessFatigueState(Map.of(
                MuscleGroup.QUADS, new MuscleCondition(40.0, 8.0),
                MuscleGroup.CHEST, new MuscleCondition(10.0, 2.0)), T0); // Σf=50, Σfat=10

        assertThat(model.availablePerformance(state)).isEqualTo(BanisterModel.K1 * 50.0 - BanisterModel.K2 * 10.0);
    }

    @Test
    void performance_is_negative_right_after_a_big_session_then_supercompensates() {
        FitnessFatigueState justTrained = model.applyStimulus(FitnessFatigueState.initial(T0),
                on(MuscleGroup.QUADS, 100.0), NEUTRAL, T0);
        assertThat(model.availablePerformance(justTrained)).isNegative();

        FitnessFatigueState rested = model.decayedTo(justTrained, NEUTRAL, T0.plus(Duration.ofDays(14)));
        assertThat(model.availablePerformance(rested)).isPositive();
        assertThat(rested.totalFitness()).isGreaterThan(rested.totalFatigue());
    }

    @Test
    void decaying_to_a_past_instant_is_rejected() {
        FitnessFatigueState state = FitnessFatigueState.initial(T0);

        assertThatThrownBy(() -> model.decayedTo(state, NEUTRAL, T0.minus(Duration.ofSeconds(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recule");
    }

    // --- Individualisation génétique (Couche 3) ---

    @Test
    void a_higher_recovery_rate_decays_fatigue_faster_not_fitness() {
        FitnessFatigueState start = state(MuscleGroup.QUADS, 100.0, 100.0, T0);
        GeneticModifiers fastRecovery = new GeneticModifiers(1.20, 1.0);

        MuscleCondition neutral = model.decayedTo(start, NEUTRAL, T0.plus(Duration.ofDays(7))).condition(MuscleGroup.QUADS);
        MuscleCondition fast = model.decayedTo(start, fastRecovery, T0.plus(Duration.ofDays(7))).condition(MuscleGroup.QUADS);

        assertThat(fast.fatigue()).isLessThan(neutral.fatigue());  // fatigue tombe plus vite
        assertThat(fast.fitness()).isEqualTo(neutral.fitness());   // τ_fitness inchangé (court terme)
    }

    @Test
    void a_higher_stimulus_multiplier_deposits_a_bigger_impulse() {
        GeneticModifiers strongResponder = new GeneticModifiers(1.0, 1.15);

        FitnessFatigueState neutral = model.applyStimulus(FitnessFatigueState.initial(T0), on(MuscleGroup.QUADS, 10.0), NEUTRAL, T0);
        FitnessFatigueState strong = model.applyStimulus(FitnessFatigueState.initial(T0), on(MuscleGroup.QUADS, 10.0), strongResponder, T0);

        assertThat(strong.condition(MuscleGroup.QUADS).fitness())
                .isCloseTo(neutral.condition(MuscleGroup.QUADS).fitness() * 1.15, within(1e-9));
    }
}
