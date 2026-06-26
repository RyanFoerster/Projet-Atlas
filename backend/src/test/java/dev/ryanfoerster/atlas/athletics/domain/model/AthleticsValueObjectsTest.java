package dev.ryanfoerster.atlas.athletics.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Validation auto des value objects d'Athletics (bornes de bas niveau). */
class AthleticsValueObjectsTest {

    private static final Instant NOW = Instant.parse("2026-01-05T08:00:00Z");

    @Test
    void fitness_fatigue_state_rejects_negative_values() {
        assertThatThrownBy(() -> new FitnessFatigueState(-1.0, 0.0, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fitness");
        assertThatThrownBy(() -> new FitnessFatigueState(0.0, -1.0, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("fatigue");
    }

    @Test
    void fitness_fatigue_state_requires_a_timestamp() {
        assertThatThrownBy(() -> new FitnessFatigueState(0.0, 0.0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void initial_state_is_zeroed() {
        FitnessFatigueState initial = FitnessFatigueState.initial(NOW);

        assertThat(initial.fitness()).isZero();
        assertThat(initial.fatigue()).isZero();
        assertThat(initial.lastUpdated()).isEqualTo(NOW);
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
}
