package dev.ryanfoerster.atlas.roster.application.eventhandler;

import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consumer Modulith de l'event {@link WorkoutLogged} (publié par PersonalTraining). Met à jour le
 * {@code TrainingHistory} de l'athlète <strong>miroir</strong> du Player : sa dernière séance et les
 * patterns de force couverts.
 *
 * <p><strong>{@link ApplicationModuleListener}</strong> = {@code @TransactionalEventListener(AFTER_COMMIT)}
 * + {@code @Async} + {@code @Transactional(REQUIRES_NEW)}. Cohérence éventuelle assumée (ADR-023) : la
 * séance est déjà commitée côté PersonalTraining ; le miroir est mis à jour juste après, de façon
 * asynchrone, dans sa propre transaction. La livraison est durable et au-moins-une-fois via l'event
 * publication registry.
 *
 * <p><strong>Idempotence</strong> : aucun compteur n'est incrémenté ici (le nombre de séances vit dans
 * PersonalTraining, option D — ADR-025). La seule mutation est l'écrasement <em>monotone</em> de la
 * dernière séance ({@code Roster.recordMirrorWorkout} → {@code TrainingHistory.recordWorkout}). Rejouer
 * cet event (au restart, ou après échec) est donc un no-op : pas de double comptage, pas de régression.
 *
 * <p><strong>Isolation Modulith</strong> : ce handler n'importe que {@code personaltraining.api.events.*}
 * (l'event + ses snapshots), jamais le domaine interne de PersonalTraining.
 */
@Component
public class WorkoutLoggedHandler {

    private final RosterRepository rosterRepository;

    public WorkoutLoggedHandler(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @ApplicationModuleListener
    public void on(WorkoutLogged event) {
        UserId owner = new UserId(event.ownerId());
        rosterRepository.findByOwnerId(owner).ifPresent(roster -> {
            Roster updated = roster.recordMirrorWorkout(event.performedAt(), compoundPatterns(event));
            rosterRepository.save(updated);
        });
    }

    /**
     * Patterns de force couverts par la séance — <strong>uniquement</strong> les exercices composés du
     * snapshot ({@code categoryType == COMPOUND_FORCE}). Un accessoire ne couvre pas un pattern de force
     * (cohérent avec {@code WorkoutSession.patternsCovered()} et ADR-026).
     */
    private static Set<MovementPattern> compoundPatterns(WorkoutLogged event) {
        return event.exercises().stream()
                .filter(snapshot -> LoggedExerciseSnapshot.COMPOUND_FORCE.equals(snapshot.categoryType()))
                .map(LoggedExerciseSnapshot::pattern)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
