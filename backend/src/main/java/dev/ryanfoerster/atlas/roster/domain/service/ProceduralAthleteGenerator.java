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

import java.util.ArrayList;
import java.util.Collections;
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

    // Les ratios de force (bandes [intermédiaire, élite], genrées) vivent dans StrengthStandards — source
    // unique partagée avec le calcul du plafond génétique côté query (Couche 3, T3 du sprint 6).

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
        double[] thresholds = StrengthStandards.ratioBand(pattern, gender);
        double t = (ratio - thresholds[0]) / (thresholds[1] - thresholds[0]);
        return clamp(t, 0.0, 1.0);
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
        // Tiers RENDUS DISTINCTS (ADR-020) par le NOMBRE d'axes spécialisés ET la magnitude du spike
        // — pas seulement la magnitude, sinon la frontière Promising/Specialist serait floue. La base
        // = axes « moyens » ; PRODIGY a une base un peu plus variable.
        double baseLo = rarity == Rarity.PRODIGY ? 0.85 : 0.88;
        double baseHi = rarity == Rarity.PRODIGY ? 1.12 : 1.05;
        int spikeCount = switch (rarity) {
            case GENERIC -> 0;
            case PROMISING -> 1;
            case SPECIALIST -> 2;
            case PRODIGY -> 1;
        };
        double[] spikeBand = switch (rarity) {            // bande du/des axe(s) spécialisé(s), orientée force
            case GENERIC -> null;
            case PROMISING -> new double[] {1.08, 1.16};  // un axe modeste
            case SPECIALIST -> new double[] {1.12, 1.22}; // deux axes francs
            case PRODIGY -> new double[] {1.20, 1.25};    // un axe exceptionnel
        };

        Map<MuscleGroup, Double> hypertrophy = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup g : MuscleGroup.values()) {
            hypertrophy.put(g, clamp(uniform(rng, baseLo, baseHi), Genetics.HYPERTROPHY_MIN, Genetics.HYPERTROPHY_MAX));
        }
        Map<MovementPattern, Double> strength = new EnumMap<>(MovementPattern.class);
        for (MovementPattern p : MovementPattern.values()) {
            strength.put(p, clamp(uniform(rng, baseLo, baseHi), Genetics.STRENGTH_MIN, Genetics.STRENGTH_MAX));
        }

        if (spikeBand != null) {
            // spikeCount axes DISTINCTS, piochés dans le pool combiné (patterns de force + groupes
            // musculaires) via un shuffle seedé (déterminisme).
            int patternCount = MovementPattern.values().length;
            List<Integer> pool = new ArrayList<>();
            for (int i = 0; i < patternCount + MuscleGroup.values().length; i++) {
                pool.add(i);
            }
            Collections.shuffle(pool, rng);
            for (int k = 0; k < spikeCount; k++) {
                int idx = pool.get(k);
                if (idx < patternCount) {
                    MovementPattern p = MovementPattern.values()[idx];
                    strength.put(p, clamp(uniform(rng, spikeBand[0], spikeBand[1]),
                            Genetics.STRENGTH_MIN, Genetics.STRENGTH_MAX));
                } else {
                    MuscleGroup g = MuscleGroup.values()[idx - patternCount];
                    // L'hypertrophie monte plus haut (max 1.30) : bande décalée de +0.05.
                    hypertrophy.put(g, clamp(uniform(rng, spikeBand[0] + 0.05, spikeBand[1] + 0.05),
                            Genetics.HYPERTROPHY_MIN, Genetics.HYPERTROPHY_MAX));
                }
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
            double baselineRatio = StrengthStandards.intermediateRatio(pattern, gender); // niveau intermédiaire (genré)
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
