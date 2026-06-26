package dev.ryanfoerster.atlas;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutCommand;
import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutUseCase;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Reproduit le bug d'ACCUMULATION : logger deux séances espacées dans le temps sur le même miroir doit
 * faire ACCUMULER l'état (charger l'existant → décroître depuis lastUpdated → ajouter le stimulus → sauver),
 * pas l'écraser. Après deux séances à 10 jours d'intervalle, la fitness (τ long) a moins décru que la
 * fatigue (τ court) → {@code fitness > fatigue STRICTEMENT}. Un écrasement (état reparti du seul stimulus)
 * donnerait {@code fitness == fatigue}.
 *
 * <p>Chemin non couvert jusqu'ici : le test event-driven validait UNE séance ; la simulation GATE 1
 * chaînait {@code applyStimulus} en mémoire (le modèle math), jamais le chemin persistant multi-séances.
 */
class MirrorConditionAccumulationTest extends AbstractIntegrationTest {

    private static final Instant SESSION_1_AT = Instant.parse("2026-06-14T18:00:00Z"); // J-10
    private static final Instant SESSION_2_AT = Instant.parse("2026-06-24T18:00:00Z"); // J0
    private static final Instant NOW = Instant.parse("2026-06-24T20:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();

    @Autowired
    private LogWorkoutUseCase logWorkout;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AthleteConditionRepository conditionRepository;

    @Test
    void two_spaced_sessions_accumulate_instead_of_overwriting_the_condition() {
        UserId owner = createOwnerWithMirror();
        AthleteId mirrorId = rosterRepository.findByOwnerId(owner).orElseThrow()
                .mirrorAthlete().orElseThrow().id();

        // Séance 1 (J-10) : squat 5×5 @ RPE 8 (volume conséquent).
        logWorkout.logWorkout(owner, new LogWorkoutCommand(SESSION_1_AT, 60, null, List.of(
                new LoggedExercise(ExerciseName.of("Back Squat"),
                        ExerciseCategory.compound(MovementPattern.SQUAT),
                        List.of(new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)),
                                new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)),
                                new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)),
                                new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)),
                                new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)))))));
        await().atMost(Duration.ofSeconds(10)).until(() -> SESSION_1_AT.equals(lastUpdated(mirrorId)));

        // Séance 2 (J0) : petite séance (traction 1×10 @ RPE 8), comme le scénario manuel.
        logWorkout.logWorkout(owner, new LogWorkoutCommand(SESSION_2_AT, 20, null, List.of(
                new LoggedExercise(ExerciseName.of("Pull-up"),
                        ExerciseCategory.compound(MovementPattern.CHIN_UP),
                        List.of(new ExerciseSet(10, Weight.ofKilograms(80), RPE.of(8.0)))))));
        await().atMost(Duration.ofSeconds(10)).until(() -> SESSION_2_AT.equals(lastUpdated(mirrorId)));

        AthleteCondition condition = conditionRepository.findByAthleteId(mirrorId).orElseThrow();
        // Accumulation correcte : le résidu de la séance 1 (fitness > fatigue après 10j de décroissance)
        // s'ajoute à la séance 2 → fitness STRICTEMENT supérieure à fatigue. Un écrasement donnerait l'égalité.
        assertThat(condition.state().fitness()).isGreaterThan(condition.state().fatigue());
    }

    @Test
    void a_small_session_on_top_of_real_history_does_not_collapse_the_state() {
        // Réplique fidèle du scénario manuel : 3 séances sur 3 jours (volumes décroissants) + une 4e PETITE
        // (traction 1×10). L'écrasement supposé donnerait fitness == fatigue ; l'accumulation donne fitness > fatigue.
        UserId owner = createOwnerWithMirror();
        AthleteId mirrorId = rosterRepository.findByOwnerId(owner).orElseThrow()
                .mirrorAthlete().orElseThrow().id();

        logAndAwait(owner, mirrorId, squatSession(Instant.parse("2026-06-21T18:00:00Z"), 7));  // ~35 reps
        logAndAwait(owner, mirrorId, squatSession(Instant.parse("2026-06-22T18:00:00Z"), 5));  // 25 reps
        logAndAwait(owner, mirrorId, squatSession(Instant.parse("2026-06-23T18:00:00Z"), 2));  // 10 reps
        logAndAwait(owner, mirrorId, new LogWorkoutCommand(SESSION_2_AT, 20, null, List.of(
                new LoggedExercise(ExerciseName.of("Pull-up"),
                        ExerciseCategory.compound(MovementPattern.CHIN_UP),
                        List.of(new ExerciseSet(10, Weight.ofKilograms(80), RPE.of(8.0)))))));

        AthleteCondition condition = conditionRepository.findByAthleteId(mirrorId).orElseThrow();
        assertThat(condition.state().fitness()).isGreaterThan(condition.state().fatigue());
    }

    private void logAndAwait(UserId owner, AthleteId mirrorId, LogWorkoutCommand command) {
        logWorkout.logWorkout(owner, command);
        await().atMost(Duration.ofSeconds(10)).until(() -> command.performedAt().equals(lastUpdated(mirrorId)));
    }

    private static LogWorkoutCommand squatSession(Instant at, int sets) {
        List<ExerciseSet> setList = new ArrayList<>();
        for (int i = 0; i < sets; i++) {
            setList.add(new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0)));
        }
        return new LogWorkoutCommand(at, 60, null, List.of(
                new LoggedExercise(ExerciseName.of("Back Squat"),
                        ExerciseCategory.compound(MovementPattern.SQUAT), setList)));
    }

    private Instant lastUpdated(AthleteId mirrorId) {
        return conditionRepository.findByAthleteId(mirrorId)
                .map(c -> c.state().lastUpdated())
                .orElse(null);
    }

    private UserId createOwnerWithMirror() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(80), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(user.id(), NOW).addMirror(request, GENERATOR, 42L, NOW));
        return user.id();
    }
}
