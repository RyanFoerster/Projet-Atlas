package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * État d'adaptation court terme d'un athlète, modèle de Banister (impulse-response). Value object
 * immutable. <strong>Sprint 5 : une forme par {@link MuscleGroup}</strong> — la paire globale du sprint 4
 * (ADR-004) devient une {@code Map<MuscleGroup, MuscleCondition>} (ADR-029). On peut donc avoir les jambes
 * cuites et le haut du corps frais.
 *
 * <ul>
 *   <li>{@code byMuscle} : la condition par groupe musculaire. <strong>Sparse</strong> : un muscle jamais
 *       travaillé est absent (pas une entrée à zéro). Map vide = athlète qui n'a rien encaissé.</li>
 *   <li>{@code lastUpdated} : <strong>un seul</strong> instant de référence pour tous les muscles
 *       (tension #1 du plan sprint 5). La décroissance jusqu'à « maintenant » est calculée à la volée
 *       (lazy compute, ADR-006) par {@code BanisterModel}, jamais par un scheduler.</li>
 * </ul>
 *
 * <p><strong>Immutabilité réelle</strong> : la {@code Map} est recopiée défensivement via
 * {@link Map#copyOf} dans le constructeur canonique (comme {@code Genetics}) — sinon une référence mutable
 * fuirait.
 *
 * <p>L'<strong>indice de Forme global</strong> est agrégé par <em>somme</em> des muscles (arbitrage ②,
 * ADR-029) : {@link #totalFitness()} / {@link #totalFatigue()} portent la même échelle interne, ce qui rend
 * le ratio performance/fitness indépendant de {@code NORMALIZATION}.
 */
public record FitnessFatigueState(Map<MuscleGroup, MuscleCondition> byMuscle, Instant lastUpdated) {

    public FitnessFatigueState {
        Objects.requireNonNull(byMuscle, "byMuscle");
        Objects.requireNonNull(lastUpdated, "lastUpdated");
        byMuscle = Map.copyOf(byMuscle); // copie défensive → immutabilité réelle (rejette clés/valeurs null)
    }

    /** État initial d'un athlète qui n'a encore rien encaissé : aucun muscle (Map vide). */
    public static FitnessFatigueState initial(Instant at) {
        return new FitnessFatigueState(Map.of(), at);
    }

    /** Condition d'un muscle, ou {@link MuscleCondition#ZERO} si ce muscle n'a jamais été travaillé. */
    public MuscleCondition condition(MuscleGroup muscle) {
        return byMuscle.getOrDefault(muscle, MuscleCondition.ZERO);
    }

    /** Fitness agrégée = somme sur les muscles présents (arbitrage ② : agrégation par somme). */
    public double totalFitness() {
        return byMuscle.values().stream().mapToDouble(MuscleCondition::fitness).sum();
    }

    /** Fatigue agrégée = somme sur les muscles présents. */
    public double totalFatigue() {
        return byMuscle.values().stream().mapToDouble(MuscleCondition::fatigue).sum();
    }
}
