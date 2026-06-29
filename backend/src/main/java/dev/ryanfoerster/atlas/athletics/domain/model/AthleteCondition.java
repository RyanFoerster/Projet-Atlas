package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.StructuralProgressionModel;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * <strong>Aggregate root du module Athletics</strong> : l'état d'adaptation dynamique d'un athlète, clé par
 * {@link AthleteId} (ADR-027, option 3a). Distinct de l'identité statique (nom, génétique, rareté) qui vit
 * dans Roster : Athletics possède l'<em>état dynamique</em>, Roster l'<em>identité</em> ; l'athlète
 * « complet » est une composition à l'affichage.
 *
 * <p>Immutable : chaque évolution retourne une nouvelle instance (style fonctionnel). Égalité <strong>par
 * identité</strong> ({@link AthleteId}).
 *
 * <p><strong>Deux échelles de temps coexistent ici</strong> (sprint 6, Couche 3) :
 * <ul>
 *   <li>le {@link FitnessFatigueState} (Banister) — la <em>forme</em>, court terme (fitness semaines,
 *       fatigue jours), via {@link #applyStimulus} ;</li>
 *   <li>le {@link StructuralProgress} — l'accumulateur de <em>progression structurelle</em> du 1RM, long
 *       terme (mois/années), via {@link #progressStructure}. Store côté Athletics (l'autre, le 1RM
 *       matérialisé, vit dans Roster — ADR-032). La 3ᵉ échelle de temps.</li>
 * </ul>
 * Une séance fait évoluer les deux : la forme yo-yote, le 1RM ne fait que monter (cliquet, ADR-033).
 */
public final class AthleteCondition {

    private final AthleteId athleteId;
    private final FitnessFatigueState state;
    private final GeneticModifiers geneticModifiers;
    private final StructuralProgress structural;

    private AthleteCondition(AthleteId athleteId, FitnessFatigueState state, GeneticModifiers geneticModifiers,
                             StructuralProgress structural) {
        this.athleteId = Objects.requireNonNull(athleteId, "athleteId");
        this.state = Objects.requireNonNull(state, "state");
        this.geneticModifiers = Objects.requireNonNull(geneticModifiers, "geneticModifiers");
        this.structural = Objects.requireNonNull(structural, "structural");
    }

    /**
     * Condition initiale d'un athlète (fitness/fatigue à zéro, aucune progression structurelle entamée),
     * datée de {@code at}, avec ses modificateurs génétiques <strong>résolus une fois pour toutes</strong>
     * (la {@code Genetics} est immutable, ADR-031).
     */
    public static AthleteCondition initial(AthleteId athleteId, GeneticModifiers geneticModifiers, Instant at) {
        return new AthleteCondition(athleteId, FitnessFatigueState.initial(at), geneticModifiers,
                StructuralProgress.EMPTY);
    }

    /** <strong>FOR PERSISTENCE LAYER ONLY</strong> (ADR-015) : réhydrate depuis état + modifiers + accumulateur stockés. */
    public static AthleteCondition reconstitute(AthleteId athleteId, FitnessFatigueState state,
                                                GeneticModifiers geneticModifiers, StructuralProgress structural) {
        return new AthleteCondition(athleteId, state, geneticModifiers, structural);
    }

    /**
     * Applique un stimulus <strong>distribué par muscle</strong> daté de {@code at} : décroît l'état jusqu'à
     * {@code at} puis ajoute l'impulsion de chaque muscle, le tout <strong>modulé par la génétique</strong>
     * de l'athlète (délégué au {@link BanisterModel} pur). N'affecte que la <em>forme</em> ; la progression
     * structurelle est {@link #progressStructure} (échelle de temps distincte). Retourne une nouvelle instance.
     */
    public AthleteCondition applyStimulus(BanisterModel model, Map<MuscleGroup, TrainingStimulus> distributed,
                                          Instant at) {
        return new AthleteCondition(athleteId, model.applyStimulus(state, distributed, geneticModifiers, at),
                geneticModifiers, structural);
    }

    /**
     * Fait avancer la <strong>progression structurelle</strong> (la 3ᵉ échelle de temps, ADR-033) sur
     * {@code [from, at]} : décroissance de la charge chronique + accumulation du stimulus par pattern, vers le
     * plafond génétique. Les patterns vus pour la première fois sont initialisés depuis {@code references}
     * (1RM courant frais + plafond). Le 1RM <em>mérité</em> qui en résulte se lit via {@link #structural()} ;
     * l'émission de la progression (cliquet) est décidée par l'application, pas ici (le domaine ne publie pas
     * d'event).
     *
     * <p><strong>{@code from} explicite</strong> : c'est le {@code lastUpdated} <em>avant</em> la séance.
     * L'appelant le capture avant {@link #applyStimulus} (qui avance déjà {@code lastUpdated}), pour que la
     * décroissance chronique couvre le bon intervalle. Retourne une nouvelle instance.
     */
    public AthleteCondition progressStructure(StructuralProgressionModel model,
                                              Map<MovementPattern, TrainingStimulus> byPattern,
                                              Map<MovementPattern, PatternStrengthRef> references,
                                              Instant from, Instant at) {
        return new AthleteCondition(athleteId, state, geneticModifiers,
                model.advance(structural, byPattern, references, from, at));
    }

    /**
     * Projette l'état jusqu'à {@code at} par décroissance pure (modulée par la génétique) —
     * <strong>lazy compute</strong> (ADR-006) pour la lecture/affichage. Ne mute pas la condition.
     */
    public FitnessFatigueState projectedTo(BanisterModel model, Instant at) {
        return model.decayedTo(state, geneticModifiers, at);
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

    public GeneticModifiers geneticModifiers() {
        return geneticModifiers;
    }

    public StructuralProgress structural() {
        return structural;
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
