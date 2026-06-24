package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseSetException;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Une série (set) d'un exercice : un nombre de répétitions, une charge optionnelle (null si poids de
 * corps) et un RPE optionnel. Value object auto-validant.
 *
 * <ul>
 *   <li>{@code reps} : 1–100 (au-delà, c'est une saisie aberrante, pas un input métier valide) ;</li>
 *   <li>{@code weight} : {@link Weight} ou {@code null} (exercice au poids de corps) ;</li>
 *   <li>{@code rpe} : {@link RPE} ou {@code null} (non renseigné).</li>
 * </ul>
 */
public record ExerciseSet(int reps, Weight weight, RPE rpe) {

    public static final int MIN_REPS = 1;
    public static final int MAX_REPS = 100;

    public ExerciseSet {
        if (reps < MIN_REPS || reps > MAX_REPS) {
            throw new InvalidExerciseSetException(
                    "Le nombre de répétitions doit être dans [" + MIN_REPS + ", " + MAX_REPS + "] : " + reps);
        }
        // weight et rpe nullable par design (poids de corps / RPE non renseigné).
    }

    public Optional<Weight> weightOptional() {
        return Optional.ofNullable(weight);
    }

    public Optional<RPE> rpeOptional() {
        return Optional.ofNullable(rpe);
    }

    /**
     * Volume de la série en kg : {@code reps × charge}. Une série au poids de corps (charge nulle)
     * contribue {@code 0} au volume estimé (on ne modélise pas le poids de corps comme charge au sprint 3).
     */
    public BigDecimal volumeKg() {
        if (weight == null) {
            return BigDecimal.ZERO;
        }
        return weight.toKilograms().multiply(BigDecimal.valueOf(reps));
    }
}
