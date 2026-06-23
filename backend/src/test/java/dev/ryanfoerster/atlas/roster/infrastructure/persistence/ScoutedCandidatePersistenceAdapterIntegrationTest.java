package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.domain.port.ScoutedCandidateRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ScoutedCandidatePersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00Z");
    private final AthleteGenerator generator = new ProceduralAthleteGenerator();

    @Autowired
    private ScoutedCandidateRepository repository;
    @Autowired
    private ScoutedCandidateJpaRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    private ScoutedCandidate issued(long seed, Instant createdAt, Instant expiresAt) {
        AthleteCandidate candidate = generator.generateCandidate(seed, Rarity.PRODIGY);
        return ScoutedCandidate.issue(ScoutedCandidateId.generate(), candidate, createdAt, expiresAt);
    }

    @Test
    void round_trips_the_candidate_blob_exactly() {
        ScoutedCandidate scouted = issued(5L, NOW, NOW.plus(Duration.ofHours(1)));
        repository.save(scouted);

        ScoutedCandidate reloaded = repository.findById(scouted.id()).orElseThrow();

        assertThat(reloaded.candidate()).isEqualTo(scouted.candidate()); // blob jsonb fidèle
        assertThat(reloaded.isConsumed()).isFalse();
        assertThat(reloaded.expiresAt()).isEqualTo(scouted.expiresAt());
    }

    @Test
    void persists_consumption_state() {
        ScoutedCandidate scouted = issued(5L, NOW, NOW.plus(Duration.ofHours(1)));
        repository.save(scouted);

        Instant when = NOW.plusSeconds(60);
        repository.save(scouted.consume(when));

        ScoutedCandidate reloaded = repository.findById(scouted.id()).orElseThrow();
        assertThat(reloaded.isConsumed()).isTrue();
        assertThat(reloaded.consumedAt()).contains(when);
    }

    @Test
    void purges_only_expired_candidates() {
        ScoutedCandidate expired = issued(1L, NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)));
        ScoutedCandidate fresh = issued(2L, NOW, NOW.plus(Duration.ofHours(1)));
        repository.save(expired);
        repository.save(fresh);

        int deleted = repository.deleteExpiredBefore(NOW);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById(expired.id())).isEmpty();
        assertThat(repository.findById(fresh.id())).isPresent();
    }
}
