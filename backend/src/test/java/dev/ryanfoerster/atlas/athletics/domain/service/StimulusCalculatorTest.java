package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests de la formule de stimulus : {@code S = NORM × Σ (reps × effort(rpe) × load(%1RM))}. {@code effort}
 * = seuil convexe doux {@code clamp((rpe−4)/6)} (GATE 2, ADR-031) ; {@code load} = rampe clampée-linéaire en
 * %1RM, plancher 0.40, plafond 1.0 à ≥90 % (sprint 6, ADR-034). Les séries sans %1RM (2-arg) tombent au
 * plancher de {@code load}. Magnitudes dérivées des fonctions {@code effortFactor}/{@code loadFactor} (robuste
 * au recalibrage de NORM).
 */
class StimulusCalculatorTest {

    private final StimulusCalculator calculator = new StimulusCalculator();
    private final MuscleStimulusMapping mapping = new MuscleStimulusMapping();

    @Test
    void effort_factor_is_a_soft_convex_threshold_zeroing_warmups() {
        assertThat(StimulusCalculator.effortFactor(4.0)).isZero();        // warmup : pas de stimulus
        assertThat(StimulusCalculator.effortFactor(3.0)).isZero();        // clampé en bas
        assertThat(StimulusCalculator.effortFactor(7.0)).isCloseTo(0.5, within(1e-9));
        assertThat(StimulusCalculator.effortFactor(8.0)).isCloseTo(2.0 / 3.0, within(1e-9));
        assertThat(StimulusCalculator.effortFactor(10.0)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void missing_rpe_uses_the_neutral_default_effort() {
        assertThat(StimulusCalculator.effortFactor(null))
                .isEqualTo(StimulusCalculator.effortFactor(StimulusCalculator.DEFAULT_EFFORT_RPE))
                .isCloseTo(0.5, within(1e-9)); // RPE 7 neutre → 0.5
    }

    @Test
    void load_factor_is_a_clamped_linear_ramp_floored_below_30pct() {
        assertThat(StimulusCalculator.loadFactor(0.30)).isCloseTo(0.40, within(1e-9)); // plancher
        assertThat(StimulusCalculator.loadFactor(0.20)).isCloseTo(0.40, within(1e-9)); // clampé sous le plancher
        assertThat(StimulusCalculator.loadFactor(0.60)).isCloseTo(0.70, within(1e-9)); // mi-rampe
        assertThat(StimulusCalculator.loadFactor(0.90)).isCloseTo(1.00, within(1e-9)); // plafond
        assertThat(StimulusCalculator.loadFactor(1.20)).isCloseTo(1.00, within(1e-9)); // PR : clampé au plafond
        assertThat(StimulusCalculator.loadFactor(null)).isCloseTo(0.40, within(1e-9)); // pas de 1RM → plancher
    }

    @Test
    void load_and_effort_are_orthogonal_no_double_counting_at_equal_rpe() {
        // Même RPE9 (même effortFactor), charges opposées : load les distingue → info réelle, pas de doublon.
        double light = calculator.from(List.of(new SetEffort(20, 9.0, 0.50))).magnitude(); // 20 reps @50%
        double heavy = calculator.from(List.of(new SetEffort(2, 9.0, 0.90))).magnitude();   // 2 reps @90%
        // À %1RM identique, le volume domine ; mais à RPE égal, la série lourde est créditée plus PAR REP :
        assertThat(20 * StimulusCalculator.loadFactor(0.50)).isGreaterThan(2 * StimulusCalculator.loadFactor(0.90));
        assertThat(light).isGreaterThan(heavy); // le volume l'emporte ici
        // load rabote la série légère (×0.60) sans toucher la lourde (×1.0) → ratio resserré vs sans charge.
        double ratioWithLoad = light / heavy;
        double ratioNoLoad = (20.0 * StimulusCalculator.effortFactor(9.0)) / (2.0 * StimulusCalculator.effortFactor(9.0));
        assertThat(ratioWithLoad).isLessThan(ratioNoLoad);
    }

    @Test
    void a_loaded_set_out_stimulates_an_unreferenced_one_at_equal_volume_and_effort() {
        double loaded = calculator.from(List.of(new SetEffort(5, 8.0, 0.85))).magnitude();   // composé lourd
        double floored = calculator.from(List.of(new SetEffort(5, 8.0))).magnitude();        // pas de 1RM → plancher
        assertThat(loaded).isGreaterThan(floored);
    }

    @Test
    void magnitude_is_the_normalized_sum_of_reps_times_effort_times_load() {
        List<SetEffort> sets = List.of(new SetEffort(5, 8.0), new SetEffort(5, null)); // 2-arg → load plancher
        double lf = StimulusCalculator.loadFactor(null);
        double rawExpected = (5 * StimulusCalculator.effortFactor(8.0) + 5 * StimulusCalculator.effortFactor(null)) * lf;

        TrainingStimulus stimulus = calculator.from(sets);

        assertThat(stimulus.magnitude()).isCloseTo(StimulusCalculator.NORMALIZATION * rawExpected, within(1e-9));
    }

    @Test
    void the_double_sum_already_counts_each_set_no_separate_set_factor() {
        List<SetEffort> threeSets = List.of(
                new SetEffort(5, 8.0), new SetEffort(5, 8.0), new SetEffort(5, 8.0));
        double rawExpected = 3 * 5 * StimulusCalculator.effortFactor(8.0) * StimulusCalculator.loadFactor(null);

        TrainingStimulus stimulus = calculator.from(threeSets);

        assertThat(stimulus.magnitude()).isCloseTo(StimulusCalculator.NORMALIZATION * rawExpected, within(1e-9));
    }

    @Test
    void an_empty_session_yields_no_stimulus() {
        assertThat(calculator.from(List.of()).magnitude()).isEqualTo(0.0);
    }

    // --- Distribution par muscle (mapping pondéré sourcé, Couche 2) ---

    @Test
    void distribute_spreads_a_compound_across_its_muscles_and_conserves_total_magnitude() {
        // squat 5×1 @8 (sans %1RM ici → load plancher), réparti sur quads/glutes/hams/lombaires/core.
        double rawSquat = 5 * StimulusCalculator.effortFactor(8.0) * StimulusCalculator.loadFactor(null);
        List<ExerciseStimulus> session = List.of(
                ExerciseStimulus.compound(MovementPattern.SQUAT, List.of(new SetEffort(5, 8.0))));

        Map<MuscleGroup, TrainingStimulus> distributed = calculator.distribute(session, mapping);

        assertThat(distributed).containsOnlyKeys(MuscleGroup.QUADS, MuscleGroup.GLUTES,
                MuscleGroup.HAMSTRINGS, MuscleGroup.BACK_LOWER, MuscleGroup.CORE);
        assertThat(distributed).doesNotContainKey(MuscleGroup.BICEPS); // un squat ne fatigue pas les biceps
        assertThat(distributed.get(MuscleGroup.QUADS).magnitude())
                .isCloseTo(StimulusCalculator.NORMALIZATION * rawSquat * 0.42, within(1e-9)); // poids QUADS = 0.42
        // Conservation : la somme des magnitudes par muscle = la magnitude de l'exercice (poids somment à 1).
        double total = distributed.values().stream().mapToDouble(TrainingStimulus::magnitude).sum();
        assertThat(total).isCloseTo(StimulusCalculator.NORMALIZATION * rawSquat, within(1e-9));
    }

    @Test
    void distribute_sends_an_isolation_fully_onto_its_target_and_sums_overlaps() {
        // curl (biceps) 12×1 sans RPE → tout sur BICEPS ; un row ajoute aussi du biceps (0.20).
        double rawCurl = 12 * StimulusCalculator.effortFactor(null) * StimulusCalculator.loadFactor(null);
        double rawRow = 10 * StimulusCalculator.effortFactor(8.0) * StimulusCalculator.loadFactor(null);
        List<ExerciseStimulus> session = List.of(
                ExerciseStimulus.accessory(BodyRegion.BICEPS, List.of(new SetEffort(12, null))),
                ExerciseStimulus.compound(MovementPattern.ROW, List.of(new SetEffort(10, 8.0))));

        Map<MuscleGroup, TrainingStimulus> distributed = calculator.distribute(session, mapping);

        // BICEPS = curl (×1.0) + row (×0.20), magnitudes sommées par muscle.
        assertThat(distributed.get(MuscleGroup.BICEPS).magnitude())
                .isCloseTo(StimulusCalculator.NORMALIZATION * (rawCurl + rawRow * 0.20), within(1e-9));
        assertThat(distributed).containsKey(MuscleGroup.BACK_UPPER); // le dos du row
    }

    @Test
    void distribute_ignores_empty_exercises_and_an_empty_session() {
        assertThat(calculator.distribute(List.of(), mapping)).isEmpty();
        assertThat(calculator.distribute(List.of(
                ExerciseStimulus.compound(MovementPattern.SQUAT, List.of())), mapping)).isEmpty();
    }
}
