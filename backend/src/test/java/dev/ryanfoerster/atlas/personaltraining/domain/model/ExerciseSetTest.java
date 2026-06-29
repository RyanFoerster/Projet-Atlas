package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseSetException;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ExerciseSetTest {

    @Test
    void accepts_an_external_load_set() {
        ExerciseSet set = ExerciseSet.external(5, Weight.ofKilograms(140), RPE.of(8.0));

        assertThat(set.reps()).isEqualTo(5);
        assertThat(set.load()).isEqualTo(Load.external(Weight.ofKilograms(140)));
        assertThat(set.rpeOptional()).contains(RPE.of(8.0));
    }

    @Test
    void accepts_a_bodyweight_set_with_null_rpe() {
        ExerciseSet set = ExerciseSet.bodyweight(12, null);

        assertThat(set.load()).isEqualTo(Load.bodyweight());
        assertThat(set.rpeOptional()).isEmpty();
        assertThat(set.volumeKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accepts_a_weighted_set_carrying_the_added_load() {
        ExerciseSet set = ExerciseSet.weighted(6, Weight.ofKilograms(40), RPE.of(8.5));

        assertThat(set.load()).isEqualTo(Load.weighted(Weight.ofKilograms(40)));
    }

    @Test
    void rejects_a_null_load() {
        assertThatNullPointerException().isThrownBy(() -> new ExerciseSet(5, null, RPE.of(8.0)));
    }

    @Test
    void rejects_zero_reps() {
        assertThatExceptionOfType(InvalidExerciseSetException.class)
                .isThrownBy(() -> ExerciseSet.bodyweight(0, null));
    }

    @Test
    void rejects_reps_above_maximum() {
        assertThatExceptionOfType(InvalidExerciseSetException.class)
                .isThrownBy(() -> ExerciseSet.bodyweight(ExerciseSet.MAX_REPS + 1, null));
    }

    @Test
    void accepts_the_rep_boundaries() {
        assertThat(ExerciseSet.bodyweight(ExerciseSet.MIN_REPS, null).reps()).isEqualTo(1);
        assertThat(ExerciseSet.bodyweight(ExerciseSet.MAX_REPS, null).reps()).isEqualTo(100);
    }

    @Test
    void external_volume_is_reps_times_external_weight() {
        assertThat(ExerciseSet.external(5, Weight.ofKilograms(100), null).volumeKg())
                .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void weighted_volume_counts_only_the_added_load_not_the_bodyweight() {
        // PersonalTraining ne connaît pas le poids de corps : seule la charge AJOUTÉE compte dans ce volume
        // externe (la charge totale, avec poids de corps, est résolue côté Athletics).
        assertThat(ExerciseSet.weighted(5, Weight.ofKilograms(40), null).volumeKg())
                .isEqualByComparingTo(BigDecimal.valueOf(200));
    }
}
