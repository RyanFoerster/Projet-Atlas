package dev.ryanfoerster.atlas.personaltraining.application.query;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Query : l'historique paginé des séances d'un Player (le plus récent d'abord). Orchestration pure.
 */
@Service
public class GetWorkoutHistoryUseCase {

    private final WorkoutSessionRepository repository;

    public GetWorkoutHistoryUseCase(WorkoutSessionRepository repository) {
        this.repository = repository;
    }

    public WorkoutHistory historyFor(UserId owner, int page, int size) {
        List<WorkoutSession> sessions = repository.findByOwner(owner, page, size);
        long total = repository.countByOwner(owner);
        return new WorkoutHistory(sessions, total);
    }

    /** Résultat de la query : la page de séances + le total (pour la pagination côté web). */
    public record WorkoutHistory(List<WorkoutSession> sessions, long total) {
    }
}
