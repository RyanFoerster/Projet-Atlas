package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représentation JPA de l'aggregate {@link dev.ryanfoerster.atlas.roster.domain.model.Roster}
 * (table {@code rosters}). Les athlètes sont gérés comme partie de l'aggregate via {@code @OneToMany}
 * (cascade ALL + orphanRemoval) — le Roster est persisté en entier (ADR-019).
 */
@Entity
@Table(name = "rosters")
public class RosterJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true, updatable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "roster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AthleteJpaEntity> athletes = new ArrayList<>();

    public RosterJpaEntity() {
    }

    /** Ajoute un athlète en maintenant la cohérence bidirectionnelle (FK roster_id). */
    public void addAthlete(AthleteJpaEntity athlete) {
        athlete.setRoster(this);
        this.athletes.add(athlete);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<AthleteJpaEntity> getAthletes() {
        return athletes;
    }

    public void setAthletes(List<AthleteJpaEntity> athletes) {
        this.athletes = athletes;
    }
}
