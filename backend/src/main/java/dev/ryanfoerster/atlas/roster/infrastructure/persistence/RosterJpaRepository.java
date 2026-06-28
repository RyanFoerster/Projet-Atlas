package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Repository Spring Data sur {@link RosterJpaEntity}. Utilisé par {@code RosterPersistenceAdapter}. */
public interface RosterJpaRepository extends JpaRepository<RosterJpaEntity, UUID> {

    Optional<RosterJpaEntity> findByOwnerId(UUID ownerId);

    /** Le roster qui contient l'athlète donné (jointure sur la collection d'athlètes). */
    @Query("select r from RosterJpaEntity r join r.athletes a where a.id = :athleteId")
    Optional<RosterJpaEntity> findByAthleteId(@Param("athleteId") UUID athleteId);
}
