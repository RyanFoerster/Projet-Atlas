package dev.ryanfoerster.atlas.personaltraining.domain.port;

import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSessionId;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port secondaire de persistance des {@link WorkoutSession} (hexagonal, ADR-003). Le domaine définit
 * l'interface ; l'adapter JPA l'implémente en infrastructure.
 *
 * <p>Chaque séance est un aggregate autonome : pas de navigation depuis un parent, on persiste et on
 * recharge des {@code WorkoutSession} directement (contrairement à Roster).
 */
public interface WorkoutSessionRepository {

    WorkoutSession save(WorkoutSession session);

    Optional<WorkoutSession> findById(WorkoutSessionId id);

    /** Historique d'un Player, le plus récent d'abord (tri {@code performedAt} DESC), paginé. */
    List<WorkoutSession> findByOwner(UserId ownerId, int page, int size);

    /** Nombre total de séances d'un Player (pour la pagination). */
    long countByOwner(UserId ownerId);
}
