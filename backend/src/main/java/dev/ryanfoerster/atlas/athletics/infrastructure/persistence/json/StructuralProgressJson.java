package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Map;

/**
 * Forme sérialisable (JSONB) de l'accumulateur de progression structurelle d'un athlète : la
 * {@code Map<MovementPattern, PatternProgress>} de {@code StructuralProgress} (colonne
 * {@code structural_progress}). Wrapper record — même convention que {@code MuscleConditionsJson} : le type
 * de colonne est un record, pas une {@code Map} nue. Les clés enum sont sérialisées par Jackson sous leur nom.
 * Un athlète sans progression encore entamée sérialise {@code {"byPattern":{}}} (défaut de la colonne, V015).
 */
public record StructuralProgressJson(Map<MovementPattern, PatternProgressJson> byPattern) {
}
