package dev.ryanfoerster.atlas.roster.api;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Map;

/**
 * Le <strong>plafond génétique</strong> de force d'un athlète, par pattern, en kilogrammes ({@code bodyweight
 * × ratio_élite × strengthAffinity}). Calculé par Roster — seul détenteur des standards de force
 * ({@code StrengthStandards}) et de la génétique — et lu par Athletics pour borner la progression structurelle
 * (Couche 3, ADR-033). T3 du sprint 6 : Roster calcule le plafond, Athletics le lit (pas de duplication des
 * ratios élite côté Athletics).
 *
 * <p><strong>Dénormalisable, contrairement au 1RM courant.</strong> Le plafond est <em>immutable</em> (la
 * génétique et le poids de corps ne bougent pas ce sprint) → Athletics le lit <strong>une fois à
 * l'initialisation</strong> d'un pattern, pas à chaque séance. C'est le pendant de {@link AthleteLoadProfile}
 * (le 1RM courant, <em>mutable</em>, lu frais à chaque calcul de %1RM) : on lit frais ce qui change, on
 * dénormalise ce qui ne change pas (T5). D'où une méthode de port distincte, aux cycles de vie séparés.
 *
 * <p>Ne contient que les patterns dotés d'un standard (les 4 grands lifts) : {@code ROW}/{@code CHIN_UP}
 * sont absents → pas de plafond → pas de progression structurelle (ADR-033 §5).
 */
public record AthleteStrengthCeiling(Map<MovementPattern, Double> ceilingOneRmKgByPattern) {

    public AthleteStrengthCeiling {
        ceilingOneRmKgByPattern = Map.copyOf(ceilingOneRmKgByPattern);
    }

    /** Le plafond du pattern en kg, ou {@code null} s'il n'a pas de standard de force (ROW/CHIN_UP). */
    public Double ceilingOneRmKg(MovementPattern pattern) {
        return ceilingOneRmKgByPattern.get(pattern);
    }

    /** Athlète introuvable / sans plafond connu : aucune progression structurelle possible. */
    public static final AthleteStrengthCeiling UNKNOWN = new AthleteStrengthCeiling(Map.of());
}
