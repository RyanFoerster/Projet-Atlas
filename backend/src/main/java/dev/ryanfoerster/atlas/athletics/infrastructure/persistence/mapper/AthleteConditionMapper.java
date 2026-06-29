package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.MuscleCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.PatternProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.StructuralProgress;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.AthleteConditionJpaEntity;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json.MuscleConditionJson;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json.MuscleConditionsJson;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json.PatternProgressJson;
import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json.StructuralProgressJson;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mapper <strong>manuel</strong> domaine ↔ JPA (ADR-015) : un aggregate riche (constructeurs privés,
 * réhydratation via {@code reconstitute}) est l'anti-bean de MapStruct. Réhydratation par le même
 * constructeur que la création → invariants garantis.
 *
 * <p>Sprint 5 : la forme par muscle ({@code Map<MuscleGroup, MuscleCondition>}) est convertie vers/depuis le
 * DTO jsonb {@link MuscleConditionsJson} (le volet « sérialisation » du mapping manuel, le domaine ne voit
 * jamais Jackson).
 */
@Component
public class AthleteConditionMapper {

    public AthleteConditionJpaEntity toEntity(AthleteCondition condition) {
        FitnessFatigueState state = condition.state();
        AthleteConditionJpaEntity entity = new AthleteConditionJpaEntity();
        entity.setAthleteId(condition.athleteId().value());
        entity.setByMuscle(toJson(state.byMuscle()));
        entity.setLastUpdated(state.lastUpdated());
        entity.setRecoveryRate(condition.geneticModifiers().recoveryRate());
        entity.setStimulusMultiplier(condition.geneticModifiers().stimulusMultiplier());
        entity.setStructuralProgress(toJson(condition.structural()));
        return entity;
    }

    public AthleteCondition toDomain(AthleteConditionJpaEntity entity) {
        return AthleteCondition.reconstitute(
                new AthleteId(entity.getAthleteId()),
                new FitnessFatigueState(fromJson(entity.getByMuscle()), entity.getLastUpdated()),
                new GeneticModifiers(entity.getRecoveryRate(), entity.getStimulusMultiplier()),
                fromJson(entity.getStructuralProgress()));
    }

    private static MuscleConditionsJson toJson(Map<MuscleGroup, MuscleCondition> byMuscle) {
        Map<MuscleGroup, MuscleConditionJson> json = new EnumMap<>(MuscleGroup.class);
        byMuscle.forEach((muscle, condition) ->
                json.put(muscle, new MuscleConditionJson(condition.fitness(), condition.fatigue())));
        return new MuscleConditionsJson(json);
    }

    private static Map<MuscleGroup, MuscleCondition> fromJson(MuscleConditionsJson json) {
        Map<MuscleGroup, MuscleCondition> byMuscle = new EnumMap<>(MuscleGroup.class);
        json.byMuscle().forEach((muscle, c) ->
                byMuscle.put(muscle, new MuscleCondition(c.fitness(), c.fatigue())));
        return byMuscle;
    }

    private static StructuralProgressJson toJson(StructuralProgress structural) {
        Map<MovementPattern, PatternProgressJson> json = new EnumMap<>(MovementPattern.class);
        structural.byPattern().forEach((pattern, p) ->
                json.put(pattern, new PatternProgressJson(p.startOneRmKg(), p.ceilingOneRmKg(), p.chronicLoad())));
        return new StructuralProgressJson(json);
    }

    private static StructuralProgress fromJson(StructuralProgressJson json) {
        Map<MovementPattern, PatternProgress> byPattern = new EnumMap<>(MovementPattern.class);
        json.byPattern().forEach((pattern, p) ->
                byPattern.put(pattern, new PatternProgress(p.startOneRmKg(), p.ceilingOneRmKg(), p.chronicLoad())));
        return new StructuralProgress(byPattern);
    }
}
