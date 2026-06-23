package dev.ryanfoerster.atlas.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou sur les énumérations du kernel : fige les valeurs attendues (cf. glossaire), pour
 * qu'un ajout/retrait accidentel d'un pattern ou groupe musculaire casse un test.
 */
class MovementEnumsTest {

    @Test
    void movement_patterns_match_the_glossary() {
        assertThat(MovementPattern.values()).containsExactly(
                MovementPattern.SQUAT, MovementPattern.BENCH_PRESS, MovementPattern.DEADLIFT,
                MovementPattern.OVERHEAD_PRESS, MovementPattern.ROW, MovementPattern.CHIN_UP);
    }

    @Test
    void muscle_groups_match_the_glossary() {
        assertThat(MuscleGroup.values()).hasSize(11)
                .contains(MuscleGroup.CHEST, MuscleGroup.QUADS, MuscleGroup.CORE);
    }
}
