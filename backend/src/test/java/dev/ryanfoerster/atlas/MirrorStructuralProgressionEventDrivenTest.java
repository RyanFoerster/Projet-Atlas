package dev.ryanfoerster.atlas;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
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
import dev.ryanfoerster.atlas.roster.api.AthleteStrengthCeiling;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * <strong>GATE CONCEPTUEL du sprint 6</strong> — la preuve rigoureuse et permanente que les <strong>trois
 * échelles de temps</strong> coexistent dans le système complet (event-driven, vrai PostgreSQL), pas seulement
 * dans la simulation pure de 3a. C'est le test qui valide tout le sprint.
 *
 * <p><strong>Horloge contrôlée</strong> par les {@code performedAt} des séances (le moteur est en lazy
 * compute, ADR-006 : aucune dépendance à l'heure réelle). On sérialise strictement les séances (on attend que
 * chacune soit consommée avant de logger la suivante) — sinon le traitement async pourrait réordonner les
 * events et la garde d'idempotence {@code acceptsStimulusAt} en rejetterait (limitation assumée sprint 4).
 *
 * <p>Deux phases :
 * <ol>
 *   <li><strong>Entraînement</strong> (12 semaines, 1 squat/sem à charge fixe) → le 1RM <em>progresse</em>
 *       (Couche 3) et reste <strong>borné par le plafond</strong> (stabilité de la boucle d'auto-régulation
 *       dans le système réel : 1RM↑ → %1RM↓ → stimulus↓, amortissant, pas divergent).</li>
 *   <li><strong>Repos</strong> (6 semaines, aucune séance) → la <em>forme</em> (fitness Banister) <strong>
 *       baisse</strong> (décroissance τ=42j), MAIS le 1RM <strong>reste exactement inchangé</strong> (cliquet
 *       — aucun {@code CurrentStatsProgressed} émis au repos). C'est la distinction court terme / structurel,
 *       rendue mécanique.</li>
 * </ol>
 */
class MirrorStructuralProgressionEventDrivenTest extends AbstractIntegrationTest {

    private static final Instant START = Instant.parse("2026-01-05T17:00:00Z"); // un lundi
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();
    private static final double BODYWEIGHT_KG = 80.0;
    private static final double START_SQUAT_KG = 140.0;
    private static final int TRAINING_WEEKS = 12;
    private static final int REST_WEEKS = 6;

    @Autowired
    private LogWorkoutUseCase logWorkout;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RosterQueryPort rosterQueryPort;
    @Autowired
    private AthleteConditionRepository conditionRepository;
    @Autowired
    private BanisterModel banisterModel;

    @Test
    void over_weeks_the_mirror_one_rep_max_progresses_then_rest_drops_form_but_holds_the_one_rep_max() {
        UserId owner = createOwnerWithMirror();
        AthleteId mirrorId = mirror(owner).id();
        double ceiling = ceilingKg(mirrorId);
        assertThat(squatKg(mirrorId)).isEqualTo(START_SQUAT_KG);

        // --- Phase 1 : 12 semaines d'entraînement (1 squat/sem, charge fixe 120 kg) ---
        for (int week = 1; week <= TRAINING_WEEKS; week++) {
            Instant performedAt = START.plus(Duration.ofDays(7L * week));
            logWorkout.logWorkout(owner, squatSession(performedAt));
            awaitConditionAppliedAt(mirrorId, performedAt); // sérialise : la séance N est consommée avant la N+1
        }
        // La dernière progression doit se propager jusqu'à Roster (2ᵉ hop async : CurrentStatsProgressed).
        await().atMost(10, TimeUnit.SECONDS).until(() -> squatKg(mirrorId) > START_SQUAT_KG + 1.0);

        double trainedOneRm = squatKg(mirrorId);
        AthleteCondition trained = conditionRepository.findByAthleteId(mirrorId).orElseThrow();
        double trainedFitness = trained.state().totalFitness();

        // PROGRESSION + STABILITÉ : le 1RM a monté ET reste sous le plafond (asymptote — pas de divergence).
        assertThat(trainedOneRm).isGreaterThan(START_SQUAT_KG + 1.0);
        assertThat(trainedOneRm).isLessThan(ceiling);

        // --- Phase 2 : 6 semaines de repos (aucune séance loggée) ---
        Instant restEnd = START.plus(Duration.ofDays(7L * (TRAINING_WEEKS + REST_WEEKS)));
        double restedFitness = trained.projectedTo(banisterModel, restEnd).totalFitness(); // lazy compute, pas d'horloge réelle

        System.out.printf("=== GATE conceptuel sprint 6 (squat, départ %.0f, plafond %.1f) ===%n", START_SQUAT_KG, ceiling);
        System.out.printf("  Entraînement %d sem (charge fixe 120 kg) : 1RM %.0f → %.2f kg (+%.2f), borné < plafond ✓%n",
                TRAINING_WEEKS, START_SQUAT_KG, trainedOneRm, trainedOneRm - START_SQUAT_KG);
        System.out.printf("  Repos %d sem : fitness %.3f → %.3f (forme BAISSE), 1RM reste %.2f (cliquet) ✓%n",
                REST_WEEKS, trainedFitness, restedFitness, squatKg(mirrorId));

        // FORME BAISSE : la fitness (court terme, τ=42j) s'érode sans stimulus.
        assertThat(restedFitness).isLessThan(trainedFitness);
        // 1RM RESTE : aucun event au repos → le 1RM matérialisé est EXACTEMENT inchangé (cliquet).
        assertThat(squatKg(mirrorId)).isEqualTo(trainedOneRm);
    }

    /** Une séance squat à charge fixe (5×5 @ 120 kg externe, RPE 8) — la boucle d'auto-régulation est exercée. */
    private static LogWorkoutCommand squatSession(Instant performedAt) {
        LoggedExercise squat = new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(ExerciseSet.external(5, Weight.ofKilograms(120), RPE.of(8.0)),
                        ExerciseSet.external(5, Weight.ofKilograms(120), RPE.of(8.0)),
                        ExerciseSet.external(5, Weight.ofKilograms(120), RPE.of(8.0)),
                        ExerciseSet.external(5, Weight.ofKilograms(120), RPE.of(8.0)),
                        ExerciseSet.external(5, Weight.ofKilograms(120), RPE.of(8.0))));
        return new LogWorkoutCommand(performedAt, 60, null, List.of(squat));
    }

    private void awaitConditionAppliedAt(AthleteId mirrorId, Instant performedAt) {
        await().atMost(10, TimeUnit.SECONDS).until(() -> conditionRepository.findByAthleteId(mirrorId)
                .map(c -> c.state().lastUpdated().equals(performedAt))
                .orElse(false));
    }

    private UserId createOwnerWithMirror() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), START));
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(BODYWEIGHT_KG), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(START_SQUAT_KG)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(user.id(), START).addMirror(request, GENERATOR, 42L, START));
        return user.id();
    }

    private Roster roster(UserId owner) {
        return rosterRepository.findByOwnerId(owner).orElseThrow();
    }

    private dev.ryanfoerster.atlas.roster.domain.model.Athlete mirror(UserId owner) {
        return roster(owner).mirrorAthlete().orElseThrow();
    }

    private double squatKg(AthleteId mirrorId) {
        return rosterRepository.findByAthleteId(mirrorId).orElseThrow()
                .findAthlete(mirrorId).orElseThrow()
                .currentOneRepMax(MovementPattern.SQUAT).orElseThrow()
                .weight().toKilograms().doubleValue();
    }

    private double ceilingKg(AthleteId mirrorId) {
        AthleteStrengthCeiling ceiling = rosterQueryPort.findStrengthCeiling(mirrorId).orElseThrow();
        return ceiling.ceilingOneRmKg(MovementPattern.SQUAT);
    }
}
