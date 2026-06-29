package dev.ryanfoerster.atlas;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
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
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Test end-to-end de la boucle event-driven du sprint 4 (GATE 2) : PersonalTraining logge une séance →
 * publie {@code WorkoutLogged} → <strong>Athletics</strong> la consomme en async → la <em>forme</em>
 * (Banister) de l'athlète miroir évolue, un snapshot est créé. Utilise l'API {@code Scenario} de Modulith
 * (attente de la consommation asynchrone).
 *
 * <p><strong>Sprint 6, Couche 3</strong> : le 1RM ne reste plus « inchangé » — une séance le fait désormais
 * <em>progresser</em> (ADR-033/032). La distinction court/long terme (forme yo-yote vs 1RM cliquet) est
 * désormais portée par le <strong>gate conceptuel</strong> ({@code MirrorStructuralProgressionEventDrivenTest}),
 * qui attend correctement la propagation jusqu'à Roster. Ce test-ci reste focalisé sur la <em>forme</em>.
 */
@EnableScenarios
class MirrorConditionEventDrivenTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final Instant PERFORMED_AT = Instant.parse("2026-06-23T18:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();
    // squat 5×1 @ RPE 8 + curl 12×1 sans RPE (effort neutre), magnitude dérivée de la formule (GATE 2).
    // Squat 140 kg pour un 1RM de 140 → %1RM = 1.0 → loadFactor plafond (1.0). Curl = accessoire (pas de 1RM)
    // → loadFactor plancher. La charge entre dans le stimulus (Couche 2, ADR-034).
    private static final double EXPECTED_STIMULUS = StimulusCalculator.NORMALIZATION
            * (5 * StimulusCalculator.effortFactor(8.0) * StimulusCalculator.loadFactor(140.0 / 140.0)
            + 12 * StimulusCalculator.effortFactor(null) * StimulusCalculator.loadFactor(null));

    @Autowired
    private LogWorkoutUseCase logWorkout;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AthleteConditionRepository conditionRepository;
    @Autowired
    private ConditionSnapshotRepository snapshotRepository;

    @Test
    void logging_a_workout_evolves_the_mirror_form(Scenario scenario) {
        UserId owner = createOwnerWithMirror();
        AthleteId mirrorId = mirror(owner).id();

        scenario.stimulate(() -> logWorkout.logWorkout(owner, command()))
                .andWaitForStateChange(() -> conditionRepository.findByAthleteId(mirrorId).orElse(null))
                .andVerify(condition -> {
                    // La forme du miroir a évolué : même impulsion sur fitness ET fatigue (décroissance nulle
                    // à l'instant de la séance) → fitness == fatigue == stimulus.
                    assertThat(condition).isNotNull();
                    // Agrégat = somme des muscles travaillés (squat → quads, curl → biceps), modulé par la
                    // génétique. La séance est fraîche (décroissance nulle) → fitness == fatigue.
                    assertThat(condition.state().totalFitness()).isEqualTo(condition.state().totalFatigue());
                    assertThat(condition.state().lastUpdated()).isEqualTo(PERFORMED_AT);
                    // INDIVIDUALISATION GÉNÉTIQUE (Couche 3) : les modifiers dénormalisés viennent bien de la
                    // Genetics réelle du miroir (résolus via le port Roster à la création de la condition).
                    Athlete m = mirror(owner);
                    assertThat(condition.geneticModifiers().recoveryRate())
                            .isEqualTo(m.genetics().baseRecoveryRate());
                    assertThat(condition.geneticModifiers().stimulusMultiplier())
                            .isEqualTo(m.genetics().trainingResponseSensitivity());
                    // L'impulsion reflète le multiplicateur génétique sur le stimulus de base.
                    assertThat(condition.state().totalFitness())
                            .isCloseTo(EXPECTED_STIMULUS * m.genetics().trainingResponseSensitivity(), within(1e-9));
                });

        // Un snapshot a été capturé (futur sprint 7), daté de la séance, performance « cuite » (négative).
        List<ConditionSnapshot> snapshots = snapshotRepository.findByAthleteId(mirrorId);
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().takenAt()).isEqualTo(PERFORMED_AT);
        assertThat(snapshots.getFirst().performance()).isNegative();
        // (La progression du 1RM déclenchée par cette séance est prouvée par le gate conceptuel, qui attend
        //  la propagation jusqu'à Roster — ici on ne valide que la forme.)
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

    private static LogWorkoutCommand command() {
        LoggedExercise squat = new LoggedExercise(ExerciseName.of("Back Squat"),
                ExerciseCategory.compound(MovementPattern.SQUAT),
                List.of(ExerciseSet.external(5, Weight.ofKilograms(140), RPE.of(8.0))));
        LoggedExercise curl = new LoggedExercise(ExerciseName.of("Barbell Curl"),
                ExerciseCategory.accessory(BodyRegion.BICEPS),
                List.of(ExerciseSet.external(12, Weight.ofKilograms(20), null)));
        return new LogWorkoutCommand(PERFORMED_AT, 60, null, List.of(squat, curl));
    }

    private Athlete mirror(UserId owner) {
        return rosterRepository.findByOwnerId(owner).orElseThrow().mirrorAthlete().orElseThrow();
    }
}
