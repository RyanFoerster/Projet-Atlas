package dev.ryanfoerster.atlas.athletics.calibration;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.PatternStrengthRef;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.StructuralProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.athletics.domain.service.StructuralProgressionModel;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scénario de calibration de la <strong>progression structurelle du 1RM</strong> (Couche 3, ADR-033) — le
 * GATE 3a. Simulation longue (16 semaines, squat 3×/sem, surcharge progressive à ~82,5 % du 1RM courant),
 * validée à l'œil de lifter sur les <em>ordres de grandeur</em> et la <em>stabilité de la boucle</em> :
 *
 * <ul>
 *   <li><strong>Débutant</strong> (loin du plafond) : +15-20 kg de squat sur 12 sem (newbie gains réels) ;</li>
 *   <li><strong>Avancé</strong> (proche du plafond) : quelques kg — rendement décroissant <em>émergent</em>
 *       de l'écart au plafond, pas codé ;</li>
 *   <li><strong>Spread génétique</strong> : à départ égal, une forte {@code strengthAffinity} (plafond plus
 *       haut) gagne nettement plus ;</li>
 *   <li><strong>Stabilité</strong> : la trajectoire <em>monte puis plafonne</em> — monotone non-décroissante,
 *       bornée par le plafond (asymptote), jamais de divergence ni d'étouffement ;</li>
 *   <li><strong>Cliquet</strong> : un repos prolongé laisse le 1RM <em>inchangé</em> (la forme baisserait,
 *       le 1RM non — distinction CurrentStats / Fitness rendue mécanique par le {@code max}).</li>
 * </ul>
 *
 * <p>Le test <strong>imprime</strong> les trajectoires hebdomadaires ; les assertions ne capturent que des
 * invariants robustes (fourchettes larges, monotonie, bornes) — la valeur exacte du plateau à volume
 * constant est un curseur Atlas (ADR-033 §3), pas une vérité physiologique.
 */
class StructuralProgressionCalibrationTest {

    private static final Instant START = Instant.parse("2026-01-05T17:00:00Z"); // un lundi
    private static final double BODYWEIGHT = 80.0;
    private static final double SQUAT_ELITE_RATIO = 2.3; // borne élite du standard (ProceduralAthleteGenerator)
    private static final int[] TRAINING_DAYS = {0, 2, 4}; // lun, mer, ven

    private final StimulusCalculator calculator = new StimulusCalculator();
    private final StructuralProgressionModel model = new StructuralProgressionModel();

    @Test
    void newbie_gains_and_advanced_grind_emerge_from_the_gap_to_the_genetic_ceiling() {
        double beginnerCeiling = BODYWEIGHT * SQUAT_ELITE_RATIO * 1.00; // 184
        double advancedCeiling = BODYWEIGHT * SQUAT_ELITE_RATIO * 1.10; // 202

        List<Double> beginner = runSquatProgram(100.0, beginnerCeiling, 16, 16); // pas de repos
        List<Double> advanced = runSquatProgram(170.0, advancedCeiling, 16, 16);

        printTrajectory("Débutant (squat 100, plafond 184)", beginner, beginnerCeiling);
        printTrajectory("Avancé   (squat 170, plafond 202)", advanced, advancedCeiling);

        double beginnerGain12 = beginner.get(11) - 100.0;
        double advancedGain12 = advanced.get(11) - 170.0;

        // 1) Ordres de grandeur (œil de lifter) : débutant +15-20, avancé quelques kg.
        assertThat(beginnerGain12).isBetween(15.0, 22.0);
        assertThat(advancedGain12).isBetween(3.0, 11.0);
        // 2) Rendement décroissant émergent : le débutant gagne nettement plus que l'avancé (même SCALE).
        assertThat(beginnerGain12).isGreaterThan(advancedGain12 * 1.8);
        // 3) Stabilité : monotone non-décroissante, bornée par le plafond (asymptote).
        assertMonotoneAndBounded(beginner, beginnerCeiling);
        assertMonotoneAndBounded(advanced, advancedCeiling);
    }

    @Test
    void genetic_strength_affinity_widens_the_progression_at_equal_starting_strength() {
        double lowCeiling = BODYWEIGHT * SQUAT_ELITE_RATIO * 0.90;  // 165.6
        double highCeiling = BODYWEIGHT * SQUAT_ELITE_RATIO * 1.20; // 220.8

        List<Double> low = runSquatProgram(120.0, lowCeiling, 16, 16);
        List<Double> high = runSquatProgram(120.0, highCeiling, 16, 16);

        printTrajectory("Affinité 0.90 (plafond 166)", low, lowCeiling);
        printTrajectory("Affinité 1.20 (plafond 221)", high, highCeiling);

        double lowGain = low.get(11) - 120.0;
        double highGain = high.get(11) - 120.0;
        // À départ égal, le plafond plus haut (génétique forte) creuse l'écart — la génétique se sent fort.
        assertThat(highGain).isGreaterThan(lowGain * 1.8);
        assertMonotoneAndBounded(low, lowCeiling);
        assertMonotoneAndBounded(high, highCeiling);
    }

    @Test
    void cliquet_holds_the_one_rep_max_through_a_long_rest() {
        double ceiling = BODYWEIGHT * SQUAT_ELITE_RATIO; // 184
        // 16 semaines, mais repos à partir de la semaine 13 (index 12) : 12 semaines d'entraînement puis 4 de repos.
        List<Double> trajectory = runSquatProgram(100.0, ceiling, 16, 12);

        printTrajectory("Cliquet : repos S13-16 (plafond 184)", trajectory, ceiling);

        double atRestStart = trajectory.get(11); // fin semaine 12 (dernière semaine d'entraînement)
        double atEnd = trajectory.get(15);        // fin semaine 16 (après 4 semaines de repos)
        // La forme (Banister) chuterait pendant le repos ; le 1RM, lui, ne bouge PAS (cliquet = max).
        assertThat(atEnd).isEqualTo(atRestStart);
    }

    /**
     * Joue un programme squat sur {@code weeks} semaines (3 séances/sem, 5×5 @ ~82,5 % du 1RM courant), avec
     * le cliquet : à chaque séance, le 1RM courant devient {@code max(1RM, mérité)}. {@code restFromWeek} =
     * première semaine (0-indexée) sans entraînement. Retourne le 1RM au dimanche de chaque semaine.
     */
    private List<Double> runSquatProgram(double start, double ceiling, int weeks, int restFromWeek) {
        StructuralProgress progress = StructuralProgress.EMPTY;
        double currentOneRm = start;
        Instant lastApplied = START.minus(Duration.ofDays(1));
        List<Double> weekly = new ArrayList<>();

        for (int day = 0; day < weeks * 7; day++) {
            Instant now = START.plus(Duration.ofDays(day));
            boolean resting = (day / 7) >= restFromWeek;
            if (!resting && isTrainingDay(day % 7)) {
                double workingPct = 0.825; // surcharge progressive : on charge ~82,5 % du 1RM COURANT
                List<ExerciseStimulus> session = List.of(
                        ExerciseStimulus.compound(MovementPattern.SQUAT, sets(5, 5, 8.0, workingPct)));
                Map<MovementPattern, TrainingStimulus> byPattern = calculator.byPattern(session);
                Map<MovementPattern, PatternStrengthRef> refs =
                        Map.of(MovementPattern.SQUAT, new PatternStrengthRef(currentOneRm, ceiling));
                progress = model.advance(progress, byPattern, refs, lastApplied, now);
                lastApplied = now;
                double merited = model.meritedOneRmKg(progress.progress(MovementPattern.SQUAT).orElseThrow());
                currentOneRm = Math.max(currentOneRm, merited); // CLIQUET
            }
            if (day % 7 == 6) {
                weekly.add(currentOneRm);
            }
        }
        return weekly;
    }

    private static boolean isTrainingDay(int dayOfWeek) {
        for (int d : TRAINING_DAYS) {
            if (d == dayOfWeek) {
                return true;
            }
        }
        return false;
    }

    private static List<SetEffort> sets(int count, int reps, Double rpe, Double percentOneRepMax) {
        List<SetEffort> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new SetEffort(reps, rpe, percentOneRepMax));
        }
        return list;
    }

    private static void assertMonotoneAndBounded(List<Double> weekly, double ceiling) {
        for (int i = 1; i < weekly.size(); i++) {
            assertThat(weekly.get(i)).isGreaterThanOrEqualTo(weekly.get(i - 1)); // jamais à la baisse (cliquet)
            assertThat(weekly.get(i)).isLessThan(ceiling);                        // borné par le plafond
        }
    }

    private static void printTrajectory(String label, List<Double> weekly, double ceiling) {
        System.out.printf("=== %s  [SCALE=%.0f τ=%.0fj plafond=%.0f] ===%n",
                label, StructuralProgressionModel.SCALE, StructuralProgressionModel.TAU_CHRONIC_DAYS, ceiling);
        StringBuilder line = new StringBuilder("  1RM/sem:");
        for (Double w : weekly) {
            line.append(String.format(" %.0f", w));
        }
        System.out.println(line);
    }
}
