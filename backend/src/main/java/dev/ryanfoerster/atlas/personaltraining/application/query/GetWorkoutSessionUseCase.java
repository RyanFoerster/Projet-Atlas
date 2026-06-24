package dev.ryanfoerster.atlas.personaltraining.application.query;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSessionId;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Query : le détail d'une séance, <strong>à condition qu'elle appartienne au Player</strong>. La séance
 * d'un autre utilisateur renvoie {@code Optional.empty()} → le controller en fait un 404 (sécurité
 * naturelle, comme pour les athlètes au sprint 2 : on ne révèle pas l'existence d'une ressource d'autrui).
 */
@Service
public class GetWorkoutSessionUseCase {

    private final WorkoutSessionRepository repository;

    public GetWorkoutSessionUseCase(WorkoutSessionRepository repository) {
        this.repository = repository;
    }

    public Optional<WorkoutSession> forOwner(UserId owner, WorkoutSessionId id) {
        return repository.findById(id).filter(session -> session.ownerId().equals(owner));
    }
}
