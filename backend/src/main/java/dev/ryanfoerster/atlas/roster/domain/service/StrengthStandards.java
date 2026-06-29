package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

/**
 * <strong>Source unique</strong> des standards de force d'Atlas : les ratios 1RM/poids de corps de la
 * littérature, par pattern et par genre. Service de domaine pur (zéro framework), fonctions statiques sur
 * une table de constantes — comme {@code StimulusCalculator.loadFactor}.
 *
 * <p>Chaque pattern « grand lift » a une <strong>bande</strong> {@code [intermédiaire, élite]} :
 * <ul>
 *   <li>la borne <strong>intermédiaire</strong> ancre le 1RM de <em>départ</em> d'un athlète généré
 *       ({@code ProceduralAthleteGenerator.deriveOneRepMaxes}) et le seuil bas du talent miroir ;</li>
 *   <li>la borne <strong>élite</strong> ancre le <em>plafond génétique</em> de la progression structurelle
 *       (Couche 3, ADR-033 : {@code plafond = bodyweight × ratio_élite × strengthAffinity}).</li>
 * </ul>
 *
 * <p>Seuls les <strong>4 grands lifts</strong> (squat, développé couché, soulevé de terre, développé
 * militaire) ont un standard — ce sont les seuls 1RM suivis dans {@code CurrentStats} (Sprint 2). {@code ROW}
 * et {@code CHIN_UP} n'en ont pas : {@link #hasStandard} renvoie {@code false} (ADR-033 §5). C'est cette
 * classe qui possède les ratios élite, pour que ni Athletics ni qui que ce soit d'autre ne les duplique
 * (T3 du sprint 6 : Roster calcule le plafond, Athletics le lit).
 *
 * <h2>Sources</h2>
 * ExRx.net Strength Standards ; Nuckols / Stronger By Science. Facteurs féminins ExRx (~0.65 haut du corps,
 * ~0.75 bas du corps).
 */
public final class StrengthStandards {

    // Ratios 1RM/poids de corps, HOMME adulte, [intermédiaire ; élite].
    private static final double[] SQUAT_MALE = {1.5, 2.3};
    private static final double[] BENCH_MALE = {1.0, 1.65};
    private static final double[] DEADLIFT_MALE = {1.75, 2.7};
    private static final double[] OHP_MALE = {0.6, 1.0};

    // Ratios féminins vs masculins (ExRx) : ~0.65 sur le haut du corps, ~0.75 sur le bas du corps.
    private static final double FEMALE_UPPER_FACTOR = 0.65;
    private static final double FEMALE_LOWER_FACTOR = 0.75;

    private StrengthStandards() {
    }

    /** Vrai si le pattern a un standard de force (les 4 grands lifts), donc un 1RM suivi et un plafond. */
    public static boolean hasStandard(MovementPattern pattern) {
        return switch (pattern) {
            case SQUAT, BENCH_PRESS, DEADLIFT, OVERHEAD_PRESS -> true;
            case ROW, CHIN_UP -> false;
        };
    }

    /**
     * Bande {@code [ratio_intermédiaire, ratio_élite]} (1RM/poids de corps), genrée.
     *
     * @throws IllegalArgumentException si le pattern n'a pas de standard ({@link #hasStandard} faux)
     */
    public static double[] ratioBand(MovementPattern pattern, Gender gender) {
        double[] base = switch (pattern) {
            case SQUAT -> SQUAT_MALE;
            case BENCH_PRESS -> BENCH_MALE;
            case DEADLIFT -> DEADLIFT_MALE;
            case OVERHEAD_PRESS -> OHP_MALE;
            default -> throw new IllegalArgumentException("Pas de standard de force pour " + pattern);
        };
        if (gender == Gender.FEMALE) {
            double factor = (pattern == MovementPattern.BENCH_PRESS || pattern == MovementPattern.OVERHEAD_PRESS)
                    ? FEMALE_UPPER_FACTOR : FEMALE_LOWER_FACTOR;
            return new double[] {base[0] * factor, base[1] * factor};
        }
        return base.clone();
    }

    /** Ratio <strong>intermédiaire</strong> (borne basse) — ancre le 1RM de départ. */
    public static double intermediateRatio(MovementPattern pattern, Gender gender) {
        return ratioBand(pattern, gender)[0];
    }

    /** Ratio <strong>élite</strong> (borne haute) — ancre le plafond génétique (ADR-033). */
    public static double eliteRatio(MovementPattern pattern, Gender gender) {
        return ratioBand(pattern, gender)[1];
    }
}
