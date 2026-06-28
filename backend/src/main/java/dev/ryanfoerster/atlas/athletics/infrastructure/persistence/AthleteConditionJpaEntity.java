package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json.MuscleConditionsJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA de l'état dynamique d'un athlète (table {@code athlete_conditions}). Aggregate
 * autonome clé par {@code athlete_id}. <strong>Sprint 5</strong> : la forme par muscle
 * ({@code Map<MuscleGroup, MuscleCondition>}) est stockée en <strong>jsonb</strong> via le support JSON
 * natif d'Hibernate 7 ({@link JdbcTypeCode}({@link SqlTypes#JSON})), comme Roster/PersonalTraining. La
 * conversion domaine ↔ DTO JSON se fait dans le mapper (le domaine ne voit jamais Jackson).
 */
@Entity
@Table(name = "athlete_conditions")
public class AthleteConditionJpaEntity {

    @Id
    @Column(name = "athlete_id", nullable = false, updatable = false)
    private UUID athleteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "by_muscle", nullable = false, columnDefinition = "jsonb")
    private MuscleConditionsJson byMuscle;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    // Modificateurs génétiques dénormalisés (sprint 5, Couche 3) — Genetics immutable, résolus une fois.
    @Column(name = "recovery_rate", nullable = false)
    private double recoveryRate;

    @Column(name = "stimulus_multiplier", nullable = false)
    private double stimulusMultiplier;

    public AthleteConditionJpaEntity() {
    }

    public UUID getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(UUID athleteId) {
        this.athleteId = athleteId;
    }

    public MuscleConditionsJson getByMuscle() {
        return byMuscle;
    }

    public void setByMuscle(MuscleConditionsJson byMuscle) {
        this.byMuscle = byMuscle;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(double recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public double getStimulusMultiplier() {
        return stimulusMultiplier;
    }

    public void setStimulusMultiplier(double stimulusMultiplier) {
        this.stimulusMultiplier = stimulusMultiplier;
    }
}
