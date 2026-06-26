package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshotId;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adapter secondaire : implémente {@link ConditionSnapshotRepository} via Spring Data. Le snapshot est un
 * value object plat → mapping inline (pas de mapper dédié, contrairement à l'aggregate {@code AthleteCondition}).
 */
@Component
public class ConditionSnapshotPersistenceAdapter implements ConditionSnapshotRepository {

    private final ConditionSnapshotJpaRepository jpaRepository;

    public ConditionSnapshotPersistenceAdapter(ConditionSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public ConditionSnapshot save(ConditionSnapshot snapshot) {
        jpaRepository.save(toEntity(snapshot));
        return snapshot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConditionSnapshot> findByAthleteId(AthleteId athleteId) {
        return jpaRepository.findByAthleteIdOrderByTakenAtAsc(athleteId.value())
                .stream().map(ConditionSnapshotPersistenceAdapter::toDomain).toList();
    }

    private static ConditionSnapshotJpaEntity toEntity(ConditionSnapshot snapshot) {
        ConditionSnapshotJpaEntity entity = new ConditionSnapshotJpaEntity();
        entity.setId(snapshot.id().value());
        entity.setAthleteId(snapshot.athleteId().value());
        entity.setTakenAt(snapshot.takenAt());
        entity.setFitness(snapshot.fitness());
        entity.setFatigue(snapshot.fatigue());
        entity.setPerformance(snapshot.performance());
        return entity;
    }

    private static ConditionSnapshot toDomain(ConditionSnapshotJpaEntity entity) {
        return new ConditionSnapshot(
                new ConditionSnapshotId(entity.getId()),
                new AthleteId(entity.getAthleteId()),
                entity.getTakenAt(),
                entity.getFitness(),
                entity.getFatigue(),
                entity.getPerformance());
    }
}
