package dev.ryanfoerster.atlas.personaltraining.api.events;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.List;

/**
 * Forme aplatie d'un exercice loggé, pour l'event public {@link WorkoutLogged} (ADR-024). Le sealed
 * {@code ExerciseCategory} du domaine est aplati ici en un discriminant + champs nullables :
 * <ul>
 *   <li>{@code categoryType} : {@code "COMPOUND_FORCE"} ou {@code "ACCESSORY"} ;</li>
 *   <li>{@code pattern} : le {@link MovementPattern} (kernel <em>shared</em>, OK dans {@code api/events})
 *       si composé, {@code null} sinon — c'est lui que le consumer compte dans {@code lastPatternsCovered} ;</li>
 *   <li>{@code accessoryRegion} : nom de la {@code BodyRegion} (en {@code String}, pour NE PAS exposer
 *       ce type interne au module) si accessoire, {@code null} sinon.</li>
 * </ul>
 */
public record LoggedExerciseSnapshot(
        String name,
        String categoryType,
        MovementPattern pattern,
        String accessoryRegion,
        List<ExerciseSetSnapshot> sets) {

    public static final String COMPOUND_FORCE = "COMPOUND_FORCE";
    public static final String ACCESSORY = "ACCESSORY";
}
