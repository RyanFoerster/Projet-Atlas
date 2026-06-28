package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
 * <h2>{@code effort(rpe)} — seuil convexe doux {@code (rpe−4)/6} (sprint 5, GATE 2)</h2>
 * {@code effort(rpe) = clamp((rpe − 4) / 6, 0, 1)} : RPE ≤ 4 → 0, RPE 7 → 0.5, RPE 8 → 0.67, RPE 10 → 1.0.
 * Adopté au GATE 2 (ADR-031) après réévaluation sur observation. Deux propriétés voulues : <strong>les
 * warmups (RPE ≤ 4, ~6+ reps en réserve) ne stimulent quasi rien</strong> (le linéaire {@code rpe/10} leur
 * donnait 0.40, faux), et une <strong>convexité douce</strong> qui reflète les <em>stimulating reps</em>
 * (Nuckols/SBS — les dernières reps avant l'échec recrutent les fibres rapides, disproportionnellement
 * stimulantes) sans écraser les efforts modérés comme le ferait {@code (rpe/10)²}. Le seuil 4 est défendable
 * physiologiquement (sous RPE 4, stimulus négligeable pour l'adaptation), pas un magic number.
 *
 * <p>{@code rpe} absent → effort <strong>neutre</strong> {@link #DEFAULT_EFFORT_RPE} = RPE 7 → 0.5, donc
 * l'omission n'est ni récompensée ni pénalisée.
 */
public final class StimulusCalculator {

    /** RPE neutre supposé quand le RPE n'est pas renseigné (ni bonus ni malus à l'omission). */
    public static final double DEFAULT_EFFORT_RPE = 7.0;

    /** Seuil sous lequel un effort ne stimule plus (warmups) : {@code (rpe − THRESHOLD) / (10 − THRESHOLD)}. */
    public static final double EFFORT_THRESHOLD_RPE = 4.0;

    /**
     * Normalisation globale : met la magnitude à une échelle lisible face à la décroissance. La forme de
     * la courbe (supercompensation, yo-yo) est indépendante de cette constante (le modèle est linéaire en
     * l'impulsion) — elle ne fixe que l'échelle verticale. <strong>Recalibrée au GATE 2</strong> de 0.01 à
     * 0.013 : le passage de {@code rpe/10} à {@code (rpe−4)/6} réduit les magnitudes (~×0.78), NORM compense
     * pour garder l'échelle des courbes (ADR-028, ADR-031). Recalibrage légitime, pas un fudge.
     */
    public static final double NORMALIZATION = 0.013;

    /** Magnitude d'un bloc de séries = NORM × Σ (reps × effort(rpe)). Brique du calcul par exercice. */
    public TrainingStimulus from(Collection<SetEffort> sets) {
        double raw = 0.0;
        for (SetEffort set : sets) {
            raw += set.reps() * effortFactor(set.rpe());
        }
        return new TrainingStimulus(NORMALIZATION * raw);
    }

    /**
     * <strong>Distribue</strong> le stimulus d'une séance sur les {@link MuscleGroup} (sprint 5). Pour
     * chaque exercice : magnitude {@code = NORM × Σ(reps × effort)}, répartie selon les poids du
     * {@link MuscleStimulusMapping} ({@code magnitude × poids}), puis sommée par muscle sur toute la séance.
     * Un exercice sans stimulus (séance vide) est ignoré.
     *
     * @return la distribution {@code MuscleGroup → TrainingStimulus} (vide si rien à appliquer)
     */
    public Map<MuscleGroup, TrainingStimulus> distribute(List<ExerciseStimulus> exercises,
                                                         MuscleStimulusMapping mapping) {
        Map<MuscleGroup, Double> accumulator = new EnumMap<>(MuscleGroup.class);
        for (ExerciseStimulus exercise : exercises) {
            double magnitude = from(exercise.sets()).magnitude();
            if (magnitude == 0.0) {
                continue;
            }
            mapping.weightsFor(exercise).forEach((muscle, weight) ->
                    accumulator.merge(muscle, magnitude * weight, Double::sum));
        }
        Map<MuscleGroup, TrainingStimulus> distributed = new EnumMap<>(MuscleGroup.class);
        accumulator.forEach((muscle, value) -> distributed.put(muscle, new TrainingStimulus(value)));
        return distributed;
    }

    /** Facteur d'effort dans [0, 1] : {@code clamp((rpe − 4) / 6)}, avec {@code null} → effort neutre (RPE 7 → 0.5). */
    public static double effortFactor(Double rpe) {
        double value = (rpe == null) ? DEFAULT_EFFORT_RPE : rpe;
        return Math.clamp((value - EFFORT_THRESHOLD_RPE) / (10.0 - EFFORT_THRESHOLD_RPE), 0.0, 1.0);
    }
}
