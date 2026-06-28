package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Map;

import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.BACK_LOWER;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.BACK_UPPER;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.BICEPS;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.CALVES;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.CHEST;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.CORE;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.GLUTES;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.HAMSTRINGS;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.QUADS;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.SHOULDERS;
import static dev.ryanfoerster.atlas.shared.domain.MuscleGroup.TRICEPS;

/**
 * Distribue le stimulus d'un exercice sur les {@link MuscleGroup} qu'il travaille, via des <strong>tables de
 * pondération sourcées</strong> (sprint 5, Couche 2, ADR-030). Domain service pur et stateless. Retourne des
 * poids normalisés (<strong>somme = 1</strong> par exercice) : la magnitude de l'exercice est ensuite
 * répartie selon ces poids par le {@link StimulusCalculator}.
 *
 * <h2>Statut épistémique</h2>
 * L'EMG mesure l'<em>activation</em>, pas le stimulus pour l'adaptation : l'utiliser comme proxy de
 * répartition est un <strong>choix de modélisation Atlas</strong>. Le <em>classement</em> des muscles
 * (primaire / secondaire) est sourcé (EMG, Stronger By Science, biomécanique) ; les <em>nombres exacts</em>
 * sont une interprétation Atlas calibrée pour la plausibilité (même honnêteté que la calibration Banister).
 * Détail des sources et des deux frictions assumées (BACK, FOREARMS) dans ADR-030 et {@code sport-science.md}.
 *
 * <h2>Limite assumée (point « ampleur composé vs isolation »)</h2>
 * Avec somme = 1 et magnitude {@code = reps × effort} (charge absolue exclue jusqu'au sprint 6), un isolé
 * concentre tout son stimulus sur un muscle, un composé le répartit. À reps/RPE égaux, un curl dépose donc
 * plus sur ses biceps qu'un squat sur ses quads. C'est une <strong>limite assumée</strong> : ce qui rend un
 * squat « plus gros » est la charge (140 kg vs 20 kg), modélisée au sprint 6. Par muscle, le poids du prime
 * mover d'un composé (&lt; 1) capture aussi qu'il n'est pas pris aussi près de SON échec individuel qu'un
 * isolé. Pas de facteur d'ampleur ajouté ici (proxy arbitraire d'un trou déjà planifié). Voir ADR-030.
 */
public class MuscleStimulusMapping {

    /**
     * Composés : {@code MovementPattern → poids par muscle} (somme = 1). Classement sourcé EMG/SBS,
     * proportions = interprétation Atlas. Deadlift/chin-up = variante générique (conventionnel / supination
     * assumés). Voir ADR-030 pour le détail par pondération.
     */
    private static final Map<MovementPattern, Map<MuscleGroup, Double>> COMPOUND = Map.of(
            MovementPattern.SQUAT, Map.of(QUADS, 0.42, GLUTES, 0.30, HAMSTRINGS, 0.08, BACK_LOWER, 0.10, CORE, 0.10),
            MovementPattern.BENCH_PRESS, Map.of(CHEST, 0.50, TRICEPS, 0.25, SHOULDERS, 0.25),
            MovementPattern.DEADLIFT, Map.of(BACK_LOWER, 0.25, GLUTES, 0.25, HAMSTRINGS, 0.20, BACK_UPPER, 0.15, QUADS, 0.10, CORE, 0.05),
            MovementPattern.OVERHEAD_PRESS, Map.of(SHOULDERS, 0.55, TRICEPS, 0.25, CHEST, 0.10, CORE, 0.10),
            MovementPattern.ROW, Map.of(BACK_UPPER, 0.55, BICEPS, 0.20, SHOULDERS, 0.10, BACK_LOWER, 0.10, CORE, 0.05),
            MovementPattern.CHIN_UP, Map.of(BACK_UPPER, 0.55, BICEPS, 0.30, SHOULDERS, 0.05, CORE, 0.10));

    /**
     * Accessoires : {@code BodyRegion → poids par muscle} (somme = 1). Cible directe (poids 1.0) sauf les
     * deux frictions assumées (ADR-026/029/030) : {@code BACK} (plus grossier que upper/lower) réparti
     * 80/20, {@code FOREARMS} (sans équivalent modélisé) replié sur {@code BICEPS}.
     */
    private static final Map<BodyRegion, Map<MuscleGroup, Double>> ACCESSORY = Map.ofEntries(
            Map.entry(BodyRegion.BICEPS, Map.of(BICEPS, 1.0)),
            Map.entry(BodyRegion.TRICEPS, Map.of(TRICEPS, 1.0)),
            Map.entry(BodyRegion.SHOULDERS, Map.of(SHOULDERS, 1.0)),
            Map.entry(BodyRegion.CHEST, Map.of(CHEST, 1.0)),
            Map.entry(BodyRegion.CORE, Map.of(CORE, 1.0)),
            Map.entry(BodyRegion.GLUTES, Map.of(GLUTES, 1.0)),
            Map.entry(BodyRegion.HAMSTRINGS, Map.of(HAMSTRINGS, 1.0)),
            Map.entry(BodyRegion.QUADS, Map.of(QUADS, 1.0)),
            Map.entry(BodyRegion.CALVES, Map.of(CALVES, 1.0)),
            Map.entry(BodyRegion.BACK, Map.of(BACK_UPPER, 0.80, BACK_LOWER, 0.20)),
            Map.entry(BodyRegion.FOREARMS, Map.of(BICEPS, 1.0))); // friction assumée : pas de muscle avant-bras modélisé

    /** Poids par muscle pour un exercice (somme = 1), selon sa cible composée ou accessoire. */
    public Map<MuscleGroup, Double> weightsFor(ExerciseStimulus exercise) {
        return (exercise.pattern() != null)
                ? COMPOUND.get(exercise.pattern())
                : ACCESSORY.get(exercise.accessoryRegion());
    }
}
