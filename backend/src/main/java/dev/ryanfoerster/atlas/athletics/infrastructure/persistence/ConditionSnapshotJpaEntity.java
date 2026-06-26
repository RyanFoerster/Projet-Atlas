package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA d'un point daté de la trajectoire Fitness/Fatigue (table {@code condition_snapshots}).
 * Append-only — alimente les courbes du sprint 7. {@code performance} peut être négative (athlète « cuit »).
 */
@Entity
@Table(name = "condition_snapshots")
public class ConditionSnapshotJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "athlete_id", nullable = false, updatable = false)
    private UUID athleteId;

    @Column(name = "taken_at", nullable = false, updatable = false)
    private Instant takenAt;

    @Column(name = "fitness", nullable = false, updatable = false)
    private double fitness;

    @Column(name = "fatigue", nullable = false, updatable = false)
    private double fatigue;

    @Column(name = "performance", nullable = false, updatable = false)
    private double performance;

    public ConditionSnapshotJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(UUID athleteId) {
        this.athleteId = athleteId;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Instant takenAt) {
        this.takenAt = takenAt;
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

    public double getPerformance() {
        return performance;
    }

    public void setPerformance(double performance) {
        this.performance = performance;
    }
}
