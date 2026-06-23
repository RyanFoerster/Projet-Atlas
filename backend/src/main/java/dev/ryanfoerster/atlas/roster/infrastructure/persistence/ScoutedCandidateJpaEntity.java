package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.AthleteCandidateJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA d'un candidat scouté temporaire (table {@code scouted_candidates}, ADR-022).
 * Le candidat complet est stocké en un seul blob {@code jsonb}.
 */
@Entity
@Table(name = "scouted_candidates")
public class ScoutedCandidateJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidate", nullable = false, columnDefinition = "jsonb")
    private AthleteCandidateJson candidate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public ScoutedCandidateJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AthleteCandidateJson getCandidate() {
        return candidate;
    }

    public void setCandidate(AthleteCandidateJson candidate) {
        this.candidate = candidate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }
}
