package dev.ryanfoerster.atlas.athletics.application.eventhandler;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.MuscleStimulusMapping;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.personaltraining.api.events.ExerciseSetSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.api.GeneticProfile;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
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

    private static final GeneticProfile NEUTRAL_PROFILE = new GeneticProfile(1.0, 1.0, 0.5);

    private WorkoutStimulusHandler handler(Optional<AthleteId> mirror) {
        return handler(mirror, NEUTRAL_PROFILE);
    }

    private WorkoutStimulusHandler handler(Optional<AthleteId> mirror, GeneticProfile profile) {
        RosterQueryPort rosterQuery = new RosterQueryPort() {
            @Override
            public Optional<AthleteId> findMirrorAthleteId(UserId owner) {
                return mirror;
            }

            @Override
            public Optional<GeneticProfile> findGeneticProfile(AthleteId athleteId) {
                return mirror.map(id -> profile);
            }
        };
        return new WorkoutStimulusHandler(rosterQuery, conditions, snapshots, new BanisterModel(),
                new StimulusCalculator(), new MuscleStimulusMapping());
    }

    // Séance : squat 5×1 @ RPE 8 + curl 12×1 sans RPE (effort neutre). Magnitudes dérivées de la formule.
    private static final double RAW_SQUAT = 5 * StimulusCalculator.effortFactor(8.0);
    private static final double RAW_CURL = 12 * StimulusCalculator.effortFactor(null);

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
        double expectedTotal = StimulusCalculator.NORMALIZATION * (RAW_SQUAT + RAW_CURL); // conservé (poids = 1)
        assertThat(condition.state().totalFitness()).isCloseTo(expectedTotal, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(condition.state().totalFatigue()).isCloseTo(expectedTotal, org.assertj.core.api.Assertions.within(1e-9));
        // Distribution Couche 2 : le squat réparti sur les jambes (QUADS = 0.42, GLUTES = 0.30), le curl
        // entièrement sur les BICEPS — le squat ne touche PAS les biceps.
        assertThat(condition.state().condition(MuscleGroup.QUADS).fitness())
                .isCloseTo(StimulusCalculator.NORMALIZATION * RAW_SQUAT * 0.42, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(condition.state().condition(MuscleGroup.GLUTES).fitness())
                .isCloseTo(StimulusCalculator.NORMALIZATION * RAW_SQUAT * 0.30, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(condition.state().condition(MuscleGroup.BICEPS).fitness())
                .isCloseTo(StimulusCalculator.NORMALIZATION * RAW_CURL, org.assertj.core.api.Assertions.within(1e-9));
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
        double afterFirst = conditions.store.get(mirrorId).state().totalFitness();

        handler.on(event()); // rejeu : même performedAt → garde acceptsStimulusAt false → no-op

        assertThat(conditions.store.get(mirrorId).state().totalFitness()).isEqualTo(afterFirst);
        assertThat(snapshots.store).hasSize(1); // pas de second snapshot
    }

    @Test
    void a_stronger_responder_builds_more_form_from_the_same_session() {
        AthleteId strong = AthleteId.generate();
        AthleteId weak = AthleteId.generate();
        // Génétique résolue à la création : sensitivity 1.15 (fort répondeur) vs 0.85 (faible), même séance.
        handler(Optional.of(strong), new GeneticProfile(1.0, 1.15, 0.5)).on(event());
        handler(Optional.of(weak), new GeneticProfile(1.0, 0.85, 0.5)).on(event());

        assertThat(conditions.store.get(strong).state().totalFitness())
                .isGreaterThan(conditions.store.get(weak).state().totalFitness());
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
