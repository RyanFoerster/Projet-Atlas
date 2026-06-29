package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseSetException;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Une série (set) d'un exercice : un nombre de répétitions, un {@link Load} (type de charge — poids de
 * corps / lesté / externe, sprint 6 ADR-035) et un RPE optionnel. Value object auto-validant.
 *
 * <ul>
 *   <li>{@code reps} : 1–100 (au-delà, c'est une saisie aberrante, pas un input métier valide) ;</li>
 *   <li>{@code load} : {@link Load} non-null (le poids de corps est explicite, {@link Load#bodyweight()},
 *       pas un {@code null}) ;</li>
 *   <li>{@code rpe} : {@link RPE} ou {@code null} (non renseigné).</li>
 * </ul>
 *
 * <p>Les factories {@link #bodyweight}, {@link #weighted}, {@link #external} rendent la saisie
 * intention-révélante côté appelant.
 */
public record ExerciseSet(int reps, Load load, RPE rpe) {

    public static final int MIN_REPS = 1;
    public static final int MAX_REPS = 100;

    public ExerciseSet {
        if (reps < MIN_REPS || reps > MAX_REPS) {
            throw new InvalidExerciseSetException(
                    "Le nombre de répétitions doit être dans [" + MIN_REPS + ", " + MAX_REPS + "] : " + reps);
        }
        Objects.requireNonNull(load, "load");
        // rpe nullable par design (non renseigné).
    }

    /** Série au poids de corps pur. */
    public static ExerciseSet bodyweight(int reps, RPE rpe) {
        return new ExerciseSet(reps, Load.bodyweight(), rpe);
    }

    /** Série lestée : poids de corps + {@code added}. */
    public static ExerciseSet weighted(int reps, Weight added, RPE rpe) {
        return new ExerciseSet(reps, Load.weighted(added), rpe);
    }

    /** Série en charge externe. */
    public static ExerciseSet external(int reps, Weight weight, RPE rpe) {
        return new ExerciseSet(reps, Load.external(weight), rpe);
    }

    public Optional<RPE> rpeOptional() {
        return Optional.ofNullable(rpe);
    }

    /**
     * Volume <em>externe</em> de la série en kg : {@code reps × charge externe portée} (lestée/externe).
     * Le poids de corps n'est <strong>pas</strong> compté ici — PersonalTraining ne connaît pas le
     * bodyweight de l'athlète (il vit dans Roster). La charge totale déplacée est résolue côté Athletics
     * (sprint 6, couche 2). Une série au poids de corps pur contribue donc {@code 0} à ce volume externe.
     */
    public BigDecimal volumeKg() {
        return load.externalWeight()
                .map(weight -> weight.toKilograms().multiply(BigDecimal.valueOf(reps)))
                .orElse(BigDecimal.ZERO);
    }
}
