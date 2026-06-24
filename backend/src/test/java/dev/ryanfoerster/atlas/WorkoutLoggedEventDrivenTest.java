package dev.ryanfoerster.atlas;

import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.personaltraining.api.PersonalTrainingQueryPort;
import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutCommand;
import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutUseCase;
import dev.ryanfoerster.atlas.personaltraining.domain.model.BodyRegion;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test end-to-end de la communication event-driven Modulith (GATE C, le risque structurel n°1) :
 * PersonalTraining logge une séance → publie {@code WorkoutLogged} → Roster la consomme en async →
 * le {@code TrainingHistory} du miroir est à jour. Utilise l'API {@code Scenario} de Modulith, qui sait
 * attendre la consommation asynchrone (pas d'assertion synchrone naïve).
 */
@EnableScenarios
class WorkoutLoggedEventDrivenTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final Instant PERFORMED_AT = Instant.parse("2026-06-23T18:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();

    @Autowired
    private LogWorkoutUseCase logWorkout;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersonalTrainingQueryPort personalTrainingQuery;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void logging_a_workout_updates_the_mirror_training_history_via_event(Scenario scenario) {
        UserId owner = createOwnerWithMirror();

        scenario.stimulate(() -> logWorkout.logWorkout(owner, command(PERFORMED_AT)))
                .andWaitForStateChange(() -> mirrorLastWorkoutAt(owner))
                .andVerify(lastWorkoutAt -> assertThat(lastWorkoutAt).isEqualTo(PERFORMED_AT));

        // Le miroir a bien reçu la séance : date + patterns de FORCE seulement (l'accessoire BICEPS exclu).
        Athlete mirror = mirror(owner);
        assertThat(mirror.trainingHistory().lastPatternsCovered()).containsExactly(MovementPattern.SQUAT);

        // Le count n'est PAS dans Roster : sa source de vérité est PersonalTraining (option D).
        assertThat(personalTrainingQuery.countSessionsFor(owner)).isEqualTo(1);
    }

    @Test
    void a_successfully_consumed_publication_is_marked_completed(Scenario scenario) {
        // Preuve empirique du comportement (a) : après succès du handler, completion_date est NON-NULL
        // → la publication est exclue du republish au restart (vérifié au désassemblage du registry).
        UserId owner = createOwnerWithMirror();

        scenario.stimulate(() -> logWorkout.logWorkout(owner, command(PERFORMED_AT)))
                .andWaitForStateChange(() -> mirrorLastWorkoutAt(owner));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(workoutLoggedCompletionDate(owner)).isNotNull());
    }

    /** {@code completion_date} de la publication {@code WorkoutLogged} de ce owner, ou null si absente/incomplète. */
    private Instant workoutLoggedCompletionDate(UserId owner) {
        List<Timestamp> rows = jdbcTemplate.query(
                "SELECT completion_date FROM event_publication "
                        + "WHERE event_type LIKE '%WorkoutLogged' AND serialized_event LIKE ?",
                (rs, n) -> rs.getTimestamp("completion_date"), "%" + owner.value() + "%");
        return rows.isEmpty() || rows.getFirst() == null ? null : rows.getFirst().toInstant();
    }

    private UserId createOwnerWithMirror() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        UserId owner = user.id();
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(80), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(owner, NOW).addMirror(request, GENERATOR, 42L, NOW));
        return owner;
    }

    private static LogWorkoutCommand command(Instant performedAt) {
        LoggedExercise squat = new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0))));
        LoggedExercise curl = new LoggedExercise(ExerciseName.of("Barbell Curl"),
                ExerciseCategory.accessory(BodyRegion.BICEPS),
                List.of(new ExerciseSet(12, Weight.ofKilograms(20), null)));
        return new LogWorkoutCommand(performedAt, 60, null, List.of(squat, curl));
    }

    private Instant mirrorLastWorkoutAt(UserId owner) {
        return rosterRepository.findByOwnerId(owner)
                .flatMap(Roster::mirrorAthlete)
                .map(a -> a.trainingHistory().lastWorkoutAt())
                .orElse(null);
    }

    private Athlete mirror(UserId owner) {
        return rosterRepository.findByOwnerId(owner).orElseThrow().mirrorAthlete().orElseThrow();
    }
}
