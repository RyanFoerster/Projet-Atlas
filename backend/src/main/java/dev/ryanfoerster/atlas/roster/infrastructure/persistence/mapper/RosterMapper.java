package dev.ryanfoerster.atlas.roster.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.model.RosterId;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.RosterJpaEntity;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper manuel de l'aggregate Roster (ADR-015). <strong>Triple rôle</strong> : domaine ↔ entité JPA,
 * et domaine ↔ DTO JSON (via {@link AthleteMapper} et {@code RosterJsonConverter}) pour les colonnes
 * jsonb. Le domaine reste pur — toute la sérialisation vit ici, en infrastructure.
 */
@Component
public class RosterMapper {

    public RosterJpaEntity toEntity(Roster roster) {
        RosterJpaEntity entity = new RosterJpaEntity();
        entity.setId(roster.id().value());
        entity.setOwnerId(roster.ownerId().value());
        entity.setCreatedAt(roster.createdAt());
        for (Athlete athlete : roster.athletes()) {
            entity.addAthlete(AthleteMapper.toEntity(athlete)); // maintient la cohérence bidirectionnelle
        }
        return entity;
    }

    public Roster toDomain(RosterJpaEntity entity) {
        RosterId rosterId = new RosterId(entity.getId());
        List<Athlete> athletes = entity.getAthletes().stream()
                .map(jpaAthlete -> AthleteMapper.toDomain(jpaAthlete, rosterId))
                .toList();
        return Roster.reconstitute(rosterId, new UserId(entity.getOwnerId()), athletes, entity.getCreatedAt());
    }
}
