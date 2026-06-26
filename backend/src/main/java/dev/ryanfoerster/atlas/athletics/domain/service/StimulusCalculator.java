package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;

import java.util.Collection;

/**
 * Calcule la magnitude du {@link TrainingStimulus} d'une séance. Domain service pur et stateless.
 * <strong>LA décision de modélisation du sprint</strong> (ADR-028, sport-science.md).
 *
 * <h2>Formule (sprint 4, stat globale)</h2>
 * <pre>{@code  S = NORMALIZATION × Σ_séries ( reps × effort(rpe) )}</pre>
 *
 * Deux des trois variables de dose de la musculation :
 * <ul>
 *   <li><strong>volume</strong> = {@code reps} (driver primaire — Schoenfeld &amp; Krieger) ;</li>
 *   <li><strong>intensité d'effort</strong> = {@code effort(rpe)} (proximité de l'échec — Helms RIR /
 *       Nuckols « stimulating reps »).</li>
 * </ul>
 * La 3<sup>e</sup> (intensité de charge / %1RM) est <strong>volontairement hors-scope</strong> : le RPE
 * capture déjà l'intensité <em>relative à la capacité</em> (RPE 8 = 2 RIR quel que soit le poids absolu),
 * sans avoir besoin du 1RM. La charge absolue et la distribution par muscle sont le sprint 5.
 *
 * <h2>{@code effort(rpe)} — linéaire pour démarrer</h2>
 * {@code effort(rpe) = rpe / 10} (RPE 6 → 0.6, RPE 10 → 1.0). Choix le plus défendable (zéro paramètre
 * arbitraire). Candidats de raffinement <em>réservés au GATE 1</em> si la fatigue ne discrimine pas assez
 * les séances dures des faciles : seuil {@code (rpe−4)/6} ou convexe {@code (rpe/10)²}
 * (« stimulating reps »). On décide sur observation de la courbe, pas par anticipation.
 *
 * <p>{@code rpe} absent → effort <strong>neutre</strong> {@link #DEFAULT_EFFORT_RPE} : 0.7 = RPE moyen réel,
 * donc l'omission n'est ni récompensée ni pénalisée (0.6 inciterait à sur-logger des RPE optimistes).
 */
public final class StimulusCalculator {

    /** Effort neutre quand le RPE n'est pas renseigné (ni bonus ni malus à l'omission). */
    public static final double DEFAULT_EFFORT_RPE = 7.0;

    /**
     * Normalisation globale : met la magnitude à une échelle lisible face à la décroissance. La forme de
     * la courbe (supercompensation, yo-yo) est indépendante de cette constante (le modèle est linéaire en
     * l'impulsion) — elle ne fixe que l'échelle verticale. <strong>Valeur provisoire, calibrée au GATE 1</strong>
     * sur la courbe de simulation (ADR-028).
     */
    public static final double NORMALIZATION = 0.01;

    /** Magnitude de la séance = NORM × Σ (reps × effort(rpe)) sur toutes ses séries. */
    public TrainingStimulus from(Collection<SetEffort> sets) {
        double raw = 0.0;
        for (SetEffort set : sets) {
            raw += set.reps() * effortFactor(set.rpe());
        }
        return new TrainingStimulus(NORMALIZATION * raw);
    }

    /** Facteur d'effort dans [0.1, 1.0] : {@code rpe/10}, avec {@code null} → effort neutre (0.7). */
    public static double effortFactor(Double rpe) {
        double value = (rpe == null) ? DEFAULT_EFFORT_RPE : rpe;
        return value / 10.0;
    }
}
