package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.ScoutedCandidateNotUsableException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Un candidat scouté <strong>persisté temporairement</strong> (ADR-022, décision #2 du co-affinage).
 *
 * <p>Pourquoi : {@code /scout} renvoie un candidat ; {@code /recruit} ne doit pas faire confiance au
 * candidat ré-envoyé par le client (sinon on forge un Prodigy). On persiste donc le candidat à
 * l'émission (avec un id + un TTL), et {@code /recruit} le reconstitue depuis la base par son id.
 *
 * <p>Même calque mental que {@code MagicLink} d'identity : objet à durée de vie courte, consommé une
 * seule fois. Entity (identité = {@link ScoutedCandidateId}), immutable et fonctionnel.
 */
public final class ScoutedCandidate {

    private final ScoutedCandidateId id;
    private final AthleteCandidate candidate;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant consumedAt; // null tant que non recruté

    private ScoutedCandidate(ScoutedCandidateId id, AthleteCandidate candidate, Instant createdAt,
                             Instant expiresAt, Instant consumedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt doit être après createdAt");
        }
        this.consumedAt = consumedAt;
    }

    /** Émet un candidat scouté non consommé, valable jusqu'à {@code expiresAt}. */
    public static ScoutedCandidate issue(ScoutedCandidateId id, AthleteCandidate candidate,
                                         Instant createdAt, Instant expiresAt) {
        return new ScoutedCandidate(id, candidate, createdAt, expiresAt, null);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean canBeConsumed(Instant now) {
        return !isExpired(now) && !isConsumed();
    }

    /**
     * Consomme (recrute) le candidat : retourne une nouvelle instance marquée consommée.
     *
     * @throws ScoutedCandidateNotUsableException si expiré ou déjà recruté
     */
    public ScoutedCandidate consume(Instant now) {
        if (isConsumed()) {
            throw new ScoutedCandidateNotUsableException("Ce candidat a déjà été recruté");
        }
        if (isExpired(now)) {
            throw new ScoutedCandidateNotUsableException("Ce candidat n'est plus disponible (expiré)");
        }
        return new ScoutedCandidate(id, candidate, createdAt, expiresAt, now);
    }

    /** <strong>FOR PERSISTENCE LAYER ONLY.</strong> Réhydrate depuis un état stocké (ADR-015). */
    public static ScoutedCandidate reconstitute(ScoutedCandidateId id, AthleteCandidate candidate,
                                                Instant createdAt, Instant expiresAt, Instant consumedAt) {
        return new ScoutedCandidate(id, candidate, createdAt, expiresAt, consumedAt);
    }

    public ScoutedCandidateId id() {
        return id;
    }

    public AthleteCandidate candidate() {
        return candidate;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> consumedAt() {
        return Optional.ofNullable(consumedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ScoutedCandidate other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
