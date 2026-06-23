package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repository Spring Data sur {@link RosterJpaEntity}. Utilisé par {@code RosterPersistenceAdapter}. */
public interface RosterJpaRepository extends JpaRepository<RosterJpaEntity, UUID> {

    Optional<RosterJpaEntity> findByOwnerId(UUID ownerId);
}
