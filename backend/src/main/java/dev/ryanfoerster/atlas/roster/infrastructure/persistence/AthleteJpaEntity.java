package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.CurrentStatsJson;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.GeneticsJson;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.TrainingHistoryJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA d'un athlète (table {@code athletes}). Entity ENFANT, gérée via la relation
 * {@code @OneToMany} depuis {@link RosterJpaEntity} (pas de repository propre — ADR-019).
 *
 * <p>{@code genetics}/{@code currentStats} sont stockés en <strong>jsonb</strong> via le support JSON
 * natif d'Hibernate 7 ({@link JdbcTypeCode}({@link SqlTypes#JSON})) — aucune dépendance externe. La
 * conversion domaine ↔ DTO JSON se fait dans le mapper (le domaine ne voit jamais Jackson).
 */
@Entity
@Table(name = "athletes")
public class AthleteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roster_id", nullable = false)
    private RosterJpaEntity roster;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "body_weight_kg", nullable = false)
    private BigDecimal bodyWeightKg;

    @Column(name = "body_height_cm", nullable = false)
    private int bodyHeightCm;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "rarity", nullable = false, length = 20)
    private String rarity;

    @Column(name = "is_mirror", nullable = false)
    private boolean mirror;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "genetics", nullable = false, columnDefinition = "jsonb")
    private GeneticsJson genetics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_stats", nullable = false, columnDefinition = "jsonb")
    private CurrentStatsJson currentStats;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "training_history", nullable = false, columnDefinition = "jsonb")
    private TrainingHistoryJson trainingHistory;

    @Column(name = "recruited_at", nullable = false, updatable = false)
    private Instant recruitedAt;

    public AthleteJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RosterJpaEntity getRoster() {
        return roster;
    }

    public void setRoster(RosterJpaEntity roster) {
        this.roster = roster;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public BigDecimal getBodyWeightKg() {
        return bodyWeightKg;
    }

    public void setBodyWeightKg(BigDecimal bodyWeightKg) {
        this.bodyWeightKg = bodyWeightKg;
    }

    public int getBodyHeightCm() {
        return bodyHeightCm;
    }

    public void setBodyHeightCm(int bodyHeightCm) {
        this.bodyHeightCm = bodyHeightCm;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public GeneticsJson getGenetics() {
        return genetics;
    }

    public void setGenetics(GeneticsJson genetics) {
        this.genetics = genetics;
    }

    public CurrentStatsJson getCurrentStats() {
        return currentStats;
    }

    public void setCurrentStats(CurrentStatsJson currentStats) {
        this.currentStats = currentStats;
    }

    public TrainingHistoryJson getTrainingHistory() {
        return trainingHistory;
    }

    public void setTrainingHistory(TrainingHistoryJson trainingHistory) {
        this.trainingHistory = trainingHistory;
    }

    public Instant getRecruitedAt() {
        return recruitedAt;
    }

    public void setRecruitedAt(Instant recruitedAt) {
        this.recruitedAt = recruitedAt;
    }
}
