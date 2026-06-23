package dev.ryanfoerster.atlas.roster.infrastructure.scheduling;

import dev.ryanfoerster.atlas.roster.domain.port.ScoutedCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Purge périodique des candidats scoutés expirés (ADR-022, « ceinture » — la « bretelle » étant le
 * re-check à la consommation). La périodicité n'a pas besoin d'être synchrone avec le TTL.
 *
 * <p>Mono-instance assumé pour le MVP (ADR-022) : en multi-instance, prévoir ShedLock / élection de
 * leader pour éviter les purges concurrentes.
 */
@Component
class ScoutedCandidatePurgeJob {

    private static final Logger log = LoggerFactory.getLogger(ScoutedCandidatePurgeJob.class);

    private final ScoutedCandidateRepository repository;
    private final Clock clock;

    ScoutedCandidatePurgeJob(ScoutedCandidateRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedRate = 3_600_000L) // toutes les heures
    void purgeExpired() {
        int deleted = repository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Purge des candidats scoutés expirés : {} supprimé(s)", deleted);
        }
    }
}
