package dev.ryanfoerster.atlas.roster.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.ScoutedCandidateJpaEntity;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.RosterJsonConverter;
import org.springframework.stereotype.Component;

/** Mapper manuel des candidats scoutés (ADR-015/ADR-022). Le candidat est (dé)sérialisé en jsonb. */
@Component
public class ScoutedCandidateMapper {

    public ScoutedCandidateJpaEntity toEntity(ScoutedCandidate scouted) {
        ScoutedCandidateJpaEntity entity = new ScoutedCandidateJpaEntity();
        entity.setId(scouted.id().value());
        entity.setCandidate(RosterJsonConverter.toJson(scouted.candidate()));
        entity.setCreatedAt(scouted.createdAt());
        entity.setExpiresAt(scouted.expiresAt());
        entity.setConsumedAt(scouted.consumedAt().orElse(null));
        return entity;
    }

    public ScoutedCandidate toDomain(ScoutedCandidateJpaEntity entity) {
        return ScoutedCandidate.reconstitute(
                new ScoutedCandidateId(entity.getId()),
                RosterJsonConverter.fromJson(entity.getCandidate()),
                entity.getCreatedAt(), entity.getExpiresAt(), entity.getConsumedAt());
    }
}
