package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutSessionPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T18:00:00Z");
    private static final Instant YESTERDAY = NOW.minusSeconds(86_400);

    @Autowired
    private WorkoutSessionRepository repository;
    @Autowired
    private WorkoutSessionJpaRepository jpaRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserId createOwner() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        return user.id();
    }

    /** Séance riche : un composé chargé (avec RPE), un accessoire au poids de corps (sans RPE). */
    private static WorkoutSession richSession(UserId owner) {
        LoggedExercise squat = new LoggedExercise(
                ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(
                        ExerciseSet.external(5, Weight.ofKilograms(140), RPE.of(7.5)),
                        ExerciseSet.external(5, Weight.ofKilograms(140), RPE.of(8.5))));
        LoggedExercise plank = new LoggedExercise(
                ExerciseName.of("Plank"),
                ExerciseCategory.accessory(BodyRegion.CORE),
                List.of(ExerciseSet.bodyweight(3, null))); // poids de corps, pas de RPE
        return WorkoutSession.log(owner, YESTERDAY, List.of(squat, plank), 75, "Sensations propres", NOW);
    }

    @Test
    void reads_a_legacy_row_whose_sets_have_no_load_type_without_crashing() {
        // Régression du bug « V013 inopérante » : des séries loggées avant le sprint 6 n'ont PAS de loadType
        // dans le JSONB. findByOwner (l'historique) ne doit pas planter — le lecteur tolérant infère le type.
        UserId owner = createOwner();
        String legacyExercises = """
                {"exercises":[
                  {"name":"Back Squat","categoryType":"COMPOUND_FORCE","pattern":"SQUAT","region":null,
                   "sets":[{"reps":5,"weightKg":140,"rpe":7.5},{"reps":12,"weightKg":null,"rpe":null}]}
                ]}""";
        jdbcTemplate.update(
                "INSERT INTO workout_sessions (id, owner_id, performed_at, duration_minutes, notes, exercises, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), owner.value(), Timestamp.from(YESTERDAY), null, null, legacyExercises,
                Timestamp.from(NOW));

        List<WorkoutSession> sessions = repository.findByOwner(owner, 0, 20);

        assertThat(sessions).hasSize(1);
        List<ExerciseSet> sets = sessions.getFirst().exercises().getFirst().sets();
        assertThat(sets.get(0).load()).isInstanceOfSatisfying(Load.External.class,
                e -> assertThat(e.weight().toKilograms()).isEqualByComparingTo("140")); // weightKg → EXTERNAL
        assertThat(sets.get(1).load()).isEqualTo(Load.bodyweight());                    // weightKg null → BODYWEIGHT
        assertThat(sets.get(1).rpe()).isNull();                                         // RPE null legacy intact
    }

    private static final String COUNT_SETS_WITHOUT_LOAD_TYPE = """
            SELECT count(*)
            FROM workout_sessions ws
            CROSS JOIN LATERAL jsonb_array_elements(ws.exercises -> 'exercises') AS ex
            CROSS JOIN LATERAL jsonb_array_elements(ex -> 'sets') AS st
            WHERE NOT (st ? 'loadType')""";

    @Test
    void v014_backfills_legacy_sets_down_to_the_set_level_and_its_guard_holds() throws Exception {
        // Ce test exerce le SQL de V014 sur une ligne legacy : il aurait fait ÉCHOUER V013 (no-op silencieux).
        UserId owner = createOwner();
        String legacyExercises = """
                {"exercises":[
                  {"name":"Back Squat","categoryType":"COMPOUND_FORCE","pattern":"SQUAT","region":null,
                   "sets":[{"reps":5,"weightKg":140,"rpe":7.5},{"reps":12,"weightKg":null,"rpe":null}]}
                ]}""";
        jdbcTemplate.update(
                "INSERT INTO workout_sessions (id, owner_id, performed_at, duration_minutes, notes, exercises, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), owner.value(), Timestamp.from(YESTERDAY), null, null, legacyExercises,
                Timestamp.from(NOW));

        // Avant : les 2 séries de la ligne legacy n'ont pas de loadType (la situation que V013 a laissée).
        assertThat(jdbcTemplate.queryForObject(COUNT_SETS_WITHOUT_LOAD_TYPE, Integer.class)).isEqualTo(2);

        // Rejoue le VRAI SQL de V014 (idempotent : il n'a touché aucune ligne au démarrage, base vierge).
        String v014 = new String(getClass().getResourceAsStream(
                "/db/migration/V014__backfill_workout_set_load_type.sql").readAllBytes(), StandardCharsets.UTF_8);
        jdbcTemplate.execute(v014);

        // Après : 0 série sans loadType (sinon le garde-fou de V014 aurait levé), et l'inférence est correcte.
        assertThat(jdbcTemplate.queryForObject(COUNT_SETS_WITHOUT_LOAD_TYPE, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT st ->> 'loadType'
                FROM workout_sessions ws
                CROSS JOIN LATERAL jsonb_array_elements(ws.exercises -> 'exercises') AS ex
                CROSS JOIN LATERAL jsonb_array_elements(ex -> 'sets') WITH ORDINALITY AS s(st, ord)
                ORDER BY ord""", String.class))
                .containsExactly("EXTERNAL", "BODYWEIGHT"); // weightKg 140 → EXTERNAL ; weightKg null → BODYWEIGHT
    }

    @Test
    void persists_and_reloads_a_session_exactly() {
        UserId owner = createOwner();
        WorkoutSession session = richSession(owner);

        repository.save(session);
        WorkoutSession reloaded = repository.findById(session.id()).orElseThrow();

        assertThat(reloaded).isEqualTo(session);                       // égalité par identité
        assertThat(reloaded.ownerId()).isEqualTo(owner);
        assertThat(reloaded.performedAt()).isEqualTo(YESTERDAY);
        assertThat(reloaded.durationMinutes()).contains(75);
        assertThat(reloaded.notes()).contains("Sensations propres");
        assertThat(reloaded.exercises()).isEqualTo(session.exercises()); // round-trip PAR VALEUR
        assertThat(reloaded.patternsCovered()).containsExactly(MovementPattern.SQUAT);
    }

    @Test
    void sealed_exercise_category_survives_the_jsonb_round_trip() {
        // LE test de S2 : le discriminant CompoundForce/Accessory doit être correctement reconstruit.
        UserId owner = createOwner();
        WorkoutSession session = richSession(owner);
        repository.save(session);

        WorkoutSession reloaded = repository.findById(session.id()).orElseThrow();
        LoggedExercise squat = reloaded.exercises().get(0);
        LoggedExercise plank = reloaded.exercises().get(1);

        assertThat(squat.category()).isInstanceOf(ExerciseCategory.CompoundForce.class);
        assertThat(((ExerciseCategory.CompoundForce) squat.category()).pattern()).isEqualTo(MovementPattern.SQUAT);

        assertThat(plank.category()).isInstanceOf(ExerciseCategory.Accessory.class);
        assertThat(((ExerciseCategory.Accessory) plank.category()).region()).isEqualTo(BodyRegion.CORE);

        // Le type de charge survit au round-trip JSONB : poids de corps pur côté plank, externe côté squat.
        assertThat(plank.sets().getFirst().load()).isEqualTo(Load.bodyweight());
        assertThat(plank.sets().getFirst().rpe()).isNull();
        assertThat(squat.sets().getFirst().load()).isInstanceOfSatisfying(Load.External.class,
                e -> assertThat(e.weight().toKilograms()).isEqualByComparingTo("140")); // compareTo : scale-insensible
        assertThat(squat.sets().getFirst().rpe()).isEqualTo(RPE.of(7.5));
    }

    @Test
    void exercises_column_is_real_native_jsonb() {
        UserId owner = createOwner();
        WorkoutSession session = richSession(owner);
        repository.save(session);

        // Prouve que c'est du vrai jsonb natif (object, car on enveloppe la liste) — pas du text déguisé.
        String type = jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(exercises) FROM workout_sessions WHERE id = ?",
                String.class, session.id().value());

        assertThat(type).isEqualTo("object");
    }

    @Test
    void history_is_paginated_and_most_recent_first() {
        UserId owner = createOwner();
        WorkoutSession oldest = WorkoutSession.log(owner, NOW.minusSeconds(3 * 86_400),
                List.of(squatOnly()), null, null, NOW);
        WorkoutSession middle = WorkoutSession.log(owner, NOW.minusSeconds(2 * 86_400),
                List.of(squatOnly()), null, null, NOW);
        WorkoutSession newest = WorkoutSession.log(owner, NOW.minusSeconds(86_400),
                List.of(squatOnly()), null, null, NOW);
        repository.save(oldest);
        repository.save(middle);
        repository.save(newest);

        List<WorkoutSession> firstPage = repository.findByOwner(owner, 0, 2);

        assertThat(repository.countByOwner(owner)).isEqualTo(3);
        assertThat(firstPage).extracting(WorkoutSession::id)
                .containsExactly(newest.id(), middle.id()); // DESC, page de taille 2
        assertThat(repository.findByOwner(owner, 1, 2)).extracting(WorkoutSession::id)
                .containsExactly(oldest.id());
    }

    private static LoggedExercise squatOnly() {
        return new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(ExerciseSet.external(5, Weight.ofKilograms(100), RPE.of(8.0))));
    }
}
