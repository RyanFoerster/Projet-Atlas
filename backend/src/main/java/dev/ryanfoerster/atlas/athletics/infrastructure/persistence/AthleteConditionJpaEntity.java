package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA de l'état dynamique d'un athlète (table {@code athlete_conditions}). Aggregate
 * autonome clé par {@code athlete_id}. État plat (2 doubles + 1 timestamp) — pas de JSONB nécessaire,
 * contrairement aux structures imbriquées de Roster/PersonalTraining.
 */
@Entity
@Table(name = "athlete_conditions")
public class AthleteConditionJpaEntity {

    @Id
    @Column(name = "athlete_id", nullable = false, updatable = false)
    private UUID athleteId;

    @Column(name = "fitness", nullable = false)
    private double fitness;

    @Column(name = "fatigue", nullable = false)
    private double fatigue;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    public AthleteConditionJpaEntity() {
    }

    public UUID getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(UUID athleteId) {
        this.athleteId = athleteId;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getFatigue() {
        return fatigue;
    }

    public void setFatigue(double fatigue) {
        this.fatigue = fatigue;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
