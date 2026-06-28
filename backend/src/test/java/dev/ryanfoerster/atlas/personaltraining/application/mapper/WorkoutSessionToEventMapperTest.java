package dev.ryanfoerster.atlas.personaltraining.application.mapper;

import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutSessionToEventMapperTest {

    private static final Instant NOW = Instant.parse("2026-06-24T18:00:00Z");
    private final UserId owner = UserId.generate();

    @Test
    void flattens_session_to_event_preserving_values_and_the_sealed_discriminant() {
        LoggedExercise squat = new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(7.5))));
        LoggedExercise curl = new LoggedExercise(ExerciseName.of("Barbell Curl"),
                ExerciseCategory.accessory(BodyRegion.BICEPS),
                List.of(new ExerciseSet(12, null, null))); // poids de corps, pas de RPE
        WorkoutSession session = WorkoutSession.log(owner, NOW.minusSeconds(3600),
                List.of(squat, curl), 60, null, NOW);

        WorkoutLogged event = WorkoutSessionToEventMapper.toEvent(session);

        assertThat(event.ownerId()).isEqualTo(owner.value());
        assertThat(event.sessionId()).isEqualTo(session.id().value());
        assertThat(event.performedAt()).isEqualTo(NOW.minusSeconds(3600));
        assertThat(event.durationMinutes()).isEqualTo(60);
        assertThat(event.exercises()).hasSize(2);

        LoggedExerciseSnapshot squatSnap = event.exercises().get(0);
        assertThat(squatSnap.categoryType()).isEqualTo(LoggedExerciseSnapshot.COMPOUND_FORCE);
        assertThat(squatSnap.pattern()).isEqualTo(MovementPattern.SQUAT);
        assertThat(squatSnap.accessoryRegion()).isNull();
        assertThat(squatSnap.sets()).singleElement().satisfies(s -> {
            assertThat(s.reps()).isEqualTo(5);
            assertThat(s.weightKg()).isEqualTo(140.0);
            assertThat(s.rpe()).isEqualTo(7.5);
        });

        LoggedExerciseSnapshot curlSnap = event.exercises().get(1);
        assertThat(curlSnap.categoryType()).isEqualTo(LoggedExerciseSnapshot.ACCESSORY);
        assertThat(curlSnap.pattern()).isNull();
        assertThat(curlSnap.accessoryRegion()).isEqualTo("BICEPS");
        assertThat(curlSnap.sets()).singleElement().satisfies(s -> {
            assertThat(s.weightKg()).isNull(); // poids de corps survit
            assertThat(s.rpe()).isNull();
        });
    }

    @Test
    void nullable_duration_becomes_null_in_the_event() {
        WorkoutSession session = WorkoutSession.log(owner, NOW.minusSeconds(3600),
                List.of(new LoggedExercise(ExerciseName.of("Back Squat"),
                        ExerciseCategory.compound(MovementPattern.SQUAT),
                        List.of(new ExerciseSet(5, Weight.ofKilograms(100), null)))),
                null, null, NOW);

        assertThat(WorkoutSessionToEventMapper.toEvent(session).durationMinutes()).isNull();
    }
}
