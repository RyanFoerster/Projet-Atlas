package dev.ryanfoerster.atlas.personaltraining.application.query;

import dev.ryanfoerster.atlas.personaltraining.api.PersonalTrainingQueryPort;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Service;

/**
 * Implémente le port public {@link PersonalTrainingQueryPort}. Adapter applicatif vers le repository du
 * domaine — orchestration pure, pas de logique métier.
 */
@Service
public class PersonalTrainingQueryService implements PersonalTrainingQueryPort {

    private final WorkoutSessionRepository repository;

    public PersonalTrainingQueryService(WorkoutSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countSessionsFor(UserId owner) {
        return repository.countByOwner(owner);
    }
}
