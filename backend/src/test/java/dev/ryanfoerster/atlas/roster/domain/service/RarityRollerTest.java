package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class RarityRollerTest {

    private final RarityRoller roller = new RarityRoller();

    @Test
    void maps_boundaries_to_the_right_tiers() {
        assertThat(roller.roll(0.0)).isEqualTo(Rarity.GENERIC);
        assertThat(roller.roll(0.64)).isEqualTo(Rarity.GENERIC);
        assertThat(roller.roll(0.65)).isEqualTo(Rarity.PROMISING);
        assertThat(roller.roll(0.89)).isEqualTo(Rarity.PROMISING);
        assertThat(roller.roll(0.90)).isEqualTo(Rarity.SPECIALIST);
        assertThat(roller.roll(0.97)).isEqualTo(Rarity.SPECIALIST);
        assertThat(roller.roll(0.98)).isEqualTo(Rarity.PRODIGY);
        assertThat(roller.roll(0.999)).isEqualTo(Rarity.PRODIGY);
    }

    @Test
    void rejects_a_roll_out_of_range() {
        assertThatIllegalArgumentException().isThrownBy(() -> roller.roll(-0.1));
        assertThatIllegalArgumentException().isThrownBy(() -> roller.roll(1.0));
    }

    @Test
    void distribution_converges_to_the_target_over_10000_draws() {
        Random rng = new Random(2026); // seed fixe → déterministe, pas flaky
        int n = 10_000;
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        for (Rarity r : Rarity.values()) {
            counts.put(r, 0);
        }
        for (int i = 0; i < n; i++) {
            counts.merge(roller.roll(rng.nextDouble()), 1, Integer::sum);
        }

        System.out.printf("[RARITY DISTRIBUTION over %d] GENERIC=%.2f%% PROMISING=%.2f%% SPECIALIST=%.2f%% PRODIGY=%.2f%%%n",
                n, 100.0 * counts.get(Rarity.GENERIC) / n, 100.0 * counts.get(Rarity.PROMISING) / n,
                100.0 * counts.get(Rarity.SPECIALIST) / n, 100.0 * counts.get(Rarity.PRODIGY) / n);

        assertThat((double) counts.get(Rarity.GENERIC) / n).isCloseTo(0.65, within(0.005));
        assertThat((double) counts.get(Rarity.PROMISING) / n).isCloseTo(0.25, within(0.005));
        assertThat((double) counts.get(Rarity.SPECIALIST) / n).isCloseTo(0.08, within(0.005));
        assertThat((double) counts.get(Rarity.PRODIGY) / n).isCloseTo(0.02, within(0.005));
    }
}
