package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule la magnitude du {@link TrainingStimulus} d'une séance. Domain service pur et stateless.
 * <strong>LA décision de modélisation du sprint</strong> (ADR-028, sport-science.md).
 *
 * <h2>Formule (sprint 6, ADR-034)</h2>
 * <pre>{@code  S = NORMALIZATION × Σ_séries ( reps × effort(rpe) × load(%1RM) )}</pre>
 *
 * Les <strong>trois</strong> variables de dose de la musculation :
 * <ul>
 *   <li><strong>volume</strong> = {@code reps} (driver primaire — Schoenfeld &amp; Krieger) ;</li>
 *   <li><strong>intensité d'effort</strong> = {@code effort(rpe)} (proximité de l'échec — Helms RIR /
 *       Nuckols « stimulating reps ») ;</li>
 *   <li><strong>intensité de charge</strong> = {@code load(%1RM)} (tension mécanique — sprint 6).</li>
 * </ul>
 *
 * <h2>{@code effort} et {@code load} sont orthogonaux (pas de double comptage)</h2>
 * {@code effort(rpe)} mesure la <em>proximité de l'échec</em> — ce que le RPE voit. {@code load(%1RM)} mesure
 * la <em>tension mécanique</em> — ce que le RPE <strong>ignore</strong> (à RPE 8, le RPE ne sait pas le poids
 * sur la barre). À RPE égal, deux séries de charges opposées (20 reps @50% vs 2 reps @90%) ont le même
 * {@code effort} mais un {@code load} différent : la dimension charge ajoute une info réelle, sans doubler
 * l'intensité. C'est aussi ce qui <strong>résout le point d'ampleur</strong> (sprint 5) : un squat lourd
 * génère plus de stimulus qu'un curl léger, naturellement via {@code load}.
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
     * l'impulsion) — elle ne fixe que l'échelle verticale. Recalibrée au GATE 2 du sprint 5 (0.01 → 0.013),
     * puis au GATE de la Couche 2 du sprint 6 (<strong>0.013 → 0.014</strong>) : l'ajout de {@code load(%1RM)}
     * réduit légèrement les magnitudes (le travail utile vit à 70-90 % où {@code load ≈ 0.8-1.0}, donc le
     * bump est minime), NORM compense pour garder l'échelle des courbes (ADR-034). Recalibrage légitime.
     */
    public static final double NORMALIZATION = 0.014;

    /** Plancher du {@code loadFactor} : charge très légère (&lt; {@link #LOAD_PCT_FLOOR}) ou %1RM inconnu. */
    public static final double LOAD_FACTOR_FLOOR = 0.40;

    /** Sous ce %1RM, {@code load} reste au plancher (tension faible). */
    public static final double LOAD_PCT_FLOOR = 0.30;

    /** À/au-dessus de ce %1RM, {@code load} plafonne à 1.0 (tension maximale). */
    public static final double LOAD_PCT_CEIL = 0.90;

    /** Magnitude d'un bloc de séries = NORM × Σ (reps × effort(rpe) × load(%1RM)). Brique du calcul par exercice. */
    public TrainingStimulus from(Collection<SetEffort> sets) {
        double raw = 0.0;
        for (SetEffort set : sets) {
            raw += set.reps() * effortFactor(set.rpe()) * loadFactor(set.percentOneRepMax());
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

    /**
     * Agrège le stimulus <strong>par {@link MovementPattern}</strong> (composés uniquement) — l'alimentation
     * de l'accumulateur chronique de la progression structurelle (Couche 3, ADR-033). Pour chaque exercice
     * composé : magnitude {@code = NORM × Σ(reps × effort × load)}, sommée par pattern sur toute la séance.
     *
     * <p>Les <strong>accessoires</strong> (cible = {@code BodyRegion}, sans pattern) sont <strong>exclus</strong>
     * : ils ne font pas progresser un 1RM directement (spécificité / SAID). L'effet hypertrophie → force d'un
     * composé est un effet de 2ⁿᵈ ordre, hors-scope de cette couche (simplification assumée). À comparer avec
     * {@link #distribute} qui, lui, répartit <em>aussi</em> les accessoires sur les muscles pour la forme.
     *
     * @return la distribution {@code MovementPattern → TrainingStimulus} (vide si aucun composé stimulant)
     */
    public Map<MovementPattern, TrainingStimulus> byPattern(List<ExerciseStimulus> exercises) {
        Map<MovementPattern, Double> accumulator = new EnumMap<>(MovementPattern.class);
        for (ExerciseStimulus exercise : exercises) {
            if (exercise.pattern() == null) {
                continue; // accessoire → ne pilote pas la progression du 1RM
            }
            double magnitude = from(exercise.sets()).magnitude();
            if (magnitude == 0.0) {
                continue;
            }
            accumulator.merge(exercise.pattern(), magnitude, Double::sum);
        }
        Map<MovementPattern, TrainingStimulus> byPattern = new EnumMap<>(MovementPattern.class);
        accumulator.forEach((pattern, value) -> byPattern.put(pattern, new TrainingStimulus(value)));
        return byPattern;
    }

    /** Facteur d'effort dans [0, 1] : {@code clamp((rpe − 4) / 6)}, avec {@code null} → effort neutre (RPE 7 → 0.5). */
    public static double effortFactor(Double rpe) {
        double value = (rpe == null) ? DEFAULT_EFFORT_RPE : rpe;
        return Math.clamp((value - EFFORT_THRESHOLD_RPE) / (10.0 - EFFORT_THRESHOLD_RPE), 0.0, 1.0);
    }

    /**
     * Facteur de charge dans [{@link #LOAD_FACTOR_FLOOR}, 1] : clampé-linéaire en %1RM, parallèle à
     * {@code effortFactor}. {@code floor + (1 − floor) × clamp((%1RM − 0.30) / (0.90 − 0.30))}. Le plancher
     * (≥ 0.40) évite d'annuler le travail léger haut-volume ; le plafond à ≥ 90 % reflète la tension maximale.
     *
     * <p>{@code percentOneRepMax} {@code null} → <strong>plancher</strong> : pas de 1RM de référence
     * (accessoire, ou composé sans 1RM connu). On crédite l'effort (RPE) mais pas une tension qu'on ne peut
     * mesurer. Choix tranché au GATE Couche 2 (ADR-034), validé sur simulation (œil de lifter).
     */
    public static double loadFactor(Double percentOneRepMax) {
        if (percentOneRepMax == null) {
            return LOAD_FACTOR_FLOOR;
        }
        double ramp = Math.clamp((percentOneRepMax - LOAD_PCT_FLOOR) / (LOAD_PCT_CEIL - LOAD_PCT_FLOOR), 0.0, 1.0);
        return LOAD_FACTOR_FLOOR + (1.0 - LOAD_FACTOR_FLOOR) * ramp;
    }
}
