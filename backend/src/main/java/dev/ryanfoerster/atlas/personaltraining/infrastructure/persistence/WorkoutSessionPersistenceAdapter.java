package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSessionId;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.mapper.WorkoutSessionMapper;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter secondaire : implémente {@link WorkoutSessionRepository} via Spring Data + {@link
 * WorkoutSessionMapper}.
 *
 * <p>Pas de collection LAZY ici (les exercices sont un attribut jsonb chargé avec la ligne), donc pas
 * de risque de {@code LazyInitializationException} comme côté Roster. Les transactions {@code readOnly}
 * restent pour la cohérence et la clarté d'intention.
 */
@Component
public class WorkoutSessionPersistenceAdapter implements WorkoutSessionRepository {

    private final WorkoutSessionJpaRepository jpaRepository;
    private final WorkoutSessionMapper mapper;

    public WorkoutSessionPersistenceAdapter(WorkoutSessionJpaRepository jpaRepository,
                                            WorkoutSessionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public WorkoutSession save(WorkoutSession session) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(session)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkoutSession> findById(WorkoutSessionId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutSession> findByOwner(UserId ownerId, int page, int size) {
        return jpaRepository.findByOwnerIdOrderByPerformedAtDesc(ownerId.value(), PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOwner(UserId ownerId) {
        return jpaRepository.countByOwnerId(ownerId.value());
    }
}
