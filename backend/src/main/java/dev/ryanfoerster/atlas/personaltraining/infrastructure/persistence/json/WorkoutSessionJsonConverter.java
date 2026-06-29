package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.Accessory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.CompoundForce;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.math.BigDecimal;
import java.util.List;

/**
 * Conversions domaine ↔ DTO JSON pour les exercices d'une séance (volet « sérialisation » du mapping
 * manuel, ADR-015). Le domaine reste pur : c'est ici qu'on aplatit le sealed {@link ExerciseCategory}
 * en discriminant et qu'on reconstruit le bon sous-type. Poids normalisés en kg.
 */
public final class WorkoutSessionJsonConverter {

    static final String COMPOUND_FORCE = "COMPOUND_FORCE";
    static final String ACCESSORY = "ACCESSORY";

    static final String BODYWEIGHT = "BODYWEIGHT";
    static final String WEIGHTED = "WEIGHTED";
    static final String EXTERNAL = "EXTERNAL";

    private WorkoutSessionJsonConverter() {
    }

    public static ExercisesJson toJson(List<LoggedExercise> exercises) {
        return new ExercisesJson(exercises.stream().map(WorkoutSessionJsonConverter::toJson).toList());
    }

    public static List<LoggedExercise> fromJson(ExercisesJson json) {
        return json.exercises().stream().map(WorkoutSessionJsonConverter::fromJson).toList();
    }

    private static ExerciseJson toJson(LoggedExercise exercise) {
        // Aplatissement du sealed via pattern matching exhaustif (pas de default — le compilateur veille).
        String categoryType;
        String pattern = null;
        String region = null;
        switch (exercise.category()) {
            case CompoundForce cf -> {
                categoryType = COMPOUND_FORCE;
                pattern = cf.pattern().name();
            }
            case Accessory a -> {
                categoryType = ACCESSORY;
                region = a.region().name();
            }
        }
        List<ExerciseSetJson> sets = exercise.sets().stream().map(WorkoutSessionJsonConverter::toJson).toList();
        return new ExerciseJson(exercise.name().value(), categoryType, pattern, region, sets);
    }

    private static LoggedExercise fromJson(ExerciseJson json) {
        ExerciseCategory category = switch (json.categoryType()) {
            case COMPOUND_FORCE -> ExerciseCategory.compound(MovementPattern.valueOf(json.pattern()));
            case ACCESSORY -> ExerciseCategory.accessory(BodyRegion.valueOf(json.region()));
            default -> throw new IllegalStateException(
                    "categoryType inconnu en base : " + json.categoryType()); // corruption de données → 500
        };
        List<ExerciseSet> sets = json.sets().stream().map(WorkoutSessionJsonConverter::fromJson).toList();
        return new LoggedExercise(ExerciseName.of(json.name()), category, sets);
    }

    private static ExerciseSetJson toJson(ExerciseSet set) {
        String loadType;
        BigDecimal weightKg = null;
        switch (set.load()) {
            case Load.Bodyweight ignored -> loadType = BODYWEIGHT;
            case Load.Weighted w -> {
                loadType = WEIGHTED;
                weightKg = w.added().toKilograms();
            }
            case Load.External e -> {
                loadType = EXTERNAL;
                weightKg = e.weight().toKilograms();
            }
        }
        return new ExerciseSetJson(set.reps(), loadType, weightKg, set.rpe() == null ? null : set.rpe().value());
    }

    private static ExerciseSet fromJson(ExerciseSetJson json) {
        RPE rpe = json.rpe() == null ? null : RPE.of(json.rpe());
        return new ExerciseSet(json.reps(), toLoad(json), rpe);
    }

    /**
     * <strong>Lecteur tolérant</strong> (expand/contract, ADR-035). Une série antérieure au sprint 6 n'a
     * pas de {@code loadType} en base : on l'infère avec la règle legacy (la même que la migration V014 et
     * le mapper entrant — {@code weightKg} null → poids de corps, sinon externe) plutôt que de planter tout
     * l'historique. Le {@code default -> throw} reste réservé à une vraie valeur inconnue (corruption).
     */
    private static Load toLoad(ExerciseSetJson json) {
        if (json.loadType() == null) {
            return json.weightKg() == null ? Load.bodyweight() : Load.external(Weight.ofKilograms(json.weightKg()));
        }
        return switch (json.loadType()) {
            case BODYWEIGHT -> Load.bodyweight();
            case WEIGHTED -> Load.weighted(Weight.ofKilograms(json.weightKg()));
            case EXTERNAL -> Load.external(Weight.ofKilograms(json.weightKg()));
            default -> throw new IllegalStateException(
                    "loadType inconnu en base : " + json.loadType()); // corruption de données → 500
        };
    }
}
