package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.domain.port.ScoutedCandidateRepository;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.mapper.ScoutedCandidateMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class ScoutedCandidatePersistenceAdapter implements ScoutedCandidateRepository {

    private final ScoutedCandidateJpaRepository jpaRepository;
    private final ScoutedCandidateMapper mapper;

    public ScoutedCandidatePersistenceAdapter(ScoutedCandidateJpaRepository jpaRepository,
                                              ScoutedCandidateMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ScoutedCandidate save(ScoutedCandidate candidate) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(candidate)));
    }

    @Override
    public Optional<ScoutedCandidate> findById(ScoutedCandidateId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public int deleteExpiredBefore(Instant threshold) {
        return (int) jpaRepository.deleteByExpiresAtBefore(threshold);
    }
}
