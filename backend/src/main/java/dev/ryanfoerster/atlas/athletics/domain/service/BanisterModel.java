package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.MuscleCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Modèle Fitness-Fatigue de Banister (impulse-response), formulation <strong>récursive discrète</strong>.
 * Domain service pur et stateless (zéro framework). Cœur scientifique d'Atlas (ADR-004, ADR-028).
 *
 * <h2>Formulation</h2>
 * On ne ré-intègre jamais tout l'historique (convolution continue, coûteuse) : on garde l'état courant
 * {@code (fitness, fatigue)} + son {@code lastUpdated}, et on fait décroître exponentiellement depuis ce
 * timestamp à chaque lecture ou application de stimulus — ce qui colle au <em>lazy compute</em> (ADR-006).
 *
 * <ul>
 *   <li>Décroissance : {@code fitness ← fitness · exp(−Δt/τ_fitness)}, {@code fatigue ← fatigue · exp(−Δt/τ_fatigue)} ;</li>
 *   <li>Stimulus : {@code fitness += S}, {@code fatigue += S} (<strong>même</strong> impulsion en entrée — l'asymétrie
 *       du modèle est portée par {@code τ_fatigue ≪ τ_fitness} et par {@code k2 > k1} en sortie) ;</li>
 *   <li>Performance disponible : {@code k1 · fitness − k2 · fatigue}.</li>
 * </ul>
 *
 * <p>Conséquence clé (supercompensation) : juste après une séance, la fatigue domine → performance basse
 * (« cuit ») ; après quelques jours, la fatigue décroît plus vite que la fitness → la performance dépasse
 * le niveau initial. C'est le fondement de la périodisation et du deload.
 *
 * <h2>Calibration des constantes</h2>
 * Les constantes de temps proviennent de la littérature endurance classique
 * (Banister 1975 ; Calvert et al. 1976) : τ_fitness ≈ 42j, τ_fatigue ≈ 7j ; poids de sortie classiques
 * k1 = 1, k2 = 2. <strong>Aucune valeur de littérature n'existe pour transposer ce modèle à une « forme »
 * de musculation</strong> : ces constantes sont donc une calibration <em>par défaut</em>, validée et
 * ajustée par les scénarios de simulation (12 semaines, supercompensation après deload). Voir ADR-028 et
 * {@code docs/domain/sport-science.md}.
 *
 * <h2>Sources</h2>
 * <ul>
 *   <li>Banister, E.W. (1975). <em>A systems model of training for athletic performance</em>. Aust J Sports Med.</li>
 *   <li>Calvert, T.W. et al. (1976). <em>A systems model of the effects of training on physical performance</em>.</li>
 * </ul>
 */
public final class BanisterModel {

    /** Constante de temps de la fitness, en jours (décroissance LENTE). Banister/Calvert : ~42j. */
    public static final double TAU_FITNESS_DAYS = 42.0;

    /** Constante de temps de la fatigue, en jours (décroissance RAPIDE). Banister/Calvert : ~7j. */
    public static final double TAU_FATIGUE_DAYS = 7.0;

    /** Poids de la fitness dans la performance disponible. Classique : 1.0. */
    public static final double K1 = 1.0;

    /** Poids de la fatigue dans la performance disponible. Classique : 2.0 (la fatigue « masque » ~2×). */
    public static final double K2 = 2.0;

    private static final double SECONDS_PER_DAY = 86_400.0;

    /**
     * Projette l'état jusqu'à {@code at} par décroissance exponentielle pure (sans nouveau stimulus),
     * <strong>muscle par muscle</strong> (sprint 5). Les muscles sont indépendants ; ils partagent les
     * mêmes constantes de temps (la modulation génétique de τ est la Couche 3). C'est le calcul du lazy
     * compute : l'état à l'affichage = décroissance depuis {@code lastUpdated}.
     *
     * @throws IllegalArgumentException si {@code at} est antérieur à {@code state.lastUpdated()} (le temps
     *                                  ne recule pas).
     */
    public FitnessFatigueState decayedTo(FitnessFatigueState state, GeneticModifiers modifiers, Instant at) {
        double days = elapsedDays(state.lastUpdated(), at);
        double fitnessFactor = Math.exp(-days / TAU_FITNESS_DAYS);
        // τ_fatigue_eff = τ_fatigue / recoveryRate (récupère vite ⇒ fatigue décroît plus vite).
        double fatigueFactor = Math.exp(-days * modifiers.recoveryRate() / TAU_FATIGUE_DAYS);
        Map<MuscleGroup, MuscleCondition> decayed = new EnumMap<>(MuscleGroup.class);
        state.byMuscle().forEach((muscle, condition) -> decayed.put(muscle,
                new MuscleCondition(condition.fitness() * fitnessFactor, condition.fatigue() * fatigueFactor)));
        return new FitnessFatigueState(decayed, at);
    }

    /**
     * Applique un stimulus <strong>distribué par muscle</strong> à l'instant {@code at} : on décroît d'abord
     * tout l'état jusqu'à {@code at}, puis on ajoute la magnitude de chaque muscle (×{@code stimulusMultiplier}
     * génétique) à sa fitness ET à sa fatigue (arbitrage : asymétrie en sortie uniquement). Un muscle absent
     * de l'état mais présent dans la distribution est créé depuis zéro.
     */
    public FitnessFatigueState applyStimulus(FitnessFatigueState state,
                                             Map<MuscleGroup, TrainingStimulus> distributed,
                                             GeneticModifiers modifiers, Instant at) {
        Map<MuscleGroup, MuscleCondition> result = new EnumMap<>(MuscleGroup.class);
        result.putAll(decayedTo(state, modifiers, at).byMuscle()); // EnumMap(Map) lèverait si la source est vide
        distributed.forEach((muscle, stimulus) -> {
            double impulse = stimulus.magnitude() * modifiers.stimulusMultiplier();
            MuscleCondition base = result.getOrDefault(muscle, MuscleCondition.ZERO);
            result.put(muscle, new MuscleCondition(base.fitness() + impulse, base.fatigue() + impulse));
        });
        return new FitnessFatigueState(result, at);
    }

    /**
     * Performance disponible <strong>agrégée</strong> = k1·Σfitness − k2·Σfatigue (arbitrage ② : somme des
     * muscles). Peut être négative juste après une grosse séance (« cuit »).
     */
    public double availablePerformance(FitnessFatigueState state) {
        return K1 * state.totalFitness() - K2 * state.totalFatigue();
    }

    /** Performance disponible d'<strong>un</strong> muscle = k1·fitness − k2·fatigue (utile à l'analyse par muscle). */
    public double availablePerformance(MuscleCondition condition) {
        return K1 * condition.fitness() - K2 * condition.fatigue();
    }

    private static double elapsedDays(Instant from, Instant to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Le temps ne recule pas : " + to + " < " + from);
        }
        return (to.getEpochSecond() - from.getEpochSecond()) / SECONDS_PER_DAY;
    }
}
