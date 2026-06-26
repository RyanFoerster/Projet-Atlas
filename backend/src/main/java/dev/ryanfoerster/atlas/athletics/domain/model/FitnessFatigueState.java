package dev.ryanfoerster.atlas.athletics.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * État d'adaptation court terme d'un athlète, modèle de Banister (impulse-response). Value object
 * immutable. <strong>Sprint 4 : une seule paire globale</strong> (fitness, fatigue) par athlète — le
 * raffinement par {@code MuscleGroup} est le sprint 5 (ADR-004, phasing).
 *
 * <ul>
 *   <li>{@code fitness} : adaptation positive, monte modérément, décroît LENTEMENT (τ ≈ 42j) ;</li>
 *   <li>{@code fatigue} : effet négatif, monte fortement, décroît VITE (τ ≈ 7j) ;</li>
 *   <li>{@code lastUpdated} : instant auquel cet état est exact. La décroissance jusqu'à « maintenant »
 *       est calculée à la volée (lazy compute, ADR-006) par {@code BanisterModel}, jamais par un
 *       scheduler.</li>
 * </ul>
 *
 * <p>Les deux grandeurs sont adimensionnelles et toujours {@code >= 0} (la décroissance multiplie un
 * non-négatif par {@code exp(...) > 0}, le stimulus est non-négatif). Une valeur négative est une
 * incohérence de bas niveau (jamais un input métier) → {@link IllegalArgumentException}, comme {@code Weight}.
 */
public record FitnessFatigueState(double fitness, double fatigue, Instant lastUpdated) {

    public FitnessFatigueState {
        if (fitness < 0) {
            throw new IllegalArgumentException("La fitness ne peut pas être négative : " + fitness);
        }
        if (fatigue < 0) {
            throw new IllegalArgumentException("La fatigue ne peut pas être négative : " + fatigue);
        }
        Objects.requireNonNull(lastUpdated, "lastUpdated");
    }

    /** État initial d'un athlète qui n'a encore rien encaissé : fitness et fatigue à zéro. */
    public static FitnessFatigueState initial(Instant at) {
        return new FitnessFatigueState(0.0, 0.0, at);
    }
}
