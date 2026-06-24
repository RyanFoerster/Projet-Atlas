package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import java.util.List;

/**
 * Forme sérialisable (JSONB) d'un exercice loggé. Le sealed {@code ExerciseCategory} du domaine est
 * <strong>aplati</strong> ici en un discriminant + champs nullables (le même esprit que le snapshot
 * d'event, ADR-024) :
 * <ul>
 *   <li>{@code categoryType} : {@code "COMPOUND_FORCE"} ou {@code "ACCESSORY"} ;</li>
 *   <li>{@code pattern} : nom du {@code MovementPattern} si composé, {@code null} sinon ;</li>
 *   <li>{@code region} : nom de la {@code BodyRegion} si accessoire, {@code null} sinon.</li>
 * </ul>
 * La reconstruction du bon sous-type se fait dans {@link WorkoutSessionJsonConverter}.
 */
public record ExerciseJson(
        String name,
        String categoryType,
        String pattern,
        String region,
        List<ExerciseSetJson> sets) {
}
