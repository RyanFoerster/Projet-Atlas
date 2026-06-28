package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.util.Map;

/**
 * Forme sérialisable (JSONB) de la forme par muscle d'un athlète : la {@code Map<MuscleGroup,
 * MuscleCondition>} de {@code FitnessFatigueState}. Wrapper record (colonne {@code by_muscle}) — même
 * convention que {@code GeneticsJson} / {@code ExercisesJson} : le type de colonne est un record, pas une
 * {@code Map} nue. Les clés enum sont sérialisées par Jackson sous leur nom.
 */
public record MuscleConditionsJson(Map<MuscleGroup, MuscleConditionJson> byMuscle) {
}
