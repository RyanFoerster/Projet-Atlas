package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.MirrorAlreadyExistsException;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Écurie d'un Player — <strong>seul aggregate root du module roster</strong> (ADR-019). Les
 * {@link Athlete} sont des entities internes : on ne les manipule qu'à travers le Roster (création,
 * navigation). Égalité <b>par identité</b> ({@link RosterId}). Immutable : chaque mutation retourne
 * un nouveau Roster (la liste d'athlètes est recopiée défensivement — décision #4).
 *
 * <p><strong>Invariant clé</strong> : au plus <b>un</b> athlète miroir ({@code isMirror == true}).
 */
public final class Roster {

    private final RosterId id;
    private final UserId ownerId;
    private final List<Athlete> athletes;
    private final Instant createdAt;

    private Roster(RosterId id, UserId ownerId, List<Athlete> athletes, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(athletes, "athletes");
        if (athletes.stream().filter(Athlete::isMirror).count() > 1) {
            throw new MirrorAlreadyExistsException("Un roster ne peut pas avoir plus d'un athlète miroir");
        }
        this.athletes = List.copyOf(athletes); // immutabilité réelle
    }

    /** Crée un roster vide pour un Player. */
    public static Roster createFor(UserId ownerId, Instant now) {
        return new Roster(RosterId.generate(), ownerId, List.of(), now);
    }

    /**
     * Ajoute l'athlète miroir (orchestration de sa création ici, ADR-019). La génétique miroir est
     * générée par {@code generator} à partir des 1RM saisis (génération hybride, ADR-021) ; le
     * {@code seed} rend le tirage reproductible (testabilité — injecté par le use case).
     *
     * @throws MirrorAlreadyExistsException si un miroir existe déjà (→ 409)
     */
    public Roster addMirror(MirrorCreationRequest request, AthleteGenerator generator, long seed, Instant now) {
        Objects.requireNonNull(request, "request");
        if (hasMirror()) {
            throw new MirrorAlreadyExistsException("Ce roster a déjà un athlète miroir");
        }
        Genetics genetics = generator.generateGeneticsForMirror(
                request.oneRepMaxes(), request.bodyWeight(), request.gender(), seed);
        CurrentStats stats = new CurrentStats(request.oneRepMaxes());
        Athlete mirror = Athlete.createMirror(id, request.name(), request.age(), request.bodyWeight(),
                request.bodyHeight(), request.gender(), genetics, stats, now);
        return withAthlete(mirror);
    }

    /** Recrute un athlète virtuel à partir d'un candidat scouté accepté. */
    public Roster recruit(AthleteCandidate candidate, Instant now) {
        return withAthlete(Athlete.recruit(id, candidate, now));
    }

    private Roster withAthlete(Athlete athlete) {
        List<Athlete> next = new ArrayList<>(athletes);
        next.add(athlete);
        return new Roster(id, ownerId, next, createdAt);
    }

    /**
     * Applique une séance IRL à l'athlète <strong>miroir</strong> (consommation de l'event
     * {@code WorkoutLogged}, ADR-025). Idempotent par écrasement monotone (la logique vit dans
     * {@link TrainingHistory#recordWorkout}). S'il n'y a pas de miroir, c'est un no-op. Retourne une
     * nouvelle instance (immutabilité), les athlètes virtuels sont inchangés.
     */
    public Roster recordMirrorWorkout(Instant performedAt, Set<MovementPattern> patternsCovered) {
        List<Athlete> next = athletes.stream()
                .map(a -> a.isMirror() ? a.withWorkout(performedAt, patternsCovered) : a)
                .toList();
        return new Roster(id, ownerId, next, createdAt);
    }

    public Optional<Athlete> mirrorAthlete() {
        return athletes.stream().filter(Athlete::isMirror).findFirst();
    }

    public List<Athlete> virtualAthletes() {
        return athletes.stream().filter(a -> !a.isMirror()).toList();
    }

    public Optional<Athlete> findAthlete(AthleteId athleteId) {
        return athletes.stream().filter(a -> a.id().equals(athleteId)).findFirst();
    }

    public boolean hasMirror() {
        return mirrorAthlete().isPresent();
    }

    public int size() {
        return athletes.size();
    }

    /** Liste immutable des athlètes (mapper/queries). */
    public List<Athlete> athletes() {
        return athletes;
    }

    public RosterId id() {
        return id;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** <strong>FOR PERSISTENCE LAYER ONLY.</strong> Réhydrate un roster et ses athlètes (ADR-015). */
    public static Roster reconstitute(RosterId id, UserId ownerId, List<Athlete> athletes, Instant createdAt) {
        return new Roster(id, ownerId, athletes, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Roster other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Roster[id=" + id + ", owner=" + ownerId + ", athletes=" + athletes.size() + "]";
    }
}
