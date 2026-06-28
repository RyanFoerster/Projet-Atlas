package dev.ryanfoerster.atlas.athletics.calibration;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.MuscleCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.MuscleStimulusMapping;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scénario de calibration <strong>par muscle</strong> (ADR-004, ADR-028, ADR-029) : 12 semaines, 4
 * séances/semaine, deload en semaine 7. Validation de modèle par simulation longue : on vérifie la
 * <strong>forme</strong> de la courbe agrégée (la fitness se construit lentement, la fatigue yo-yote, la
 * supercompensation apparaît après le deload), pas une valeur ponctuelle.
 *
 * <p>Le test <strong>imprime</strong> : (1) la trajectoire hebdomadaire agrégée ; (2) un détail par muscle ;
 * (3) la comparaison <em>indice agrégé par somme</em> vs <em>maillon-faible (min)</em> — pour trancher
 * l'agrégation au GATE 1 ; (4) l'observation sur la <em>convexité de effort(rpe)</em>. Les assertions ne
 * capturent que les invariants robustes (indépendants de {@code NORMALIZATION}).
 */
class BanisterCalibrationSimulationTest {

    private static final Instant START = Instant.parse("2026-01-05T17:00:00Z"); // un lundi 17:00
    private static final int WEEKS = 12;
    private static final int DELOAD_WEEK = 7; // 1-indexé
    private static final int[] TRAINING_DAYS = {0, 1, 3, 4}; // lun, mar, jeu, ven

    private static final GeneticModifiers NEUTRAL = GeneticModifiers.NEUTRAL;
    private final BanisterModel model = new BanisterModel();
    private final StimulusCalculator calculator = new StimulusCalculator();
    private final MuscleStimulusMapping mapping = new MuscleStimulusMapping();

    private record WeeklyPoint(int week, double fitness, double fatigue, double performance,
                               int indexSum, int indexWeakestLink) {
    }

    @Test
    void twelve_week_program_shows_a_credible_per_muscle_curve_with_supercompensation_after_deload() {
        List<WeeklyPoint> trajectory = run12Weeks(NEUTRAL);

        printTrajectory(trajectory);
        printPerMuscleDetail();
        printAsymmetricSplitObservation();
        printConvexityObservation();
        printGeneticIndividualizationObservation();

        // --- Invariants robustes (indépendants de NORMALIZATION, qui n'est qu'une échelle verticale) ---
        WeeklyPoint week4 = trajectory.get(3);
        WeeklyPoint preDeload = trajectory.get(5);  // semaine 6, fin de bloc d'accumulation
        WeeklyPoint deload = trajectory.get(6);     // semaine 7, deload
        WeeklyPoint postDeload = trajectory.get(7);  // semaine 8, juste après deload
        WeeklyPoint week12 = trajectory.get(11);

        // 1) Construction long terme : la fitness agrégée monte sur la durée.
        assertThat(week12.fitness()).isGreaterThan(week4.fitness());

        // 2) Fatigue rapide : le deload (peu de stimulus) fait chuter la fatigue vs une semaine dure.
        assertThat(deload.fatigue()).isLessThan(preDeload.fatigue());

        // 3) SUPERCOMPENSATION : la performance agrégée après le deload dépasse celle d'avant le deload.
        assertThat(postDeload.performance()).isGreaterThan(preDeload.performance());

        // 4) Signature τ_fatigue ≪ τ_fitness : pendant le deload, la fatigue s'efface BEAUCOUP plus,
        //    en relatif, que la fitness (distinction court terme / structurel — les CurrentStats ne bougent pas).
        double fitnessRelativeDrop = (preDeload.fitness() - deload.fitness()) / preDeload.fitness();
        double fatigueRelativeDrop = (preDeload.fatigue() - deload.fatigue()) / preDeload.fatigue();
        assertThat(fatigueRelativeDrop).isGreaterThan(fitnessRelativeDrop);
    }

    @Test
    void genetic_individualization_produces_coherent_different_progressions() {
        // Même programme 12 semaines, génétiques différentes → progressions différentes et cohérentes (GATE 3).
        List<WeeklyPoint> fastRecovery = run12Weeks(new GeneticModifiers(1.20, 1.0));
        List<WeeklyPoint> slowRecovery = run12Weeks(new GeneticModifiers(0.85, 1.0));
        // Haut recovery → fatigue (τ7) décroît plus vite → supercompense plus (perf post-deload plus haute).
        assertThat(fastRecovery.get(7).performance()).isGreaterThan(slowRecovery.get(7).performance());

        List<WeeklyPoint> strongResponder = run12Weeks(new GeneticModifiers(1.0, 1.15));
        List<WeeklyPoint> weakResponder = run12Weeks(new GeneticModifiers(1.0, 0.85));
        // Fort répondeur → impulsion plus grande → plus de fitness construite à terme.
        assertThat(strongResponder.get(11).fitness()).isGreaterThan(weakResponder.get(11).fitness());
    }

    /** Joue le programme 12 semaines (4 séances/sem, deload S7) pour une génétique donnée. */
    private List<WeeklyPoint> run12Weeks(GeneticModifiers genetics) {
        FitnessFatigueState state = FitnessFatigueState.initial(START.minus(Duration.ofDays(1)));
        List<WeeklyPoint> trajectory = new ArrayList<>();
        for (int week = 1; week <= WEEKS; week++) {
            Instant weekStart = START.plus(Duration.ofDays((long) (week - 1) * 7));
            List<ExerciseStimulus> session = (week == DELOAD_WEEK) ? deloadSession() : hardSession();
            for (int dayOffset : TRAINING_DAYS) {
                Map<MuscleGroup, TrainingStimulus> distributed = calculator.distribute(session, mapping);
                state = model.applyStimulus(state, distributed, genetics, weekStart.plus(Duration.ofDays(dayOffset)));
            }
            // Snapshot hebdo au dimanche (2 jours après la dernière séance — un point « reposé » comparable).
            FitnessFatigueState snapshot = model.decayedTo(state, genetics, weekStart.plus(Duration.ofDays(6)));
            trajectory.add(new WeeklyPoint(week, snapshot.totalFitness(), snapshot.totalFatigue(),
                    model.availablePerformance(snapshot), aggregateIndexBySum(snapshot), weakestLinkIndex(snapshot)));
        }
        return trajectory;
    }

    private void printGeneticIndividualizationObservation() {
        System.out.println("--- GATE 3 : individualisation génétique (même programme 12 sem) ---");
        printGeneticRow("recovery 1.20 (récupérateur)", new GeneticModifiers(1.20, 1.0));
        printGeneticRow("recovery 0.85 (lent)        ", new GeneticModifiers(0.85, 1.0));
        printGeneticRow("sensitivity 1.15 (fort rép.)", new GeneticModifiers(1.0, 1.15));
        printGeneticRow("sensitivity 0.85 (faible)   ", new GeneticModifiers(1.0, 0.85));
        System.out.println();
    }

    private void printGeneticRow(String label, GeneticModifiers genetics) {
        List<WeeklyPoint> t = run12Weeks(genetics);
        WeeklyPoint pre = t.get(5), post = t.get(7), end = t.get(11);
        System.out.printf("  %s : ΣFitness(S12)=%6.2f  perf pré-deload=%5.2f  post-deload=%5.2f  Δsuper=%+.2f%n",
                label, end.fitness(), pre.performance(), post.performance(), post.performance() - pre.performance());
    }

    /** Séance « dure » : 2 composés (squat→quads, bench→chest) + 2 accessoires (biceps, triceps). */
    private static List<ExerciseStimulus> hardSession() {
        return List.of(
                ExerciseStimulus.compound(MovementPattern.SQUAT, sets(4, 5, 8.0)),
                ExerciseStimulus.compound(MovementPattern.BENCH_PRESS, sets(4, 5, 8.0)),
                ExerciseStimulus.accessory(BodyRegion.BICEPS, sets(3, 10, 7.5)),
                ExerciseStimulus.accessory(BodyRegion.TRICEPS, sets(3, 12, null)));
    }

    /** Semaine de deload : volume et intensité fortement réduits, composés seulement. */
    private static List<ExerciseStimulus> deloadSession() {
        return List.of(
                ExerciseStimulus.compound(MovementPattern.SQUAT, sets(2, 5, 6.0)),
                ExerciseStimulus.compound(MovementPattern.BENCH_PRESS, sets(2, 5, 6.0)));
    }

    private static List<SetEffort> sets(int count, int reps, Double rpe) {
        List<SetEffort> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new SetEffort(reps, rpe));
        }
        return list;
    }

    // --- Règles d'agrégation candidates (la décision est tranchée à l'œil sur l'impression) ---

    /** Indice agrégé par SOMME (arbitrage ② proposé) : 50 + 50·(Σperf/Σfitness). */
    private int aggregateIndexBySum(FitnessFatigueState state) {
        return formIndex(state.totalFitness(), model.availablePerformance(state));
    }

    /** Indice « maillon faible » (alternative) : le plus petit indice parmi les muscles travaillés. */
    private int weakestLinkIndex(FitnessFatigueState state) {
        return state.byMuscle().values().stream()
                .mapToInt(mc -> formIndex(mc.fitness(), model.availablePerformance(mc)))
                .min().orElse(50);
    }

    private static int formIndex(double fitness, double performance) {
        if (fitness < 1e-9) {
            return 50;
        }
        return (int) Math.clamp(Math.round(50 + 50 * (performance / fitness)), 0, 100);
    }

    private void printPerMuscleDetail() {
        // Une séance dure isolée sur un athlète frais, vue à J+2 (reposé) : montre la distribution par muscle.
        FitnessFatigueState fresh = FitnessFatigueState.initial(START);
        fresh = model.applyStimulus(fresh, calculator.distribute(hardSession(), mapping), NEUTRAL, START);
        FitnessFatigueState rested = model.decayedTo(fresh, NEUTRAL, START.plus(Duration.ofDays(2)));
        System.out.println("--- Détail par muscle (1 séance dure, vue à J+2) ---");
        rested.byMuscle().forEach((muscle, mc) -> System.out.printf(
                "  %-11s fitness=%.3f fatigue=%.3f  indice=%d%n",
                muscle, mc.fitness(), mc.fatigue(), formIndex(mc.fitness(), model.availablePerformance(mc))));
        System.out.println();
    }

    /**
     * GATE 2 — agrégation SOMME vs MAILLON-FAIBLE sous asymétrie <strong>TEMPORELLE</strong>. Insight : dans
     * une seule séance, tous les muscles partagent le même ratio fitness/fatigue (même τ, partis de
     * fitness==fatigue) → même indice → somme == maillon-faible. La divergence exige des muscles entraînés à
     * des <em>moments différents</em>. Scénario : jambes cuites J0, bras frais J+5, vu à J+6 → les jambes ont
     * récupéré (fatigue τ7 effacée → frais), les bras sont encore cuits.
     */
    private void printAsymmetricSplitObservation() {
        FitnessFatigueState s = FitnessFatigueState.initial(START);
        s = model.applyStimulus(s, calculator.distribute(            // J0 : journée jambes lourde
                List.of(ExerciseStimulus.compound(MovementPattern.SQUAT, sets(5, 5, 9.0))), mapping), NEUTRAL, START);
        Instant armDay = START.plus(Duration.ofDays(5));
        s = model.applyStimulus(s, calculator.distribute(            // J+5 : bras (curls), frais
                List.of(ExerciseStimulus.accessory(BodyRegion.BICEPS, sets(4, 12, 9.0))), mapping), NEUTRAL, armDay);
        FitnessFatigueState observed = model.decayedTo(s, NEUTRAL, START.plus(Duration.ofDays(6))); // J+6

        System.out.println("--- GATE 2 : agrégation sous asymétrie TEMPORELLE (jambes J0 cuites, bras J+5, vu J+6) ---");
        observed.byMuscle().forEach((muscle, mc) -> System.out.printf("  %-11s indice=%d%n",
                muscle, formIndex(mc.fitness(), model.availablePerformance(mc))));
        System.out.printf("  → IndiceSomme=%d   IndiceMaillonFaible=%d%n",
                aggregateIndexBySum(observed), weakestLinkIndex(observed));
        System.out.println("  Lecture : maillon-faible = les bras encore cuits (J+1) ; somme = mélange pondéré.");
        System.out.println("  La somme dit \"globalement OK\", le maillon-faible dit \"tes bras sont morts\".");
        System.out.println();
    }

    private void printConvexityObservation() {
        // GATE 2 — réévaluation effort(rpe) : linéaire (actuel) vs candidats. Sur un éventail de RPE.
        System.out.println("--- GATE 2 : observation convexité effort(rpe) ---");
        System.out.println("  RPE | linéaire rpe/10 | candidat (rpe-4)/6 clampé | repli (rpe/10)^1.5");
        for (double rpe : new double[]{3, 4, 5, 6, 7, 8, 9, 10}) {
            System.out.printf("  %4.0f |      %.2f       |          %.2f             |       %.2f%n",
                    rpe, rpe / 10.0, Math.clamp((rpe - 4) / 6.0, 0.0, 1.0), Math.pow(rpe / 10.0, 1.5));
        }
        System.out.println("  Lecture : le candidat (rpe-4)/6 met les warmups (RPE<=4) à 0 et durcit la pente ;");
        System.out.println("  le linéaire donne 0.40 à un warmup RPE 4 (sur-crédité). Décision à trancher ici.");
        System.out.println();
    }

    private static void printTrajectory(List<WeeklyPoint> trajectory) {
        System.out.println();
        System.out.printf("=== Simulation Banister PAR MUSCLE %d sem — 4 séances/sem, deload S%d ===%n", WEEKS, DELOAD_WEEK);
        System.out.printf("τ_fitness=%.0fj  τ_fatigue=%.0fj  k1=%.1f  k2=%.1f  NORM=%.4f%n",
                BanisterModel.TAU_FITNESS_DAYS, BanisterModel.TAU_FATIGUE_DAYS,
                BanisterModel.K1, BanisterModel.K2, StimulusCalculator.NORMALIZATION);
        System.out.println("Sem | ΣFitness | ΣFatigue | ΣPerf  | IndiceSomme | IndiceMaillonFaible");
        for (WeeklyPoint p : trajectory) {
            String marker = (p.week() == DELOAD_WEEK) ? "  <- deload" : "";
            System.out.printf(" %2d | %8.3f | %8.3f | %6.3f |     %3d     |        %3d%s%n",
                    p.week(), p.fitness(), p.fatigue(), p.performance(), p.indexSum(), p.indexWeakestLink(), marker);
        }
        System.out.println();
    }
}
