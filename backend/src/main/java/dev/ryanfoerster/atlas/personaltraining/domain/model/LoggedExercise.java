package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Un exercice loggé dans une séance : un nom, une {@link ExerciseCategory} (composé ou accessoire) et
 * au moins une série. Value object riche (mais sans identité propre — il n'a de sens que dans une
 * {@link WorkoutSession}).
 *
 * <p><strong>Immutabilité réelle</strong> : la liste de séries est recopiée défensivement via
 * {@link List#copyOf} dans le constructeur canonique (sinon « record immutable » serait un mensonge).
 */
public record LoggedExercise(ExerciseName name, ExerciseCategory category, List<ExerciseSet> sets) {

    public LoggedExercise {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(sets, "sets");
        if (sets.isEmpty()) {
            throw new InvalidExerciseException("Un exercice doit contenir au moins une série : " + name);
        }
        sets = List.copyOf(sets); // copie défensive → immutabilité réelle
    }

    public int totalSets() {
        return sets.size();
    }

    public int totalReps() {
        return sets.stream().mapToInt(ExerciseSet::reps).sum();
    }

    public BigDecimal volumeKg() {
        return sets.stream().map(ExerciseSet::volumeKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
