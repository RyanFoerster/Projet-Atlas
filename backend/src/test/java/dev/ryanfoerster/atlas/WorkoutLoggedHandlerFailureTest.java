package dev.ryanfoerster.atlas;

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
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test NÉGATIF (bonus GATE C) : quand le consumer {@link dev.ryanfoerster.atlas.roster.application.eventhandler.WorkoutLoggedHandler}
 * <strong>échoue</strong>, l'event publication reste <strong>incomplète</strong> ({@code completion_date IS NULL})
 * et la séance reste loggée. C'est la preuve empirique de la durabilité « outbox / at-least-once » :
 * la séance n'est pas perdue, et l'event sera re-livré au restart (republish des incomplets, ADR-023).
 *
 * <p>On force l'échec en mockant {@link RosterRepository#save} pour qu'il lève. La séance, elle, est
 * persistée par le vrai {@link WorkoutSessionRepository} (commit avant le handler async).
 */
class WorkoutLoggedHandlerFailureTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final Instant PERFORMED_AT = Instant.parse("2026-06-23T18:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();

    @MockitoBean
    private RosterRepository rosterRepository; // mocké : le handler échouera à la sauvegarde

    @Autowired
    private LogWorkoutUseCase logWorkout;
    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void a_failing_handler_leaves_the_publication_incomplete_and_the_session_logged() {
        UserId owner = createOwner();
        Roster roster = Roster.createFor(owner, NOW).addMirror(mirrorRequest(), GENERATOR, 42L, NOW);
        when(rosterRepository.findByOwnerId(owner)).thenReturn(Optional.of(roster));
        when(rosterRepository.save(any())).thenThrow(new RuntimeException("boom : sauvegarde du miroir échoue"));

        logWorkout.logWorkout(owner, command());

        // Le handler s'exécute en async puis échoue à save → on attend qu'il ait tenté.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(rosterRepository, atLeastOnce()).save(any()));

        // 1) La séance est loggée malgré l'échec du consumer (elle a commité avant le handler).
        assertThat(workoutSessionRepository.countByOwner(owner)).isEqualTo(1);
        // 2) La publication reste INCOMPLÈTE (completion_date NULL) → re-livrée au restart, pas perdue.
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(workoutLoggedCompletionDate(owner)).isNull());
    }

    private UserId createOwner() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        return user.id();
    }

    private static MirrorCreationRequest mirrorRequest() {
        return new MirrorCreationRequest(AthleteName.of("Ryan"), 30, Weight.ofKilograms(80),
                Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
    }

    private static LogWorkoutCommand command() {
        LoggedExercise squat = new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(new ExerciseSet(5, Weight.ofKilograms(140), RPE.of(8.0))));
        return new LogWorkoutCommand(PERFORMED_AT, 60, null, List.of(squat));
    }

    private Instant workoutLoggedCompletionDate(UserId owner) {
        List<Timestamp> rows = jdbcTemplate.query(
                "SELECT completion_date FROM event_publication "
                        + "WHERE event_type LIKE '%WorkoutLogged' AND serialized_event LIKE ?",
                (rs, n) -> rs.getTimestamp("completion_date"), "%" + owner.value() + "%");
        return rows.isEmpty() || rows.getFirst() == null ? null : rows.getFirst().toInstant();
    }
}
