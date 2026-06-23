package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProceduralAthleteGeneratorTest {

    private final ProceduralAthleteGenerator generator = new ProceduralAthleteGenerator();

    @Test
    void same_seed_and_rarity_produce_the_same_candidate() {
        AthleteCandidate a = generator.generateCandidate(123L, Rarity.SPECIALIST);
        AthleteCandidate b = generator.generateCandidate(123L, Rarity.SPECIALIST);

        assertThat(a).isEqualTo(b); // reproductibilité = testabilité
    }

    @Test
    void different_seeds_produce_different_candidates() {
        assertThat(generator.generateCandidate(1L, Rarity.GENERIC))
                .isNotEqualTo(generator.generateCandidate(2L, Rarity.GENERIC));
    }

    @Test
    void rarity_tiers_are_distinct_by_number_of_specialized_axes_and_magnitude() {
        Genetics generic = generator.generateCandidate(7L, Rarity.GENERIC).genetics();
        Genetics promising = generator.generateCandidate(7L, Rarity.PROMISING).genetics();
        Genetics specialist = generator.generateCandidate(7L, Rarity.SPECIALIST).genetics();
        Genetics prodigy = generator.generateCandidate(7L, Rarity.PRODIGY).genetics();

        for (var e : Map.of("GENERIC", generic, "PROMISING", promising, "SPECIALIST", specialist, "PRODIGY", prodigy).entrySet()) {
            System.out.printf("[TIER %-10s] peak=%.3f, axes>=1.10=%d, axes>=1.20=%d%n",
                    e.getKey(), peakAffinity(e.getValue()),
                    countAxesAtLeast(e.getValue(), 1.10), countAxesAtLeast(e.getValue(), 1.20));
        }

        assertThat(peakAffinity(generic)).isLessThanOrEqualTo(1.06);        // 0 axe spécialisé
        assertThat(countAxesAtLeast(promising, 1.08)).isGreaterThanOrEqualTo(1); // 1 axe modeste
        assertThat(countAxesAtLeast(specialist, 1.12)).isGreaterThanOrEqualTo(2); // 2 axes francs
        assertThat(peakAffinity(prodigy)).isGreaterThanOrEqualTo(1.20);     // 1 axe exceptionnel
    }

    @Test
    void mirror_genetics_boosts_strength_affinity_in_line_with_strength_ratios() {
        long seed = 42L;
        Weight bw = Weight.ofKilograms(80);

        Genetics ryan = generator.generateGeneticsForMirror(
                Map.of(MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100))), bw, Gender.MALE, seed);
        Genetics elite = generator.generateGeneticsForMirror(
                Map.of(MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(150))), bw, Gender.MALE, seed);

        double ryanBench = ryan.strengthAffinity(MovementPattern.BENCH_PRESS);
        double eliteBench = elite.strengthAffinity(MovementPattern.BENCH_PRESS);
        System.out.printf("[MIRROR] bench100/BW80 (ratio 1.25) -> strengthAffinity[BENCH]=%.3f | "
                + "bench150/BW80 (ratio 1.875) -> %.3f (plafonné)%n", ryanBench, eliteBench);

        assertThat(ryanBench).isGreaterThanOrEqualTo(1.076).isLessThanOrEqualTo(1.25);
        assertThat(eliteBench).isGreaterThanOrEqualTo(1.20).isLessThanOrEqualTo(1.25);
        assertThat(eliteBench).isGreaterThanOrEqualTo(ryanBench); // plus de force → plus d'affinité
    }

    @Test
    void mirror_leaves_untouched_lifts_to_chance() {
        // Aucun 1RM saisi → aucun boost, la génétique reste dans ses plages normales.
        Genetics g = generator.generateGeneticsForMirror(Map.of(), Weight.ofKilograms(80), Gender.MALE, 1L);

        assertThat(g.strengthAffinity(MovementPattern.ROW))
                .isBetween(Genetics.STRENGTH_MIN, Genetics.STRENGTH_MAX);
    }

    private static int countAxesAtLeast(Genetics g, double threshold) {
        int count = 0;
        for (MovementPattern p : MovementPattern.values()) {
            if (g.strengthAffinity(p) >= threshold) {
                count++;
            }
        }
        for (MuscleGroup m : MuscleGroup.values()) {
            if (g.hypertrophyPotential(m) >= threshold) {
                count++;
            }
        }
        return count;
    }

    private static double peakAffinity(Genetics g) {
        double peak = 0.0;
        for (MovementPattern p : MovementPattern.values()) {
            peak = Math.max(peak, g.strengthAffinity(p));
        }
        for (MuscleGroup m : MuscleGroup.values()) {
            peak = Math.max(peak, g.hypertrophyPotential(m));
        }
        return peak;
    }
}
