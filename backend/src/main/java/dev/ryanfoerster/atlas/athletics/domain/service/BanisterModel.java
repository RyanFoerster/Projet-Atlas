package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;

import java.time.Instant;

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
     * Projette l'état jusqu'à {@code at} par décroissance exponentielle pure (sans nouveau stimulus).
     * C'est le calcul du lazy compute : l'état à l'affichage = décroissance depuis {@code lastUpdated}.
     *
     * @throws IllegalArgumentException si {@code at} est antérieur à {@code state.lastUpdated()} (le temps
     *                                  ne recule pas).
     */
    public FitnessFatigueState decayedTo(FitnessFatigueState state, Instant at) {
        double days = elapsedDays(state.lastUpdated(), at);
        double fitness = state.fitness() * Math.exp(-days / TAU_FITNESS_DAYS);
        double fatigue = state.fatigue() * Math.exp(-days / TAU_FATIGUE_DAYS);
        return new FitnessFatigueState(fitness, fatigue, at);
    }

    /**
     * Applique un stimulus à l'instant {@code at} : on décroît d'abord l'état jusqu'à {@code at}, puis on
     * ajoute la même magnitude à la fitness ET à la fatigue (arbitrage : asymétrie en sortie uniquement).
     */
    public FitnessFatigueState applyStimulus(FitnessFatigueState state, TrainingStimulus stimulus, Instant at) {
        FitnessFatigueState decayed = decayedTo(state, at);
        return new FitnessFatigueState(
                decayed.fitness() + stimulus.magnitude(),
                decayed.fatigue() + stimulus.magnitude(),
                at);
    }

    /** Performance disponible = k1·fitness − k2·fatigue. Peut être négative juste après une grosse séance. */
    public double availablePerformance(FitnessFatigueState state) {
        return K1 * state.fitness() - K2 * state.fatigue();
    }

    private static double elapsedDays(Instant from, Instant to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Le temps ne recule pas : " + to + " < " + from);
        }
        return (to.getEpochSecond() - from.getEpochSecond()) / SECONDS_PER_DAY;
    }
}
