package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.List;
import java.util.Objects;

/**
 * Entrée du calcul de stimulus, côté domaine Athletics : <strong>un</strong> exercice loggé, réduit à ce
 * qui détermine la distribution musculaire (sa cible) et sa magnitude (ses séries). C'est la forme
 * domaine de {@code LoggedExerciseSnapshot} de l'event {@code WorkoutLogged}, traduite par le handler
 * (qui résout le nom de région porté par l'event en {@link BodyRegion} à sa frontière).
 *
 * <p>Un exercice est soit <strong>composé</strong> (rattaché à un {@link MovementPattern} de force), soit
 * <strong>accessoire</strong> (rattaché à une {@link BodyRegion}). Exactement un des deux est renseigné
 * (invariant). La cible sert au {@code MuscleStimulusMapping} à répartir le stimulus sur les bons muscles
 * via les tables pondérées sourcées (ADR-030).
 */
public record ExerciseStimulus(MovementPattern pattern, BodyRegion accessoryRegion, List<SetEffort> sets) {

    public ExerciseStimulus {
        Objects.requireNonNull(sets, "sets");
        boolean hasPattern = pattern != null;
        boolean hasRegion = accessoryRegion != null;
        if (hasPattern == hasRegion) {
            throw new IllegalArgumentException(
                    "Un exercice est soit composé (pattern) soit accessoire (région), pas les deux ni aucun");
        }
        sets = List.copyOf(sets);
    }

    /** Exercice composé rattaché à un pattern de force. */
    public static ExerciseStimulus compound(MovementPattern pattern, List<SetEffort> sets) {
        return new ExerciseStimulus(pattern, null, sets);
    }

    /** Exercice accessoire rattaché à une région musculaire. */
    public static ExerciseStimulus accessory(BodyRegion accessoryRegion, List<SetEffort> sets) {
        return new ExerciseStimulus(null, accessoryRegion, sets);
    }
}
