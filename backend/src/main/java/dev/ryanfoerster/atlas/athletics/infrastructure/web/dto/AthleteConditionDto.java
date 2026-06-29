package dev.ryanfoerster.atlas.athletics.infrastructure.web.dto;

import dev.ryanfoerster.atlas.athletics.application.query.GetAthleteConditionUseCase.CurrentCondition;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO de la condition d'un athlète exposée à l'UI. En plus des valeurs brutes du modèle (échelle interne
 * NORM, ADR-028), il calcule un <strong>indice de Forme 0–100</strong> et un <strong>état</strong> lisibles
 * — c'est la couche de présentation qui normalise, pas le domaine.
 *
 * <p><strong>Indice de Forme</strong> : {@code 50 + 50 · (performance / fitness)}, clampé [0,100].
 * Le ratio {@code performance/fitness = k1 − k2·(fatigue/fitness)} est <strong>indépendant de NORM</strong>
 * (numérateur et dénominateur portent la même échelle), ce qui donne une mesure stable : 100 = pleinement
 * affûté (fatigue nulle), 50 = neutre, &lt; 50 = sur-fatigué (peut tomber à 0 juste après une grosse séance,
 * « cuit »). Athlète sans données (fitness ≈ 0) → 50 (neutre).
 */
public record AthleteConditionDto(
        String athleteId,
        double fitness,
        double fatigue,
        double performance,
        int formIndex,
        String formState,
        Instant asOf,
        Map<String, Double> baselineOneRmKgByPattern) {

    public static final String CUIT = "CUIT";
    public static final String FRAIS = "FRAIS";
    public static final String AFFUTE = "AFFUTE";

    public static AthleteConditionDto from(CurrentCondition condition) {
        int index = formIndex(condition.fitness(), condition.performance());
        return new AthleteConditionDto(
                condition.athleteId().toString(),
                round2(condition.fitness()),
                round2(condition.fatigue()),
                round2(condition.performance()),
                index,
                formState(index),
                condition.asOf(),
                toBaselineMap(condition.baselineOneRmKgByPattern()));
    }

    /**
     * Baselines de progression structurelle (1RM de départ figé par pattern), clés = noms d'enum. Pleine
     * précision (pas d'arrondi) : c'est le front qui calcule {@code delta = courant − baseline} puis arrondit
     * le delta à 1 décimale. {@link LinkedHashMap} pour un ordre de sérialisation stable.
     */
    private static Map<String, Double> toBaselineMap(Map<MovementPattern, Double> baselines) {
        Map<String, Double> json = new LinkedHashMap<>();
        baselines.forEach((pattern, kg) -> json.put(pattern.name(), kg));
        return json;
    }

    private static int formIndex(double fitness, double performance) {
        if (fitness < 1e-9) {
            return 50; // pas de données → neutre
        }
        double ratio = performance / fitness; // ∈ (−∞, k1], indépendant de NORM
        long index = Math.round(50 + 50 * ratio);
        return (int) Math.clamp(index, 0, 100);
    }

    private static String formState(int index) {
        if (index < 40) {
            return CUIT;
        }
        if (index <= 60) {
            return FRAIS;
        }
        return AFFUTE;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
