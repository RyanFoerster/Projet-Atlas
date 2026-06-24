package dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.List;

/**
 * Historique paginé (réponse de GET /api/personal-training/sessions). Chaque item est un <em>résumé</em>
 * (pas le détail complet des séries) — le détail s'obtient via GET /sessions/:id.
 */
public record WorkoutHistoryDto(List<Item> sessions, int page, int size, long total) {

    public static WorkoutHistoryDto of(List<WorkoutSession> sessions, int page, int size, long total) {
        return new WorkoutHistoryDto(sessions.stream().map(Item::from).toList(), page, size, total);
    }

    /** Résumé d'une séance dans la liste chronologique. */
    public record Item(
            String id,
            Instant performedAt,
            Integer durationMinutes,
            int exerciseCount,
            int totalSets,
            int totalReps,
            List<String> patternsCovered) {

        static Item from(WorkoutSession session) {
            return new Item(
                    session.id().toString(),
                    session.performedAt(),
                    session.durationMinutes().orElse(null),
                    session.exercises().size(),
                    session.totalSets(),
                    session.totalReps(),
                    session.patternsCovered().stream().map(MovementPattern::name).toList());
        }
    }
}
