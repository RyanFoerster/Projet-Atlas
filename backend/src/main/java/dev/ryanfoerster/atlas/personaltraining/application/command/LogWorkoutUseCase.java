package dev.ryanfoerster.atlas.personaltraining.application.command;

import dev.ryanfoerster.atlas.personaltraining.application.mapper.WorkoutSessionToEventMapper;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.port.WorkoutSessionRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Use case : logger une séance d'entraînement IRL. Pure orchestration — toute la logique métier
 * (invariants de séance, validité des exercices) vit dans le domaine ({@code WorkoutSession.log} et les VO).
 *
 * <p><strong>Publication de l'event dans la transaction</strong> : {@code publishEvent} est appelé
 * <em>à l'intérieur</em> de la méthode {@code @Transactional}. C'est essentiel — le consumer écoute en
 * {@code AFTER_COMMIT} via {@code @ApplicationModuleListener}, et l'event publication registry de Modulith
 * persiste la publication dans la <em>même</em> transaction que la séance. Publier hors transaction
 * ferait perdre cette garantie de durabilité (ADR-023).
 */
@Service
public class LogWorkoutUseCase {

    private final WorkoutSessionRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public LogWorkoutUseCase(WorkoutSessionRepository repository,
                             ApplicationEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public WorkoutSession logWorkout(UserId owner, LogWorkoutCommand command) {
        Instant now = clock.instant();
        WorkoutSession session = WorkoutSession.log(owner, command.performedAt(), command.exercises(),
                command.durationMinutes(), command.notes(), now);
        WorkoutSession saved = repository.save(session);
        eventPublisher.publishEvent(WorkoutSessionToEventMapper.toEvent(saved));
        return saved;
    }
}
