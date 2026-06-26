package dev.ryanfoerster.atlas.athletics.calibration;

import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scénario de calibration (ADR-004, ADR-028) : 12 semaines, 4 séances/semaine, <strong>deload en
 * semaine 7</strong>. Ce n'est pas un test unitaire mais une <em>validation de modèle par simulation
 * longue</em> : on vérifie la <strong>forme</strong> de la courbe (la fitness se construit lentement, la
 * fatigue yo-yote, la supercompensation apparaît après le deload), pas une valeur ponctuelle.
 *
 * <p>Le test <strong>imprime la trajectoire hebdomadaire</strong> (fitness / fatigue / performance) pour
 * qu'on juge la courbe à l'œil au GATE 1 et qu'on calibre {@code NORMALIZATION} (et, si besoin, la forme
 * de {@code effort(rpe)}). Les assertions ci-dessous ne capturent que les invariants robustes du modèle.
 */
class BanisterCalibrationSimulationTest {

    private static final Instant START = Instant.parse("2026-01-05T17:00:00Z"); // un lundi 17:00
    private static final int WEEKS = 12;
    private static final int DELOAD_WEEK = 7; // 1-indexé
    private static final int[] TRAINING_DAYS = {0, 1, 3, 4}; // lun, mar, jeu, ven

    private final BanisterModel model = new BanisterModel();
    private final StimulusCalculator calculator = new StimulusCalculator();

    private record WeeklyPoint(int week, double fitness, double fatigue, double performance) {
    }

    @Test
    void twelve_week_program_shows_a_credible_fitness_fatigue_curve_with_supercompensation_after_deload() {
        FitnessFatigueState state = FitnessFatigueState.initial(START.minus(Duration.ofDays(1)));
        List<WeeklyPoint> trajectory = new ArrayList<>();

        for (int week = 1; week <= WEEKS; week++) {
            Instant weekStart = START.plus(Duration.ofDays((long) (week - 1) * 7));
            List<SetEffort> session = (week == DELOAD_WEEK) ? deloadSession() : hardSession();

            for (int dayOffset : TRAINING_DAYS) {
                Instant sessionAt = weekStart.plus(Duration.ofDays(dayOffset));
                TrainingStimulus stimulus = calculator.from(session);
                state = model.applyStimulus(state, stimulus, sessionAt);
            }

            // Snapshot hebdo au dimanche (2 jours après la dernière séance — un point « reposé » comparable).
            Instant sundayRest = weekStart.plus(Duration.ofDays(6));
            FitnessFatigueState snapshot = model.decayedTo(state, sundayRest);
            trajectory.add(new WeeklyPoint(week, snapshot.fitness(), snapshot.fatigue(),
                    model.availablePerformance(snapshot)));
        }

        printTrajectory(trajectory);

        // --- Invariants robustes (indépendants de NORMALIZATION, qui n'est qu'une échelle verticale) ---
        WeeklyPoint week4 = trajectory.get(3);
        WeeklyPoint preDeload = trajectory.get(5);  // semaine 6, fin de bloc d'accumulation
        WeeklyPoint deload = trajectory.get(6);     // semaine 7, deload
        WeeklyPoint postDeload = trajectory.get(7);  // semaine 8, juste après deload
        WeeklyPoint week12 = trajectory.get(11);

        // 1) Construction long terme : la fitness monte sur la durée.
        assertThat(week12.fitness()).isGreaterThan(week4.fitness());

        // 2) Fatigue rapide : le deload (peu de stimulus) fait chuter la fatigue vs une semaine dure.
        assertThat(deload.fatigue()).isLessThan(preDeload.fatigue());

        // 3) SUPERCOMPENSATION : la performance après le deload dépasse celle d'avant le deload
        //    (la fatigue s'est effacée plus vite que la fitness).
        assertThat(postDeload.performance()).isGreaterThan(preDeload.performance());

        // 4) Signature τ_fatigue ≪ τ_fitness : pendant le deload, la fatigue s'efface BEAUCOUP plus,
        //    en relatif, que la fitness. C'est ce différentiel qui fait remonter la performance (et non
        //    une fitness « immobile » — la fitness de Banister est l'adaptation court terme, distincte
        //    des CurrentStats structurels qui, eux, ne bougent pas ce sprint).
        double fitnessRelativeDrop = (preDeload.fitness() - deload.fitness()) / preDeload.fitness();
        double fatigueRelativeDrop = (preDeload.fatigue() - deload.fatigue()) / preDeload.fatigue();
        assertThat(fatigueRelativeDrop).isGreaterThan(fitnessRelativeDrop);
    }

    /** Séance « dure » type : 2 composés lourds + 2 accessoires (dont un sans RPE). */
    private static List<SetEffort> hardSession() {
        List<SetEffort> sets = new ArrayList<>();
        addSets(sets, 4, 5, 8.0);   // composé 1 : 4×5 @ RPE 8
        addSets(sets, 4, 5, 8.0);   // composé 2 : 4×5 @ RPE 8
        addSets(sets, 3, 10, 7.5);  // accessoire 1 : 3×10 @ RPE 7.5
        addSets(sets, 3, 12, null); // accessoire 2 : 3×12, RPE non loggé (→ effort neutre)
        return sets;
    }

    /** Semaine de deload : volume et intensité fortement réduits. */
    private static List<SetEffort> deloadSession() {
        List<SetEffort> sets = new ArrayList<>();
        addSets(sets, 2, 5, 6.0); // composé 1 : 2×5 @ RPE 6
        addSets(sets, 2, 5, 6.0); // composé 2 : 2×5 @ RPE 6
        return sets;
    }

    private static void addSets(List<SetEffort> target, int count, int reps, Double rpe) {
        for (int i = 0; i < count; i++) {
            target.add(new SetEffort(reps, rpe));
        }
    }

    private static void printTrajectory(List<WeeklyPoint> trajectory) {
        System.out.println();
        System.out.printf("=== Simulation Banister %d semaines — 4 séances/sem, deload S%d ===%n", WEEKS, DELOAD_WEEK);
        System.out.printf("τ_fitness=%.0fj  τ_fatigue=%.0fj  k1=%.1f  k2=%.1f  NORM=%.4f%n",
                BanisterModel.TAU_FITNESS_DAYS, BanisterModel.TAU_FATIGUE_DAYS,
                BanisterModel.K1, BanisterModel.K2, StimulusCalculator.NORMALIZATION);
        System.out.println("Semaine | Fitness | Fatigue | Performance");
        for (WeeklyPoint p : trajectory) {
            String marker = (p.week() == DELOAD_WEEK) ? "  <- deload" : "";
            System.out.printf("  %2d    | %7.3f | %7.3f | %9.3f%s%n",
                    p.week(), p.fitness(), p.fatigue(), p.performance(), marker);
        }
        System.out.println();
    }
}
