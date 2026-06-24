package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.Accessory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.CompoundForce;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ExerciseCategoryTest {

    @Test
    void compound_force_exposes_its_movement_pattern() {
        ExerciseCategory category = ExerciseCategory.compound(MovementPattern.SQUAT);

        assertThat(category).isInstanceOf(CompoundForce.class);
        assertThat(category.movementPattern()).contains(MovementPattern.SQUAT);
    }

    @Test
    void accessory_has_no_movement_pattern() {
        ExerciseCategory category = ExerciseCategory.accessory(BodyRegion.BICEPS);

        assertThat(category).isInstanceOf(Accessory.class);
        assertThat(category.movementPattern()).isEmpty();
        assertThat(((Accessory) category).region()).isEqualTo(BodyRegion.BICEPS);
    }

    @Test
    void rejects_null_components() {
        assertThatNullPointerException().isThrownBy(() -> new CompoundForce(null));
        assertThatNullPointerException().isThrownBy(() -> new Accessory(null));
    }

    @Test
    void exhaustive_pattern_matching_over_the_sealed_type() {
        // Prouve l'intérêt du sealed : switch exhaustif sans default (le compilateur garantit la couverture).
        ExerciseCategory compound = ExerciseCategory.compound(MovementPattern.BENCH_PRESS);
        ExerciseCategory accessory = ExerciseCategory.accessory(BodyRegion.TRICEPS);

        assertThat(describe(compound)).isEqualTo("force:BENCH_PRESS");
        assertThat(describe(accessory)).isEqualTo("accessoire:TRICEPS");
    }

    private static String describe(ExerciseCategory category) {
        return switch (category) {
            case CompoundForce cf -> "force:" + cf.pattern();
            case Accessory a -> "accessoire:" + a.region();
        };
    }
}
