package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;

import java.time.Instant;
import java.util.Objects;

/**
 * <strong>Aggregate root du module Athletics</strong> : l'état d'adaptation dynamique d'un athlète
 * (modèle de Banister), clé par {@link AthleteId} (ADR-027, option 3a). Distinct de l'identité statique
 * (nom, génétique, rareté) qui vit dans Roster : Athletics possède l'<em>état dynamique</em>, Roster
 * l'<em>identité</em> ; l'athlète « complet » est une composition à l'affichage.
 *
 * <p>Immutable : chaque évolution retourne une nouvelle instance (style fonctionnel, comme les autres
 * aggregates du projet). Égalité <strong>par identité</strong> ({@link AthleteId}).
 *
 * <p><strong>Sprint 4 : une seule paire Fitness/Fatigue globale</strong> (ADR-004). Le raffinement par
 * {@code MuscleGroup} est le sprint 5 — il enrichira {@link FitnessFatigueState} sans changer la frontière
 * de cet aggregate.
 */
public final class AthleteCondition {

    private final AthleteId athleteId;
    private final FitnessFatigueState state;

    private AthleteCondition(AthleteId athleteId, FitnessFatigueState state) {
        this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Condition initiale d'un athlète (fitness/fatigue à zéro), datée de {@code at}. */
    public static AthleteCondition initial(AthleteId athleteId, Instant at) {
        return new AthleteCondition(athleteId, FitnessFatigueState.initial(at));
    }

    /** <strong>FOR PERSISTENCE LAYER ONLY</strong> (ADR-015) : réhydrate depuis un état stocké. */
    public static AthleteCondition reconstitute(AthleteId athleteId, FitnessFatigueState state) {
        return new AthleteCondition(athleteId, state);
    }

    /**
     * Applique un stimulus daté de {@code at} : décroît l'état jusqu'à {@code at} puis ajoute l'impulsion
     * (délégué au {@link BanisterModel} pur). Retourne une nouvelle instance.
     */
    public AthleteCondition applyStimulus(BanisterModel model, TrainingStimulus stimulus, Instant at) {
        return new AthleteCondition(athleteId, model.applyStimulus(state, stimulus, at));
    }

    /**
     * Projette l'état jusqu'à {@code at} par décroissance pure — <strong>lazy compute</strong> (ADR-006)
     * pour la lecture/affichage. Ne mute pas la condition, ne se persiste pas.
     */
    public FitnessFatigueState projectedTo(BanisterModel model, Instant at) {
        return model.decayedTo(state, at);
    }

    /**
     * Une séance datée de {@code performedAt} doit-elle être appliquée ? Vrai seulement si elle est
     * <strong>strictement postérieure</strong> au dernier état. Garantit l'idempotence (un rejeu de
     * l'event au même {@code performedAt} est un no-op) et ignore les séances arrivées dans le désordre
     * (antérieures à l'état courant) — limitation assumée au sprint 4.
     */
    public boolean acceptsStimulusAt(Instant performedAt) {
        return performedAt.isAfter(state.lastUpdated());
    }

    public AthleteId athleteId() {
        return athleteId;
    }

    public FitnessFatigueState state() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AthleteCondition other && athleteId.equals(other.athleteId);
    }

    @Override
    public int hashCode() {
        return athleteId.hashCode();
    }

    @Override
    public String toString() {
        return "AthleteCondition[athleteId=" + athleteId + ", state=" + state + "]";
    }
}
