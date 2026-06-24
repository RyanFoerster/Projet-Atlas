package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingHistoryTest {

    private static final Instant DAY2 = Instant.parse("2026-06-23T18:00:00Z");
    private static final Instant DAY1 = DAY2.minusSeconds(86_400);
    private static final Instant DAY3 = DAY2.plusSeconds(86_400);

    @Test
    void empty_history_has_no_workout() {
        TrainingHistory empty = TrainingHistory.empty();

        assertThat(empty.hasWorkouts()).isFalse();
        assertThat(empty.lastWorkoutAt()).isNull();
        assertThat(empty.lastPatternsCovered()).isEmpty();
    }

    @Test
    void recording_a_workout_sets_the_last_session() {
        TrainingHistory after = TrainingHistory.empty().recordWorkout(DAY2, Set.of(MovementPattern.SQUAT));

        assertThat(after.lastWorkoutAt()).isEqualTo(DAY2);
        assertThat(after.lastPatternsCovered()).containsExactly(MovementPattern.SQUAT);
    }

    @Test
    void a_more_recent_workout_overwrites() {
        TrainingHistory after = TrainingHistory.empty()
                .recordWorkout(DAY2, Set.of(MovementPattern.SQUAT))
                .recordWorkout(DAY3, Set.of(MovementPattern.BENCH_PRESS));

        assertThat(after.lastWorkoutAt()).isEqualTo(DAY3);
        assertThat(after.lastPatternsCovered()).containsExactly(MovementPattern.BENCH_PRESS);
    }

    @Test
    void replaying_the_same_workout_is_a_no_op() {
        // Idempotence : rejouer l'event (au restart / après échec) ne change rien — date égale → no-op.
        TrainingHistory once = TrainingHistory.empty().recordWorkout(DAY2, Set.of(MovementPattern.SQUAT));
        TrainingHistory twice = once.recordWorkout(DAY2, Set.of(MovementPattern.SQUAT));

        assertThat(twice).isEqualTo(once);
    }

    @Test
    void an_older_workout_received_out_of_order_is_ignored() {
        // Robustesse au désordre de livraison : un event antérieur reçu après ne régresse pas la date.
        TrainingHistory history = TrainingHistory.empty()
                .recordWorkout(DAY2, Set.of(MovementPattern.SQUAT))
                .recordWorkout(DAY1, Set.of(MovementPattern.DEADLIFT)); // plus ancien → ignoré

        assertThat(history.lastWorkoutAt()).isEqualTo(DAY2);
        assertThat(history.lastPatternsCovered()).containsExactly(MovementPattern.SQUAT);
    }

    @Test
    void patterns_are_copied_defensively() {
        var mutable = new java.util.HashSet<>(Set.of(MovementPattern.SQUAT));
        TrainingHistory history = TrainingHistory.empty().recordWorkout(DAY2, mutable);
        mutable.add(MovementPattern.BENCH_PRESS); // mutation après coup

        assertThat(history.lastPatternsCovered()).containsExactly(MovementPattern.SQUAT);
    }
}
