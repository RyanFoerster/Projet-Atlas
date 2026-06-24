package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json;

import java.util.List;

/**
 * Conteneur JSONB des exercices d'une séance. On <strong>enveloppe</strong> la liste dans un record
 * (plutôt que de mapper un {@code List} nu) pour deux raisons :
 * <ol>
 *   <li>résolution de type fiable côté Hibernate — un {@code List<ExerciseJson>} nu peut se
 *       désérialiser en {@code List<LinkedHashMap>} ; le record force le bon type d'élément ;</li>
 *   <li>cohérence avec le pattern Roster (jsonb de type {@code object}, pas {@code array}).</li>
 * </ol>
 */
public record ExercisesJson(List<ExerciseJson> exercises) {
}
