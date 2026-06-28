package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * État d'adaptation Fitness/Fatigue d'<strong>un</strong> groupe musculaire, dans le modèle de Banister.
 * Value object immutable, sans horloge : le {@code lastUpdated} vit une seule fois au niveau de
 * {@link FitnessFatigueState} (tous les muscles partagent le même instant de référence — sprint 5,
 * ADR-029). C'est la brique élémentaire de la forme par muscle.
 *
 * <ul>
 *   <li>{@code fitness} : adaptation positive, monte modérément, décroît LENTEMENT (τ ≈ 42j) ;</li>
 *   <li>{@code fatigue} : effet négatif, monte fortement, décroît VITE (τ ≈ 7j).</li>
 * </ul>
 *
 * <p>Les deux grandeurs sont adimensionnelles et toujours {@code >= 0} (la décroissance multiplie un
 * non-négatif par {@code exp(...) > 0}, le stimulus est non-négatif). Une valeur négative est une
 * incohérence de bas niveau → {@link IllegalArgumentException}.
 */
public record MuscleCondition(double fitness, double fatigue) {

    /** Muscle qui n'a encore rien encaissé (ou décroissance complète). */
    public static final MuscleCondition ZERO = new MuscleCondition(0.0, 0.0);

    public MuscleCondition {
        if (fitness < 0) {
            throw new IllegalArgumentException("La fitness ne peut pas être négative : " + fitness);
        }
        if (fatigue < 0) {
            throw new IllegalArgumentException("La fatigue ne peut pas être négative : " + fatigue);
        }
    }
}
