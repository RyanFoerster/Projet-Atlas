package dev.ryanfoerster.atlas.roster.domain.port;

import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;

import java.time.Instant;
import java.util.Optional;

/**
 * Port secondaire : persistance temporaire des {@link ScoutedCandidate} (ADR-022). Implémenté en
 * infrastructure.
 */
public interface ScoutedCandidateRepository {

    ScoutedCandidate save(ScoutedCandidate candidate);

    Optional<ScoutedCandidate> findById(ScoutedCandidateId id);

    /**
     * Purge les candidats expirés avant {@code threshold} (job de nettoyage périodique).
     *
     * @return nombre de candidats supprimés
     */
    int deleteExpiredBefore(Instant threshold);
}
