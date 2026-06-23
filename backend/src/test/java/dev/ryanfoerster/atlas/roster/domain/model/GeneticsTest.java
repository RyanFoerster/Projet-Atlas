package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidGeneticsException;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GeneticsTest {

    static Map<MuscleGroup, Double> fullHypertrophy(double v) {
        Map<MuscleGroup, Double> m = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup g : MuscleGroup.values()) {
            m.put(g, v);
        }
        return m;
    }

    static Map<MovementPattern, Double> fullStrength(double v) {
        Map<MovementPattern, Double> m = new EnumMap<>(MovementPattern.class);
        for (MovementPattern p : MovementPattern.values()) {
            m.put(p, v);
        }
        return m;
    }

    static Genetics valid() {
        return new Genetics(fullHypertrophy(1.0), fullStrength(1.0), 1.0, 0.5, 1.0);
    }

    @Test
    void builds_a_valid_genetics_and_exposes_axes() {
        Genetics g = valid();
        assertThat(g.hypertrophyPotential(MuscleGroup.CHEST)).isEqualTo(1.0);
        assertThat(g.strengthAffinity(MovementPattern.SQUAT)).isEqualTo(1.0);
    }

    @Test
    void rejects_a_missing_axis() {
        Map<MuscleGroup, Double> incomplete = fullHypertrophy(1.0);
        incomplete.remove(MuscleGroup.CORE);
        assertThatExceptionOfType(InvalidGeneticsException.class)
                .isThrownBy(() -> new Genetics(incomplete, fullStrength(1.0), 1.0, 0.5, 1.0));
    }

    @Test
    void rejects_a_value_out_of_range() {
        Map<MovementPattern, Double> tooHigh = fullStrength(1.30); // > STRENGTH_MAX 1.25
        assertThatExceptionOfType(InvalidGeneticsException.class)
                .isThrownBy(() -> new Genetics(fullHypertrophy(1.0), tooHigh, 1.0, 0.5, 1.0));
    }

    @Test
    void rejects_fiber_type_outside_zero_one() {
        assertThatExceptionOfType(InvalidGeneticsException.class)
                .isThrownBy(() -> new Genetics(fullHypertrophy(1.0), fullStrength(1.0), 1.0, 1.5, 1.0));
    }

    @Test
    void is_immutable_defensive_copy_protects_from_input_mutation() {
        Map<MuscleGroup, Double> mutable = new HashMap<>(fullHypertrophy(1.0));
        Genetics g = new Genetics(mutable, fullStrength(1.0), 1.0, 0.5, 1.0);

        mutable.put(MuscleGroup.CHEST, 99.0); // mutation APRÈS construction

        assertThat(g.hypertrophyPotential(MuscleGroup.CHEST)).isEqualTo(1.0); // inchangé
    }

    @Test
    void equality_is_by_value() {
        assertThat(valid()).isEqualTo(valid());
    }
}
