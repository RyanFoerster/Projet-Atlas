package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.mapper.AthleteConditionMapper;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adapter secondaire : implémente {@link AthleteConditionRepository} via Spring Data + le mapper manuel. */
@Component
public class AthleteConditionPersistenceAdapter implements AthleteConditionRepository {

    private final AthleteConditionJpaRepository jpaRepository;
    private final AthleteConditionMapper mapper;

    public AthleteConditionPersistenceAdapter(AthleteConditionJpaRepository jpaRepository,
                                              AthleteConditionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AthleteCondition save(AthleteCondition condition) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(condition)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AthleteCondition> findByAthleteId(AthleteId athleteId) {
        return jpaRepository.findById(athleteId.value()).map(mapper::toDomain);
    }
}
