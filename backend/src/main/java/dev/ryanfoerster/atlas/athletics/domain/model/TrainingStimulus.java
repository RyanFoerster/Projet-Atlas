package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * Impulsion d'entraînement qu'une séance inflige à un athlète, dans le modèle de Banister. Value object
 * immutable. <strong>Sprint 4 : magnitude scalaire globale</strong> (pas de distribution par
 * {@code MuscleGroup} — sprint 5, ADR-004).
 *
 * <p>La magnitude est calculée par {@code StimulusCalculator} depuis le volume × effort de la séance
 * (cf. ADR-028 / sport-science.md). Elle est ajoutée <em>identiquement</em> à la fitness ET à la fatigue
 * (l'asymétrie du modèle vit dans les constantes de temps τ et les poids de sortie k1/k2, pas dans
 * l'entrée). Toujours {@code >= 0} : une magnitude négative est une incohérence de bas niveau →
 * {@link IllegalArgumentException}.
 */
public record TrainingStimulus(double magnitude) {

    /** Absence de stimulus (jour de repos, séance vide). */
    public static final TrainingStimulus NONE = new TrainingStimulus(0.0);

    public TrainingStimulus {
        if (magnitude < 0) {
            throw new IllegalArgumentException("La magnitude d'un stimulus ne peut pas être négative : " + magnitude);
        }
    }
}
