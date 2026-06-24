package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSessionId;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.WorkoutSessionJpaEntity;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json.WorkoutSessionJsonConverter;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel de l'aggregate {@link WorkoutSession} (ADR-015). Double rôle : domaine ↔ entité JPA, et
 * domaine ↔ DTO JSON (via {@link WorkoutSessionJsonConverter}) pour la colonne jsonb {@code exercises}.
 * Le domaine reste pur — toute la sérialisation vit ici.
 */
@Component
public class WorkoutSessionMapper {

    public WorkoutSessionJpaEntity toEntity(WorkoutSession session) {
        WorkoutSessionJpaEntity entity = new WorkoutSessionJpaEntity();
        entity.setId(session.id().value());
        entity.setOwnerId(session.ownerId().value());
        entity.setPerformedAt(session.performedAt());
        entity.setDurationMinutes(session.durationMinutes().orElse(null));
        entity.setNotes(session.notes().orElse(null));
        entity.setExercises(WorkoutSessionJsonConverter.toJson(session.exercises()));
        entity.setCreatedAt(session.createdAt());
        return entity;
    }

    public WorkoutSession toDomain(WorkoutSessionJpaEntity entity) {
        return WorkoutSession.reconstitute(
                new WorkoutSessionId(entity.getId()),
                new UserId(entity.getOwnerId()),
                entity.getPerformedAt(),
                entity.getDurationMinutes(),
                WorkoutSessionJsonConverter.fromJson(entity.getExercises()),
                entity.getNotes(),
                entity.getCreatedAt());
    }
}
