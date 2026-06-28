package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Validation auto des value objects d'Athletics (bornes de bas niveau) + forme par muscle (sprint 5). */
class AthleticsValueObjectsTest {

    private static final Instant NOW = Instant.parse("2026-01-05T08:00:00Z");

    @Test
    void muscle_condition_rejects_negative_values() {
        assertThatThrownBy(() -> new MuscleCondition(-1.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fitness");
        assertThatThrownBy(() -> new MuscleCondition(0.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fatigue");
        assertThat(MuscleCondition.ZERO.fitness()).isZero();
        assertThat(MuscleCondition.ZERO.fatigue()).isZero();
    }

    @Test
    void state_requires_a_timestamp_and_a_map() {
        assertThatThrownBy(() -> new FitnessFatigueState(Map.of(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FitnessFatigueState(null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void initial_state_is_empty_and_aggregates_to_zero() {
        FitnessFatigueState initial = FitnessFatigueState.initial(NOW);

        assertThat(initial.byMuscle()).isEmpty();
        assertThat(initial.totalFitness()).isZero();
        assertThat(initial.totalFatigue()).isZero();
        assertThat(initial.lastUpdated()).isEqualTo(NOW);
    }

    @Test
    void aggregation_sums_present_muscles_and_absent_muscles_read_as_zero() {
        FitnessFatigueState state = new FitnessFatigueState(Map.of(
                MuscleGroup.QUADS, new MuscleCondition(10.0, 4.0),
                MuscleGroup.CHEST, new MuscleCondition(6.0, 1.0)), NOW);

        assertThat(state.totalFitness()).isEqualTo(16.0);
        assertThat(state.totalFatigue()).isEqualTo(5.0);
        assertThat(state.condition(MuscleGroup.QUADS)).isEqualTo(new MuscleCondition(10.0, 4.0));
        assertThat(state.condition(MuscleGroup.BICEPS)).isEqualTo(MuscleCondition.ZERO); // jamais travaillé
    }

    @Test
    void state_map_is_defensively_copied() {
        var mutable = new java.util.HashMap<MuscleGroup, MuscleCondition>();
        mutable.put(MuscleGroup.QUADS, new MuscleCondition(1.0, 1.0));
        FitnessFatigueState state = new FitnessFatigueState(mutable, NOW);

        mutable.put(MuscleGroup.CHEST, new MuscleCondition(9.0, 9.0)); // ne doit PAS fuiter dans l'état

        assertThat(state.byMuscle()).containsOnlyKeys(MuscleGroup.QUADS);
    }

    @Test
    void training_stimulus_rejects_negative_magnitude_and_exposes_none() {
        assertThatThrownBy(() -> new TrainingStimulus(-0.1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("magnitude");
        assertThat(TrainingStimulus.NONE.magnitude()).isZero();
    }

    @Test
    void set_effort_requires_at_least_one_rep() {
        assertThatThrownBy(() -> new SetEffort(0, 8.0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("rep");
    }

    @Test
    void set_effort_rejects_rpe_out_of_range_but_accepts_null() {
        assertThatThrownBy(() -> new SetEffort(5, 10.5))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("RPE");
        assertThat(new SetEffort(5, null).rpe()).isNull();
    }

    @Test
    void exercise_stimulus_requires_exactly_one_target() {
        List<SetEffort> sets = List.of(new SetEffort(5, 8.0));

        assertThat(ExerciseStimulus.compound(MovementPattern.SQUAT, sets).pattern()).isEqualTo(MovementPattern.SQUAT);
        assertThat(ExerciseStimulus.accessory(BodyRegion.BICEPS, sets).accessoryRegion()).isEqualTo(BodyRegion.BICEPS);
        // ni les deux, ni aucun
        assertThatThrownBy(() -> new ExerciseStimulus(MovementPattern.SQUAT, BodyRegion.BICEPS, sets))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExerciseStimulus(null, null, sets))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
