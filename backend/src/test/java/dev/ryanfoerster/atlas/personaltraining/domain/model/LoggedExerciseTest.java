package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseException;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class LoggedExerciseTest {

    private static final ExerciseCategory SQUAT = ExerciseCategory.compound(MovementPattern.SQUAT);

    @Test
    void aggregates_sets_reps_and_volume() {
        LoggedExercise exercise = new LoggedExercise(ExerciseName.of("Back Squat"), SQUAT, List.of(
                new ExerciseSet(5, Weight.ofKilograms(100), null),
                new ExerciseSet(5, Weight.ofKilograms(100), null)));

        assertThat(exercise.totalSets()).isEqualTo(2);
        assertThat(exercise.totalReps()).isEqualTo(10);
        assertThat(exercise.volumeKg()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void rejects_an_exercise_without_any_set() {
        assertThatExceptionOfType(InvalidExerciseException.class)
                .isThrownBy(() -> new LoggedExercise(ExerciseName.of("Squat"), SQUAT, List.of()));
    }

    @Test
    void copies_the_sets_defensively() {
        List<ExerciseSet> mutable = new ArrayList<>();
        mutable.add(new ExerciseSet(5, Weight.ofKilograms(100), null));

        LoggedExercise exercise = new LoggedExercise(ExerciseName.of("Squat"), SQUAT, mutable);
        mutable.add(new ExerciseSet(5, Weight.ofKilograms(100), null)); // mutation après construction

        assertThat(exercise.totalSets()).isEqualTo(1); // l'exercice n'a pas bougé
    }
}
