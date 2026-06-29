package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lecteur tolérant (expand/contract, ADR-035). Les lignes antérieures au sprint 6 n'ont PAS de {@code
 * loadType} dans le JSONB ; le convertisseur doit les lire sans planter, en réappliquant la règle
 * d'inférence legacy ({@code weightKg} null → poids de corps, sinon externe). Régression du bug
 * « V013 inopérante » révélé par l'historique réel.
 */
class WorkoutSessionJsonConverterTest {

    @Test
    void reads_legacy_sets_without_load_type_inferring_from_weight() {
        // Forme legacy : loadType absent (null). Une série chargée (avec RPE) + une au poids de corps (RPE null).
        ExercisesJson legacy = new ExercisesJson(List.of(new ExerciseJson(
                "Back Squat", "COMPOUND_FORCE", "SQUAT", null,
                List.of(
                        new ExerciseSetJson(5, null, new BigDecimal("140"), 7.5),
                        new ExerciseSetJson(12, null, null, null)))));

        List<LoggedExercise> exercises = WorkoutSessionJsonConverter.fromJson(legacy);

        List<ExerciseSet> sets = exercises.getFirst().sets();
        assertThat(sets.get(0).load()).isInstanceOfSatisfying(Load.External.class,
                e -> assertThat(e.weight().toKilograms()).isEqualByComparingTo("140")); // weightKg → EXTERNAL
        assertThat(sets.get(1).load()).isEqualTo(Load.bodyweight());                    // weightKg null → BODYWEIGHT
        assertThat(sets.get(1).rpe()).isNull();                                         // le RPE null legacy survit
    }

    @Test
    void still_rejects_a_genuinely_unknown_load_type() {
        ExercisesJson corrupt = new ExercisesJson(List.of(new ExerciseJson(
                "Back Squat", "COMPOUND_FORCE", "SQUAT", null,
                List.of(new ExerciseSetJson(5, "PLASMA", new BigDecimal("140"), 7.5)))));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> WorkoutSessionJsonConverter.fromJson(corrupt))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("loadType");
    }
}
