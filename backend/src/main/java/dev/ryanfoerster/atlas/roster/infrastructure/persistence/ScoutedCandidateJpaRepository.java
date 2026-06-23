package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface ScoutedCandidateJpaRepository extends JpaRepository<ScoutedCandidateJpaEntity, UUID> {

    /** Purge des candidats expirés (job périodique). Retourne le nombre de lignes supprimées. */
    @Modifying
    @Transactional
    long deleteByExpiresAtBefore(Instant threshold);
}
