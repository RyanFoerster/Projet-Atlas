package dev.ryanfoerster.atlas.personaltraining.application.command;

import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;

import java.time.Instant;
import java.util.List;

/**
 * Données d'entrée du log d'une séance, déjà traduites en objets du domaine par la couche web (les VO
 * valident à la construction). Le use case reste pure orchestration : il ne reconstruit pas de domaine.
 */
public record LogWorkoutCommand(
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        List<LoggedExercise> exercises) {
}
