package dev.ryanfoerster.atlas.roster.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RarityTest {

    @Test
    void probabilities_sum_to_one() {
        double total = 0.0;
        for (Rarity r : Rarity.values()) {
            total += r.probability();
        }
        assertThat(total).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void tiers_are_ordered_from_common_to_rare() {
        assertThat(Rarity.values())
                .containsExactly(Rarity.GENERIC, Rarity.PROMISING, Rarity.SPECIALIST, Rarity.PRODIGY);
        assertThat(Rarity.GENERIC.probability()).isGreaterThan(Rarity.PRODIGY.probability());
    }
}
