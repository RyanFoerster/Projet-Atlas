package dev.ryanfoerster.atlas.personaltraining.api.events;

/**
 * Forme aplatie d'une série, pour l'event public {@link WorkoutLogged}. Types primitifs/wrapper
 * uniquement (aucun VO du domaine) — un event public ne doit pas exposer le domaine interne (ADR-024).
 *
 * <p>Le sealed {@code Load} du domaine est aplati en un discriminant {@code loadType} + une valeur
 * {@code weightKg} (sprint 6, ADR-035) :
 * <ul>
 *   <li>{@code BODYWEIGHT} : poids de corps pur, {@code weightKg} {@code null} ;</li>
 *   <li>{@code WEIGHTED} : lesté, {@code weightKg} = charge <em>ajoutée</em> (kg) ;</li>
 *   <li>{@code EXTERNAL} : charge externe, {@code weightKg} = charge externe (kg).</li>
 * </ul>
 * La charge <em>totale déplacée</em> (avec poids de corps) et le %1RM ne sont PAS calculés ici : Athletics
 * les résout (il lit le bodyweight et le 1RM dans Roster). {@code rpe} {@code null} = non renseigné.
 */
public record ExerciseSetSnapshot(int reps, String loadType, Double weightKg, Double rpe) {

    public static final String BODYWEIGHT = "BODYWEIGHT";
    public static final String WEIGHTED = "WEIGHTED";
    public static final String EXTERNAL = "EXTERNAL";
}
