package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ExerciseNameTest {

    @Test
    void accepts_a_valid_name() {
        assertThat(ExerciseName.of("Back Squat").value()).isEqualTo("Back Squat");
    }

    @Test
    void trims_surrounding_whitespace() {
        assertThat(ExerciseName.of("  Bench Press  ").value()).isEqualTo("Bench Press");
    }

    @Test
    void rejects_a_name_too_short() {
        assertThatExceptionOfType(InvalidExerciseNameException.class)
                .isThrownBy(() -> ExerciseName.of("A"));
    }

    @Test
    void rejects_a_blank_name_after_trim() {
        assertThatExceptionOfType(InvalidExerciseNameException.class)
                .isThrownBy(() -> ExerciseName.of("   "));
    }

    @Test
    void rejects_a_name_too_long() {
        String tooLong = "x".repeat(ExerciseName.MAX_LENGTH + 1);
        assertThatExceptionOfType(InvalidExerciseNameException.class)
                .isThrownBy(() -> ExerciseName.of(tooLong));
    }

    @Test
    void rejects_null() {
        assertThatExceptionOfType(InvalidExerciseNameException.class)
                .isThrownBy(() -> ExerciseName.of(null));
    }

    @Test
    void equality_is_by_value() {
        assertThat(ExerciseName.of("Squat")).isEqualTo(ExerciseName.of("Squat"));
    }
}
