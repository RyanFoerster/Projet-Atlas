package dev.ryanfoerster.atlas.personaltraining.application.mapper;

import dev.ryanfoerster.atlas.personaltraining.api.events.ExerciseSetSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.Accessory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.CompoundForce;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.List;

/**
 * Aplatit une {@link WorkoutSession} du domaine vers l'event public {@link WorkoutLogged} (snapshot,
 * ADR-024). Le sealed {@code ExerciseCategory} est aplati en discriminant via un {@code switch}
 * exhaustif — c'est l'info qui permettra au consumer Roster de ne compter que les {@code CompoundForce}
 * dans {@code lastPatternsCovered} (cohérent avec {@code WorkoutSession.patternsCovered()}).
 *
 * <p>Classe statique pure : aucune dépendance Spring, directement testable.
 */
public final class WorkoutSessionToEventMapper {

    private WorkoutSessionToEventMapper() {
    }

    public static WorkoutLogged toEvent(WorkoutSession session) {
        List<LoggedExerciseSnapshot> exercises = session.exercises().stream()
                .map(WorkoutSessionToEventMapper::toSnapshot)
                .toList();
        return new WorkoutLogged(
                session.ownerId().value(),
                session.id().value(),
                session.performedAt(),
                session.durationMinutes().orElse(null),
                exercises);
    }

    private static LoggedExerciseSnapshot toSnapshot(LoggedExercise exercise) {
        String categoryType;
        MovementPattern pattern = null;
        String accessoryRegion = null;
        switch (exercise.category()) {
            case CompoundForce cf -> {
                categoryType = LoggedExerciseSnapshot.COMPOUND_FORCE;
                pattern = cf.pattern();
            }
            case Accessory a -> {
                categoryType = LoggedExerciseSnapshot.ACCESSORY;
                accessoryRegion = a.region().name();
            }
        }
        List<ExerciseSetSnapshot> sets = exercise.sets().stream()
                .map(WorkoutSessionToEventMapper::toSnapshot)
                .toList();
        return new LoggedExerciseSnapshot(exercise.name().value(), categoryType, pattern, accessoryRegion, sets);
    }

    private static ExerciseSetSnapshot toSnapshot(ExerciseSet set) {
        String loadType;
        Double weightKg = null;
        switch (set.load()) {
            case Load.Bodyweight ignored -> loadType = ExerciseSetSnapshot.BODYWEIGHT;
            case Load.Weighted w -> {
                loadType = ExerciseSetSnapshot.WEIGHTED;
                weightKg = w.added().toKilograms().doubleValue();
            }
            case Load.External e -> {
                loadType = ExerciseSetSnapshot.EXTERNAL;
                weightKg = e.weight().toKilograms().doubleValue();
            }
        }
        Double rpe = set.rpe() == null ? null : set.rpe().value();
        return new ExerciseSetSnapshot(set.reps(), loadType, weightKg, rpe);
    }
}
