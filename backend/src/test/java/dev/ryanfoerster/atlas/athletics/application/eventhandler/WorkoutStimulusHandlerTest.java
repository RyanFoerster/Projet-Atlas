package dev.ryanfoerster.atlas.athletics.application.eventhandler;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.personaltraining.api.events.ExerciseSetSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du {@link WorkoutStimulusHandler} avec des fakes (déterministe, sans Testcontainers) : le no-op
 * quand le Player n'a pas de miroir, l'application du stimulus + snapshot, et l'idempotence du rejeu.
 */
class WorkoutStimulusHandlerTest {

    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private static final Instant PERFORMED_AT = Instant.parse("2026-06-23T18:00:00Z");
    private final AthleteId mirrorId = AthleteId.generate();

    private final FakeConditionRepository conditions = new FakeConditionRepository();
    private final FakeSnapshotRepository snapshots = new FakeSnapshotRepository();

    private WorkoutStimulusHandler handler(Optional<AthleteId> mirror) {
        RosterQueryPort rosterQuery = owner -> mirror;
        return new WorkoutStimulusHandler(rosterQuery, conditions, snapshots, new BanisterModel(),
                new StimulusCalculator());
    }

    /** Séance : squat 5×1 @ RPE 8 (effort 0.8) + curl 12×1 sans RPE (effort 0.7) → raw = 4.0 + 8.4 = 12.4. */
    private static WorkoutLogged event() {
        LoggedExerciseSnapshot squat = new LoggedExerciseSnapshot("Back Squat",
                LoggedExerciseSnapshot.COMPOUND_FORCE, MovementPattern.SQUAT, null,
                List.of(new ExerciseSetSnapshot(5, 140.0, 8.0)));
        LoggedExerciseSnapshot curl = new LoggedExerciseSnapshot("Barbell Curl",
                LoggedExerciseSnapshot.ACCESSORY, null, "BICEPS",
                List.of(new ExerciseSetSnapshot(12, 20.0, null)));
        return new WorkoutLogged(OWNER, SESSION, PERFORMED_AT, 60, List.of(squat, curl));
    }

    @Test
    void without_a_mirror_the_event_is_a_no_op() {
        handler(Optional.empty()).on(event());

        assertThat(conditions.store).isEmpty();
        assertThat(snapshots.store).isEmpty();
    }

    @Test
    void the_first_session_creates_the_condition_and_a_snapshot() {
        handler(Optional.of(mirrorId)).on(event());

        AthleteCondition condition = conditions.store.get(mirrorId);
        assertThat(condition).isNotNull();
        double expected = StimulusCalculator.NORMALIZATION * 12.4; // raw 12.4 × NORM
        assertThat(condition.state().fitness()).isCloseTo(expected, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(condition.state().fatigue()).isCloseTo(expected, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(condition.state().lastUpdated()).isEqualTo(PERFORMED_AT);

        assertThat(snapshots.store).hasSize(1);
        ConditionSnapshot snapshot = snapshots.store.getFirst();
        assertThat(snapshot.takenAt()).isEqualTo(PERFORMED_AT);
        assertThat(snapshot.performance()).isNegative(); // « cuit » juste après (k1−k2)·S < 0
    }

    @Test
    void replaying_the_same_event_does_not_apply_the_stimulus_twice() {
        WorkoutStimulusHandler handler = handler(Optional.of(mirrorId));
        handler.on(event());
        double afterFirst = conditions.store.get(mirrorId).state().fitness();

        handler.on(event()); // rejeu : même performedAt → garde acceptsStimulusAt false → no-op

        assertThat(conditions.store.get(mirrorId).state().fitness()).isEqualTo(afterFirst);
        assertThat(snapshots.store).hasSize(1); // pas de second snapshot
    }

    private static final class FakeConditionRepository implements AthleteConditionRepository {
        private final Map<AthleteId, AthleteCondition> store = new HashMap<>();

        @Override
        public AthleteCondition save(AthleteCondition condition) {
            store.put(condition.athleteId(), condition);
            return condition;
        }

        @Override
        public Optional<AthleteCondition> findByAthleteId(AthleteId athleteId) {
            return Optional.ofNullable(store.get(athleteId));
        }
    }

    private static final class FakeSnapshotRepository implements ConditionSnapshotRepository {
        private final List<ConditionSnapshot> store = new ArrayList<>();

        @Override
        public ConditionSnapshot save(ConditionSnapshot snapshot) {
            store.add(snapshot);
            return snapshot;
        }

        @Override
        public List<ConditionSnapshot> findByAthleteId(AthleteId athleteId) {
            return store.stream().filter(s -> s.athleteId().equals(athleteId)).toList();
        }
    }
}
