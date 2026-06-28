package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Mapping stimulus → muscle, <strong>Couche 2 (pondéré sourcé, ADR-030)</strong> : invariant somme = 1,
 * distribution défendable des composés (squat → jambes/dos, pas biceps) et des accessoires, et résolution
 * des deux frictions (BACK, FOREARMS).
 */
class MuscleStimulusMappingTest {

    private final MuscleStimulusMapping mapping = new MuscleStimulusMapping();
    private static final List<SetEffort> SETS = List.of(new SetEffort(5, 8.0));

    @ParameterizedTest
    @EnumSource(MovementPattern.class)
    void every_compound_pattern_has_weights_summing_to_one(MovementPattern pattern) {
        assertThat(weightSum(mapping.weightsFor(ExerciseStimulus.compound(pattern, SETS)))).isCloseTo(1.0, within(1e-9));
    }

    @ParameterizedTest
    @EnumSource(BodyRegion.class)
    void every_accessory_region_has_weights_summing_to_one_and_is_mapped(BodyRegion region) {
        Map<MuscleGroup, Double> weights = mapping.weightsFor(ExerciseStimulus.accessory(region, SETS));
        assertThat(weights).isNotNull().isNotEmpty();
        assertThat(weightSum(weights)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void a_squat_loads_the_legs_and_lower_back_not_the_arms() {
        Map<MuscleGroup, Double> squat = mapping.weightsFor(ExerciseStimulus.compound(MovementPattern.SQUAT, SETS));

        assertThat(squat).containsOnlyKeys(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS,
                MuscleGroup.BACK_LOWER, MuscleGroup.CORE);
        assertThat(squat.get(MuscleGroup.QUADS)).isEqualTo(0.42); // quads dominants
        assertThat(squat).doesNotContainKey(MuscleGroup.BICEPS).doesNotContainKey(MuscleGroup.CHEST);
    }

    @Test
    void a_bench_press_loads_chest_then_triceps_and_shoulders() {
        Map<MuscleGroup, Double> bench = mapping.weightsFor(ExerciseStimulus.compound(MovementPattern.BENCH_PRESS, SETS));

        assertThat(bench.get(MuscleGroup.CHEST)).isEqualTo(0.50);
        assertThat(bench).containsOnlyKeys(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS);
        assertThat(bench).doesNotContainKey(MuscleGroup.BACK_UPPER);
    }

    @Test
    void a_chin_up_loads_the_back_more_than_the_biceps() {
        Map<MuscleGroup, Double> chin = mapping.weightsFor(ExerciseStimulus.compound(MovementPattern.CHIN_UP, SETS));

        assertThat(chin.get(MuscleGroup.BACK_UPPER)).isGreaterThan(chin.get(MuscleGroup.BICEPS));
        assertThat(chin.get(MuscleGroup.BICEPS)).isEqualTo(0.30); // supination → biceps marqués
    }

    @Test
    void a_biceps_isolation_maps_fully_to_the_biceps() {
        assertThat(mapping.weightsFor(ExerciseStimulus.accessory(BodyRegion.BICEPS, SETS)))
                .containsExactly(Map.entry(MuscleGroup.BICEPS, 1.0));
    }

    @Test
    void the_two_known_frictions_are_resolved() {
        // BACK (plus grossier) → 80% BACK_UPPER / 20% BACK_LOWER ; FOREARMS (sans équivalent) → BICEPS.
        Map<MuscleGroup, Double> back = mapping.weightsFor(ExerciseStimulus.accessory(BodyRegion.BACK, SETS));
        assertThat(back).containsOnlyKeys(MuscleGroup.BACK_UPPER, MuscleGroup.BACK_LOWER);
        assertThat(back.get(MuscleGroup.BACK_UPPER)).isEqualTo(0.80);

        assertThat(mapping.weightsFor(ExerciseStimulus.accessory(BodyRegion.FOREARMS, SETS)))
                .containsExactly(Map.entry(MuscleGroup.BICEPS, 1.0));
    }

    private static double weightSum(Map<MuscleGroup, Double> weights) {
        return weights.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
