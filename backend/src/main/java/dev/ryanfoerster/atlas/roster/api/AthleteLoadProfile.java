package dev.ryanfoerster.atlas.roster.api;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Map;

/**
 * Snapshot des données de <strong>charge</strong> d'un athlète, exposé à Athletics pour calculer le %1RM
 * d'une série (sprint 6, ADR-034) : son poids de corps et ses 1RM par pattern, en kg (primitifs — l'API ne
 * fuit pas les VO internes de Roster, cohérent avec {@link GeneticProfile}).
 *
 * <p><strong>Lecture FRAÎCHE, pas dénormalisée</strong> — la différence clé avec {@link GeneticProfile} : les
 * 1RM (et le poids de corps) sont <em>mutables</em> (le 1RM progresse à partir du sprint 6, Couche 3),
 * contrairement à la {@code Genetics} immutable. Athletics doit donc relire ce profil <em>à chaque calcul de
 * stimulus</em> pour ne pas travailler sur une valeur périmée. C'est la leçon « ne pas dénormaliser ce qui
 * change » (option D du sprint 3) appliquée à l'opposé du sprint 5.
 */
public record AthleteLoadProfile(double bodyWeightKg, Map<MovementPattern, Double> oneRepMaxKgByPattern) {

    public AthleteLoadProfile {
        oneRepMaxKgByPattern = Map.copyOf(oneRepMaxKgByPattern);
    }

    /** Le 1RM (kg) du pattern, ou {@code null} si aucun 1RM n'est connu (map sparse → %1RM au plancher). */
    public Double oneRepMaxKg(MovementPattern pattern) {
        return oneRepMaxKgByPattern.get(pattern);
    }

    /** Profil inconnu (athlète sans données) : aucun 1RM → tout au plancher de {@code loadFactor}. */
    public static final AthleteLoadProfile UNKNOWN = new AthleteLoadProfile(0.0, Map.of());
}
