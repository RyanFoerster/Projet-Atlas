package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidAgeException;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Athlète de l'écurie. <strong>Entity interne</strong> à l'aggregate {@link Roster}, pas un aggregate
 * root (ADR-019) : il n'existe pas hors d'un Roster, n'a pas de repository propre, et n'a pas de
 * factory de création publique — c'est {@code Roster} qui orchestre sa création
 * ({@code roster.addMirror(...)}, {@code roster.recruit(...)}). Égalité <b>par identité</b>
 * ({@link AthleteId}), comme tout aggregate/entity (cf. mini-cours sprint 1).
 *
 * <p>Immutable : aucune mutation en place.
 */
public final class Athlete {

    private static final int MIN_AGE = 16;
    private static final int MAX_AGE = 50;

    private final AthleteId id;
    private final RosterId rosterId;
    private final AthleteName name;
    private final int age;
    private final Weight bodyWeight;
    private final Height bodyHeight;
    private final Gender gender;
    private final Genetics genetics;
    private final CurrentStats currentStats;
    private final Rarity rarity;
    private final boolean mirror;
    private final Instant recruitedAt;
    private final TrainingHistory trainingHistory;

    private Athlete(AthleteId id, RosterId rosterId, AthleteName name, int age, Weight bodyWeight,
                    Height bodyHeight, Gender gender, Genetics genetics, CurrentStats currentStats,
                    Rarity rarity, boolean mirror, Instant recruitedAt, TrainingHistory trainingHistory) {
        this.id = Objects.requireNonNull(id, "id");
        this.rosterId = Objects.requireNonNull(rosterId, "rosterId");
        this.name = Objects.requireNonNull(name, "name");
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new InvalidAgeException("L'âge doit être entre " + MIN_AGE + " et " + MAX_AGE + " : " + age);
        }
        this.age = age;
        this.bodyWeight = Objects.requireNonNull(bodyWeight, "bodyWeight");
        this.bodyHeight = Objects.requireNonNull(bodyHeight, "bodyHeight");
        this.gender = Objects.requireNonNull(gender, "gender");
        this.genetics = Objects.requireNonNull(genetics, "genetics");
        this.currentStats = Objects.requireNonNull(currentStats, "currentStats");
        this.rarity = Objects.requireNonNull(rarity, "rarity");
        this.mirror = mirror;
        this.recruitedAt = Objects.requireNonNull(recruitedAt, "recruitedAt");
        this.trainingHistory = Objects.requireNonNull(trainingHistory, "trainingHistory");
    }

    /**
     * Crée l'athlète miroir (orchestré par {@link Roster#addMirror}). Génétique et stats sont déjà
     * construites en amont (la génétique miroir est générée par {@code AthleteGenerator} à partir des
     * 1RM saisis). Rareté du miroir : {@link Rarity#GENERIC} par défaut — la rareté est un concept
     * d'athlète virtuel/scouté (à confirmer en revue : faut-il la dériver de la génétique du miroir ?).
     */
    static Athlete createMirror(RosterId rosterId, AthleteName name, int age, Weight bodyWeight,
                                Height bodyHeight, Gender gender, Genetics genetics,
                                CurrentStats currentStats, Instant now) {
        return new Athlete(AthleteId.generate(), rosterId, name, age, bodyWeight, bodyHeight, gender,
                genetics, currentStats, Rarity.GENERIC, true, now, TrainingHistory.empty());
    }

    /** Crée un athlète virtuel à partir d'un candidat scouté (orchestré par {@link Roster#recruit}). */
    static Athlete recruit(RosterId rosterId, AthleteCandidate candidate, Instant now) {
        Objects.requireNonNull(candidate, "candidate");
        return new Athlete(AthleteId.generate(), rosterId, candidate.name(), candidate.age(),
                candidate.bodyWeight(), candidate.bodyHeight(), candidate.gender(), candidate.genetics(),
                new CurrentStats(candidate.baseOneRepMaxes()), candidate.rarity(), false, now,
                TrainingHistory.empty());
    }

    /**
     * <strong>FOR PERSISTENCE LAYER ONLY.</strong> Réhydrate un athlète depuis un état stocké
     * (ADR-015). Passe par le même constructeur privé → invariants garantis.
     */
    public static Athlete reconstitute(AthleteId id, RosterId rosterId, AthleteName name, int age,
                                       Weight bodyWeight, Height bodyHeight, Gender gender, Genetics genetics,
                                       CurrentStats currentStats, Rarity rarity, boolean mirror, Instant recruitedAt,
                                       TrainingHistory trainingHistory) {
        return new Athlete(id, rosterId, name, age, bodyWeight, bodyHeight, gender, genetics, currentStats,
                rarity, mirror, recruitedAt, trainingHistory);
    }

    /**
     * Applique une séance d'entraînement reçue (event {@code WorkoutLogged}) à cet athlète : retourne une
     * nouvelle instance dont le {@link TrainingHistory} est mis à jour de façon idempotente (écrasement
     * monotone — cf. {@link TrainingHistory#recordWorkout}). Orchestré par {@link Roster#recordMirrorWorkout}.
     */
    Athlete withWorkout(Instant performedAt, Set<MovementPattern> patternsCovered) {
        return new Athlete(id, rosterId, name, age, bodyWeight, bodyHeight, gender, genetics, currentStats,
                rarity, mirror, recruitedAt, trainingHistory.recordWorkout(performedAt, patternsCovered));
    }

    /**
     * Matérialise une progression structurelle du 1RM (event {@code CurrentStatsProgressed}, Couche 3). Le
     * verbe « progress » dit l'invariant : on ne matérialise qu'une <strong>hausse</strong> (cliquet). Une
     * valeur inférieure ou égale au 1RM courant est un <strong>no-op</strong> (retourne {@code this}) — ce
     * qui rend l'application <em>idempotente</em> et sûre au rejeu / réordonnancement des events (livraison
     * at-least-once de Modulith, ADR-023). La distinction CurrentStats / Fitness est ainsi gardée jusqu'au
     * point de matérialisation : le 1RM ne recule jamais. Orchestré par {@link Roster#progressAthleteStat}.
     */
    Athlete progressOneRepMax(MovementPattern pattern, OneRepMax oneRepMax) {
        boolean notAnIncrease = currentStats.oneRepMax(pattern)
                .map(current -> oneRepMax.weight().toKilograms().compareTo(current.weight().toKilograms()) <= 0)
                .orElse(false);
        if (notAnIncrease) {
            return this;
        }
        return new Athlete(id, rosterId, name, age, bodyWeight, bodyHeight, gender, genetics,
                currentStats.with(pattern, oneRepMax), rarity, mirror, recruitedAt, trainingHistory);
    }

    public Optional<OneRepMax> currentOneRepMax(MovementPattern pattern) {
        return currentStats.oneRepMax(pattern);
    }

    public AthleteId id() {
        return id;
    }

    public RosterId rosterId() {
        return rosterId;
    }

    public AthleteName name() {
        return name;
    }

    public int age() {
        return age;
    }

    public Weight bodyWeight() {
        return bodyWeight;
    }

    public Height bodyHeight() {
        return bodyHeight;
    }

    public Gender gender() {
        return gender;
    }

    public Genetics genetics() {
        return genetics;
    }

    public CurrentStats currentStats() {
        return currentStats;
    }

    public Rarity rarity() {
        return rarity;
    }

    public boolean isMirror() {
        return mirror;
    }

    public Instant recruitedAt() {
        return recruitedAt;
    }

    public TrainingHistory trainingHistory() {
        return trainingHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Athlete other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Athlete[id=" + id + ", name=" + name + ", mirror=" + mirror + ", rarity=" + rarity + "]";
    }
}
