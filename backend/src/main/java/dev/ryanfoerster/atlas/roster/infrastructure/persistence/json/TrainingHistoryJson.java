package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.List;

/**
 * Forme sérialisable (JSONB) de {@code TrainingHistory} (ADR-025). {@code lastWorkoutAt} nullable (aucune
 * séance reçue), patterns en liste (sérialisés par Jackson sous leur nom d'enum). Pas de compteur ici :
 * le nombre de séances vit dans PersonalTraining (option D, ADR-025).
 */
public record TrainingHistoryJson(Instant lastWorkoutAt, List<MovementPattern> lastPatternsCovered) {
}
