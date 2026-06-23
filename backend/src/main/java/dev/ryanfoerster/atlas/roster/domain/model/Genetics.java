package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidGeneticsException;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Map;
import java.util.Objects;

/**
 * Profil génétique immutable d'un athlète (cf. glossaire, CLAUDE.md §4). Fixé à la création,
 * jamais modifié. Cinq axes :
 * <ul>
 *   <li>{@code hypertrophyPotentialByMuscleGroup} : potentiel d'hypertrophie par groupe (0.85–1.30) ;</li>
 *   <li>{@code strengthAffinityByPattern} : affinité de force par pattern (0.80–1.25) ;</li>
 *   <li>{@code baseRecoveryRate} : vitesse de récupération de base (0.85–1.20) ;</li>
 *   <li>{@code fiberTypeProfile} : 0 (pure endurance) à 1 (pure force) ;</li>
 *   <li>{@code trainingResponseSensitivity} : bruit dans la réponse à l'entraînement
 *       (0.85–1.15 — <em>calibration par défaut, à revérifier au sprint 4 avec les scénarios Banister</em>).</li>
 * </ul>
 *
 * <p><strong>Immutabilité réelle (décision #4 du co-affinage)</strong> : les {@code Map} sont
 * recopiées défensivement via {@link Map#copyOf} dans le constructeur canonique — sinon une référence
 * mutable fuirait et « record immutable » serait un mensonge. Les deux maps doivent couvrir
 * <em>tous</em> les {@link MuscleGroup}/{@link MovementPattern} (invariant de complétude : aucun axe
 * manquant).
 */
public record Genetics(
        Map<MuscleGroup, Double> hypertrophyPotentialByMuscleGroup,
        Map<MovementPattern, Double> strengthAffinityByPattern,
        double baseRecoveryRate,
        double fiberTypeProfile,
        double trainingResponseSensitivity) {

    public static final double HYPERTROPHY_MIN = 0.85;
    public static final double HYPERTROPHY_MAX = 1.30;
    public static final double STRENGTH_MIN = 0.80;
    public static final double STRENGTH_MAX = 1.25;
    public static final double RECOVERY_MIN = 0.85;
    public static final double RECOVERY_MAX = 1.20;
    // Calibration par défaut, à revérifier au sprint 4 (scénarios Banister).
    public static final double SENSITIVITY_MIN = 0.85;
    public static final double SENSITIVITY_MAX = 1.15;

    public Genetics {
        Objects.requireNonNull(hypertrophyPotentialByMuscleGroup, "hypertrophyPotentialByMuscleGroup");
        Objects.requireNonNull(strengthAffinityByPattern, "strengthAffinityByPattern");

        requireCompleteAndInRange(hypertrophyPotentialByMuscleGroup, MuscleGroup.values(),
                HYPERTROPHY_MIN, HYPERTROPHY_MAX, "hypertrophyPotential");
        requireCompleteAndInRange(strengthAffinityByPattern, MovementPattern.values(),
                STRENGTH_MIN, STRENGTH_MAX, "strengthAffinity");
        requireInRange(baseRecoveryRate, RECOVERY_MIN, RECOVERY_MAX, "baseRecoveryRate");
        if (fiberTypeProfile < 0.0 || fiberTypeProfile > 1.0) {
            throw new InvalidGeneticsException("fiberTypeProfile doit être dans [0,1] : " + fiberTypeProfile);
        }
        requireInRange(trainingResponseSensitivity, SENSITIVITY_MIN, SENSITIVITY_MAX, "trainingResponseSensitivity");

        // Copie défensive → immutabilité réelle.
        hypertrophyPotentialByMuscleGroup = Map.copyOf(hypertrophyPotentialByMuscleGroup);
        strengthAffinityByPattern = Map.copyOf(strengthAffinityByPattern);
    }

    public double hypertrophyPotential(MuscleGroup muscleGroup) {
        return hypertrophyPotentialByMuscleGroup.get(muscleGroup);
    }

    public double strengthAffinity(MovementPattern pattern) {
        return strengthAffinityByPattern.get(pattern);
    }

    private static <K extends Enum<K>> void requireCompleteAndInRange(
            Map<K, Double> map, K[] allKeys, double min, double max, String axis) {
        for (K key : allKeys) {
            Double v = map.get(key);
            if (v == null) {
                throw new InvalidGeneticsException(axis + " : axe manquant pour " + key);
            }
            requireInRange(v, min, max, axis + "[" + key + "]");
        }
        if (map.size() != allKeys.length) {
            throw new InvalidGeneticsException(axis + " : clés inattendues (taille " + map.size()
                    + " ≠ " + allKeys.length + ")");
        }
    }

    private static void requireInRange(double value, double min, double max, String axis) {
        if (value < min || value > max) {
            throw new InvalidGeneticsException(axis + " hors plage [" + min + "," + max + "] : " + value);
        }
    }
}
