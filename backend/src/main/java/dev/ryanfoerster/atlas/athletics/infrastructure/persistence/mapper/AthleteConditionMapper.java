package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.AthleteConditionJpaEntity;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.springframework.stereotype.Component;

/**
 * Mapper <strong>manuel</strong> domaine ↔ JPA (ADR-015) : un aggregate riche (constructeurs privés,
 * réhydratation via {@code reconstitute}) est l'anti-bean de MapStruct. Réhydratation par le même
 * constructeur que la création → invariants garantis.
 */
@Component
public class AthleteConditionMapper {

    public AthleteConditionJpaEntity toEntity(AthleteCondition condition) {
        FitnessFatigueState state = condition.state();
        AthleteConditionJpaEntity entity = new AthleteConditionJpaEntity();
        entity.setAthleteId(condition.athleteId().value());
        entity.setFitness(state.fitness());
        entity.setFatigue(state.fatigue());
        entity.setLastUpdated(state.lastUpdated());
        return entity;
    }

    public AthleteCondition toDomain(AthleteConditionJpaEntity entity) {
        return AthleteCondition.reconstitute(
                new AthleteId(entity.getAthleteId()),
                new FitnessFatigueState(entity.getFitness(), entity.getFatigue(), entity.getLastUpdated()));
    }
}
