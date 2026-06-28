package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.EmptyWorkoutSessionException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidPerformedAtException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidWorkoutSessionException;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WorkoutSessionTest {

    private static final Instant NOW = Instant.parse("2026-06-24T18:00:00Z");
    private static final Instant YESTERDAY = NOW.minusSeconds(86_400);
    private final UserId owner = UserId.generate();

    private static LoggedExercise compound(MovementPattern pattern, int sets) {
        List<ExerciseSet> exerciseSets = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            exerciseSets.add(new ExerciseSet(5, Weight.ofKilograms(100), RPE.of(8.0)));
        }
        return new LoggedExercise(ExerciseName.of(pattern.name()), ExerciseCategory.compound(pattern), exerciseSets);
    }

    private static LoggedExercise accessory(BodyRegion region) {
        return new LoggedExercise(ExerciseName.of(region.name() + " work"),
                ExerciseCategory.accessory(region),
                List.of(new ExerciseSet(12, Weight.ofKilograms(20), null)));
    }

    @Test
    void log_creates_a_session_with_a_generated_id_and_creation_timestamp() {
        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY,
                List.of(compound(MovementPattern.SQUAT, 3)), 75, "Bonnes sensations", NOW);

        assertThat(session.id()).isNotNull();
        assertThat(session.ownerId()).isEqualTo(owner);
        assertThat(session.performedAt()).isEqualTo(YESTERDAY);
        assertThat(session.createdAt()).isEqualTo(NOW);
        assertThat(session.durationMinutes()).contains(75);
        assertThat(session.notes()).contains("Bonnes sensations");
    }

    @Test
    void log_rejects_an_empty_session() {
        assertThatExceptionOfType(EmptyWorkoutSessionException.class)
                .isThrownBy(() -> WorkoutSession.log(owner, YESTERDAY, List.of(), 60, null, NOW));
    }

    @Test
    void log_rejects_a_session_performed_in_the_future() {
        Instant future = NOW.plusSeconds(3_600);
        assertThatExceptionOfType(InvalidPerformedAtException.class)
                .isThrownBy(() -> WorkoutSession.log(owner, future,
                        List.of(compound(MovementPattern.SQUAT, 1)), null, null, NOW));
    }

    @Test
    void log_accepts_a_session_performed_exactly_now() {
        WorkoutSession session = WorkoutSession.log(owner, NOW,
                List.of(compound(MovementPattern.SQUAT, 1)), null, null, NOW);

        assertThat(session.performedAt()).isEqualTo(NOW);
    }

    @Test
    void duration_is_optional_but_must_be_plausible_when_present() {
        assertThat(WorkoutSession.log(owner, YESTERDAY,
                List.of(compound(MovementPattern.SQUAT, 1)), null, null, NOW).durationMinutes()).isEmpty();

        assertThatExceptionOfType(InvalidWorkoutSessionException.class)
                .isThrownBy(() -> WorkoutSession.log(owner, YESTERDAY,
                        List.of(compound(MovementPattern.SQUAT, 1)), 0, null, NOW));
        assertThatExceptionOfType(InvalidWorkoutSessionException.class)
                .isThrownBy(() -> WorkoutSession.log(owner, YESTERDAY,
                        List.of(compound(MovementPattern.SQUAT, 1)),
                        WorkoutSession.MAX_DURATION_MINUTES + 1, null, NOW));
    }

    @Test
    void blank_notes_become_no_notes_and_too_long_notes_are_rejected() {
        assertThat(WorkoutSession.log(owner, YESTERDAY,
                List.of(compound(MovementPattern.SQUAT, 1)), null, "   ", NOW).notes()).isEmpty();

        String tooLong = "x".repeat(WorkoutSession.MAX_NOTES_LENGTH + 1);
        assertThatExceptionOfType(InvalidWorkoutSessionException.class)
                .isThrownBy(() -> WorkoutSession.log(owner, YESTERDAY,
                        List.of(compound(MovementPattern.SQUAT, 1)), null, tooLong, NOW));
    }

    @Test
    void computes_total_sets_reps_and_estimated_volume() {
        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY, List.of(
                compound(MovementPattern.SQUAT, 3),    // 3 sets × 5 reps × 100kg
                accessory(BodyRegion.BICEPS)),         // 1 set × 12 reps × 20kg
                null, null, NOW);

        assertThat(session.totalSets()).isEqualTo(4);
        assertThat(session.totalReps()).isEqualTo(3 * 5 + 12);
        assertThat(session.estimatedVolume()).isEqualTo(3 * 5 * 100.0 + 12 * 20.0);
    }

    @Test
    void patterns_covered_counts_only_compound_force_exercises() {
        // Cas mixte (test demandé en rétro) : 2 exercices composés DISTINCTS + 3 accessoires.
        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY, List.of(
                compound(MovementPattern.SQUAT, 3),
                compound(MovementPattern.BENCH_PRESS, 3),
                accessory(BodyRegion.BICEPS),
                accessory(BodyRegion.TRICEPS),
                accessory(BodyRegion.CALVES)),
                null, null, NOW);

        // 5 exercices, mais seulement 2 patterns de force couverts — pas 5.
        assertThat(session.patternsCovered())
                .containsExactlyInAnyOrder(MovementPattern.SQUAT, MovementPattern.BENCH_PRESS);
    }

    @Test
    void patterns_covered_deduplicates_the_same_pattern() {
        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY, List.of(
                compound(MovementPattern.SQUAT, 3),
                compound(MovementPattern.SQUAT, 2)), // même pattern deux fois
                null, null, NOW);

        assertThat(session.patternsCovered()).containsExactly(MovementPattern.SQUAT);
    }

    @Test
    void exercises_are_copied_defensively() {
        List<LoggedExercise> mutable = new ArrayList<>();
        mutable.add(compound(MovementPattern.SQUAT, 1));

        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY, mutable, null, null, NOW);
        mutable.add(compound(MovementPattern.BENCH_PRESS, 1)); // mutation après construction

        assertThat(session.exercises()).hasSize(1);
    }

    @Test
    void equality_is_by_identity() {
        WorkoutSession session = WorkoutSession.log(owner, YESTERDAY,
                List.of(compound(MovementPattern.SQUAT, 1)), null, null, NOW);
        WorkoutSession rehydrated = WorkoutSession.reconstitute(session.id(), owner, YESTERDAY,
                null, session.exercises(), null, NOW);

        assertThat(rehydrated).isEqualTo(session); // même id → égaux
        assertThat(rehydrated).hasSameHashCodeAs(session);
    }
}
