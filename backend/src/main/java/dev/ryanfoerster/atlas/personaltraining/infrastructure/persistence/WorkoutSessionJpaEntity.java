package dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence;

import dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.json.ExercisesJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA d'une séance (table {@code workout_sessions}). Aggregate autonome : entité racine
 * avec son propre repository (contrairement à {@code AthleteJpaEntity}, enfant d'un Roster).
 *
 * <p>Les exercices (structure imbriquée exercice → séries, avec catégorie sealed aplatie) sont stockés
 * en <strong>jsonb</strong> via le support JSON natif d'Hibernate 7 ({@link JdbcTypeCode}). La
 * conversion domaine ↔ DTO JSON vit dans le mapper (le domaine ne voit jamais Jackson).
 */
@Entity
@Table(name = "workout_sessions")
public class WorkoutSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "notes", length = 500)
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exercises", nullable = false, columnDefinition = "jsonb")
    private ExercisesJson exercises;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WorkoutSessionJpaEntity() {
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

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ExercisesJson getExercises() {
        return exercises;
    }

    public void setExercises(ExercisesJson exercises) {
        this.exercises = exercises;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
