package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseSetException;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ExerciseSetTest {

    @Test
    void accepts_a_loaded_set() {
        ExerciseSet set = new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0));

        assertThat(set.reps()).isEqualTo(5);
        assertThat(set.weightOptional()).contains(Weight.ofKilograms(140));
        assertThat(set.rpeOptional()).contains(RPE.of(8.0));
    }

    @Test
    void accepts_a_bodyweight_set_with_null_weight_and_rpe() {
        ExerciseSet set = new ExerciseSet(12, null, null);

        assertThat(set.weightOptional()).isEmpty();
        assertThat(set.rpeOptional()).isEmpty();
        assertThat(set.volumeKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejects_zero_reps() {
        assertThatExceptionOfType(InvalidExerciseSetException.class)
                .isThrownBy(() -> new ExerciseSet(0, null, null));
    }

    @Test
    void rejects_reps_above_maximum() {
        assertThatExceptionOfType(InvalidExerciseSetException.class)
                .isThrownBy(() -> new ExerciseSet(ExerciseSet.MAX_REPS + 1, null, null));
    }

    @Test
    void accepts_the_rep_boundaries() {
        assertThat(new ExerciseSet(ExerciseSet.MIN_REPS, null, null).reps()).isEqualTo(1);
        assertThat(new ExerciseSet(ExerciseSet.MAX_REPS, null, null).reps()).isEqualTo(100);
    }

    @Test
    void volume_is_reps_times_weight() {
        ExerciseSet set = new ExerciseSet(5, Weight.ofKilograms(100), null);

        assertThat(set.volumeKg()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }
}
