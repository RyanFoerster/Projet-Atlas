package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository Spring Data des snapshots de condition (append-only). */
public interface ConditionSnapshotJpaRepository extends JpaRepository<ConditionSnapshotJpaEntity, UUID> {

    List<ConditionSnapshotJpaEntity> findByAthleteIdOrderByTakenAtAsc(UUID athleteId);
}
