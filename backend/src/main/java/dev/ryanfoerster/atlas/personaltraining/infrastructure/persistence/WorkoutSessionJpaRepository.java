package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository Spring Data sur {@link WorkoutSessionJpaEntity}. Utilisé par l'adapter de persistance. */
public interface WorkoutSessionJpaRepository extends JpaRepository<WorkoutSessionJpaEntity, UUID> {

    /** Historique paginé d'un Player, le plus récent d'abord. */
    List<WorkoutSessionJpaEntity> findByOwnerIdOrderByPerformedAtDesc(UUID ownerId, Pageable pageable);

    long countByOwnerId(UUID ownerId);
}
