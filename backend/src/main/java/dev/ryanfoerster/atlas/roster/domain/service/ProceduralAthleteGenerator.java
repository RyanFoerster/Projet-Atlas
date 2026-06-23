package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Génération procédurale d'athlètes, calibrée sur la sport-science (ADR-020, ADR-021).
 *
 * <p><strong>Sources de calibration</strong> (citées sur chaque constante) :
 * <ul>
 *   <li>ExRx.net — <em>Strength Standards</em> (ratios force/poids de corps, 5 niveaux
 *       Untrained→Elite), référence stable de la communauté.</li>
 *   <li>Greg Nuckols / <em>Stronger By Science</em> — standards de force par poids de corps et
 *       discussion des potentiels génétiques.</li>
 * </ul>
 * Là où la littérature ne tranche pas (variance génétique pure, bandes de spécialisation),
 * les valeurs sont marquées <em>calibration par défaut, à revérifier au sprint 4</em> (scénarios
 * Banister).
 *
 * <p><strong>Déterminisme</strong> : tout dérive d'un {@link Random} construit sur le {@code seed}
 * passé en paramètre — jamais de hasard caché (testabilité).
 */
public final class ProceduralAthleteGenerator implements AthleteGenerator {

    private static final List<MovementPattern> BIG_LIFTS =
            List.of(MovementPattern.SQUAT, MovementPattern.BENCH_PRESS,
                    MovementPattern.DEADLIFT, MovementPattern.OVERHEAD_PRESS);

    /** Affinité neutre (= moyenne, ni douée ni faible). */
    private static final double NEUTRAL_AFFINITY = 1.0;
    /**
     * Plafond du boost de force du miroir. Volontairement < {@code STRENGTH_MAX} (1.25) : une force
     * VÉRIFIÉE implique une affinité élevée, jamais maximale — la frange 1.20–1.25 reste réservée à
     * l'aléa génétique pur. (Le plafond dur de {@link Genetics} est 1.25 ; on sature donc bien avant.)
     */
    private static final double MIRROR_BOOST_CEILING = 1.20;

    // Ratios 1RM/poids de corps, HOMME adulte, [intermédiaire (début du signal de talent) ; elite
    // (saturation du boost)]. Sources : ExRx.net Strength Standards + Nuckols/Stronger By Science.
    private static final double[] SQUAT_MALE = {1.5, 2.3};
    private static final double[] BENCH_MALE = {1.0, 1.65};
    private static final double[] DEADLIFT_MALE = {1.75, 2.7};
    private static final double[] OHP_MALE = {0.6, 1.0};
    // Ratios féminins vs masculins (ExRx) : ~0.65 sur le haut du corps, ~0.75 sur le bas du corps.
    private static final double FEMALE_UPPER_FACTOR = 0.65;
    private static final double FEMALE_LOWER_FACTOR = 0.75;

    // Pools de noms (procédural ; agrandis librement plus tard).
    private static final String[] FIRST_NAMES = {
            "Marcus", "Elena", "Viktor", "Nadia", "Sasha", "Lena", "Dmitri", "Yara",
            "Tomas", "Ingrid", "Kai", "Mara", "Boris", "Aïcha", "Niels", "Petra"};
    private static final String[] LAST_NAMES = {
            "Vélaris", "Kane", "Sorokin", "Halden", "Okonkwo", "Reyes", "Larsson", "Voss",
            "Petrov", "Dubois", "Nakamura", "Brandt", "Costa", "Ivanova", "Mensah", "Roux"};

    // ----- Génétique miroir (hybride, ADR-021) ------------------------------------------------

    @Override
    public Genetics generateGeneticsForMirror(Map<MovementPattern, OneRepMax> oneRepMaxes,
                                              Weight bodyWeight, Gender gender, long seed) {
        Random rng = new Random(seed);
        Map<MuscleGroup, Double> hypertrophy = randomFullRangeHypertrophy(rng);
        Map<MovementPattern, Double> strength = randomFullRangeStrength(rng);
        double recovery = uniform(rng, Genetics.RECOVERY_MIN, Genetics.RECOVERY_MAX);
        double fiber = rng.nextDouble();
        double sensitivity = uniform(rng, Genetics.SENSITIVITY_MIN, Genetics.SENSITIVITY_MAX);

        double bodyWeightKg = bodyWeight.toKilograms().doubleValue();
        for (MovementPattern pattern : BIG_LIFTS) {
            OneRepMax oneRm = oneRepMaxes.get(pattern);
            if (oneRm == null) {
                continue; // pas de 1RM saisi pour ce pattern → axe laissé au hasard
            }
            double ratio = oneRm.weight().toKilograms().doubleValue() / bodyWeightKg;
            double talent = talentFromRatio(ratio, pattern, gender);
            double boostTarget = NEUTRAL_AFFINITY + talent * (MIRROR_BOOST_CEILING - NEUTRAL_AFFINITY);
            // On ne fait que RELEVER (une force vérifiée ne peut pas baisser une bonne génétique
            // aléatoire), plafonné au max de Genetics.
            double boosted = Math.min(Math.max(strength.get(pattern), boostTarget), Genetics.STRENGTH_MAX);
            strength.put(pattern, boosted);
        }
        return new Genetics(hypertrophy, strength, recovery, fiber, sensitivity);
    }

    /** Niveau de talent [0,1] déduit du ratio force/BW, par rapport aux standards du pattern et du genre. */
    private double talentFromRatio(double ratio, MovementPattern pattern, Gender gender) {
        double[] thresholds = thresholdsFor(pattern, gender);
        double t = (ratio - thresholds[0]) / (thresholds[1] - thresholds[0]);
        return clamp(t, 0.0, 1.0);
    }

    private double[] thresholdsFor(MovementPattern pattern, Gender gender) {
        double[] base = switch (pattern) {
            case SQUAT -> SQUAT_MALE;
            case BENCH_PRESS -> BENCH_MALE;
            case DEADLIFT -> DEADLIFT_MALE;
            case OVERHEAD_PRESS -> OHP_MALE;
            default -> throw new IllegalArgumentException("Pas de standard pour " + pattern);
        };
        if (gender == Gender.FEMALE) {
            double factor = (pattern == MovementPattern.BENCH_PRESS || pattern == MovementPattern.OVERHEAD_PRESS)
                    ? FEMALE_UPPER_FACTOR : FEMALE_LOWER_FACTOR;
            return new double[] {base[0] * factor, base[1] * factor};
        }
        return base;
    }

    // ----- Candidat virtuel (rareté = spécialisation, ADR-020) --------------------------------

    @Override
    public AthleteCandidate generateCandidate(long seed, Rarity rarity) {
        Random rng = new Random(seed);
        Gender gender = rng.nextBoolean() ? Gender.MALE : Gender.FEMALE;
        AthleteName name = randomName(rng);
        int age = 16 + rng.nextInt(35); // 16..50 inclus
        Weight bodyWeight = randomBodyWeight(rng, gender);
        Height bodyHeight = randomHeight(rng, gender);
        Genetics genetics = specializedGenetics(rng, rarity);
        Map<MovementPattern, OneRepMax> base = deriveOneRepMaxes(genetics, bodyWeight, gender);
        return new AthleteCandidate(name, age, bodyWeight, bodyHeight, gender, genetics, base, rarity);
    }

    /**
     * Génétique d'un candidat selon son tier. <strong>La rareté est de la spécialisation</strong> :
     * la base reste neutre, et seuls les tiers ≥ PROMISING <em>pointent un seul axe</em> (un pattern
     * de force OU un groupe musculaire) d'autant plus haut que le tier est rare. Un PRODIGY est
     * exceptionnel sur UN axe, pas « 99 partout ». Bandes <em>calibration par défaut, à revérifier
     * sprint 4</em>.
     */
    private Genetics specializedGenetics(Random rng, Rarity rarity) {
        // Variance de base : élargie pour PRODIGY (« les autres axes restent variables »).
        double baseLo = rarity == Rarity.PRODIGY ? 0.85 : 0.92;
        double baseHi = 1.10;
        Map<MuscleGroup, Double> hypertrophy = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup g : MuscleGroup.values()) {
            hypertrophy.put(g, uniform(rng, baseLo, baseHi));
        }
        Map<MovementPattern, Double> strength = new EnumMap<>(MovementPattern.class);
        for (MovementPattern p : MovementPattern.values()) {
            strength.put(p, uniform(rng, baseLo, baseHi));
        }

        // Pointe sur un seul axe (sauf GENERIC = équilibré).
        double[] spikeBand = switch (rarity) {
            case GENERIC -> null;
            case PROMISING -> new double[] {1.10, 1.18};
            case SPECIALIST -> new double[] {1.15, 1.22};
            case PRODIGY -> new double[] {1.22, 1.25};
        };
        if (spikeBand != null) {
            boolean spikeStrength = rng.nextBoolean();
            if (spikeStrength) {
                MovementPattern p = MovementPattern.values()[rng.nextInt(MovementPattern.values().length)];
                strength.put(p, clamp(uniform(rng, spikeBand[0], spikeBand[1]), Genetics.STRENGTH_MIN, Genetics.STRENGTH_MAX));
            } else {
                MuscleGroup g = MuscleGroup.values()[rng.nextInt(MuscleGroup.values().length)];
                // L'hypertrophie monte plus haut (plage 0.85–1.30) : on décale la bande vers le haut.
                hypertrophy.put(g, clamp(uniform(rng, spikeBand[0] + 0.05, spikeBand[1] + 0.05),
                        Genetics.HYPERTROPHY_MIN, Genetics.HYPERTROPHY_MAX));
            }
        }

        double recovery = uniform(rng, Genetics.RECOVERY_MIN, Genetics.RECOVERY_MAX);
        double fiber = rng.nextDouble();
        double sensitivity = uniform(rng, Genetics.SENSITIVITY_MIN, Genetics.SENSITIVITY_MAX);
        return new Genetics(hypertrophy, strength, recovery, fiber, sensitivity);
    }

    /**
     * Dérive des 1RM cohérents avec la génétique et le poids de corps : {@code 1RM = poids ×
     * ratio_intermédiaire × affinité}. Un athlète à forte affinité de force soulève au-dessus du
     * niveau intermédiaire. Arrondi au 2.5 kg.
     */
    private Map<MovementPattern, OneRepMax> deriveOneRepMaxes(Genetics genetics, Weight bodyWeight, Gender gender) {
        double bodyWeightKg = bodyWeight.toKilograms().doubleValue();
        Map<MovementPattern, OneRepMax> result = new EnumMap<>(MovementPattern.class);
        for (MovementPattern pattern : BIG_LIFTS) {
            double baselineRatio = thresholdsFor(pattern, gender)[0]; // niveau intermédiaire (genré)
            double oneRmKg = roundToNearest(bodyWeightKg * baselineRatio * genetics.strengthAffinity(pattern), 2.5);
            result.put(pattern, OneRepMax.measured(Weight.ofKilograms(oneRmKg)));
        }
        return result;
    }

    // ----- Helpers aléatoires (bornés aux plages de Genetics) ---------------------------------

    private Map<MuscleGroup, Double> randomFullRangeHypertrophy(Random rng) {
        Map<MuscleGroup, Double> map = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup g : MuscleGroup.values()) {
            map.put(g, uniform(rng, Genetics.HYPERTROPHY_MIN, Genetics.HYPERTROPHY_MAX));
        }
        return map;
    }

    private Map<MovementPattern, Double> randomFullRangeStrength(Random rng) {
        Map<MovementPattern, Double> map = new EnumMap<>(MovementPattern.class);
        for (MovementPattern p : MovementPattern.values()) {
            map.put(p, uniform(rng, Genetics.STRENGTH_MIN, Genetics.STRENGTH_MAX));
        }
        return map;
    }

    private AthleteName randomName(Random rng) {
        String first = FIRST_NAMES[rng.nextInt(FIRST_NAMES.length)];
        String last = LAST_NAMES[rng.nextInt(LAST_NAMES.length)];
        return AthleteName.of(first + " " + last);
    }

    private Weight randomBodyWeight(Random rng, Gender gender) {
        double kg = gender == Gender.MALE ? uniform(rng, 65, 120) : uniform(rng, 50, 95);
        return Weight.ofKilograms(roundToNearest(kg, 0.5));
    }

    private Height randomHeight(Random rng, Gender gender) {
        int cm = gender == Gender.MALE ? 165 + rng.nextInt(36) : 155 + rng.nextInt(34);
        return Height.ofCentimeters(cm);
    }

    private static double uniform(Random rng, double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double roundToNearest(double value, double step) {
        return Math.round(value / step) * step;
    }
}
