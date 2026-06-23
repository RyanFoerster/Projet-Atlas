package dev.ryanfoerster.atlas.roster.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/** Identifiant d'un {@link ScoutedCandidate}, UUID v7. */
public record ScoutedCandidateId(UUID value) {

    public ScoutedCandidateId {
        if (value == null) {
            throw new IllegalArgumentException("ScoutedCandidateId ne peut pas encapsuler un UUID null");
        }
    }

    public static ScoutedCandidateId generate() {
        return new ScoutedCandidateId(UuidCreator.getTimeOrderedEpoch());
    }

    public static ScoutedCandidateId from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("ScoutedCandidateId ne peut pas être construit depuis null");
        }
        try {
            return new ScoutedCandidateId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ScoutedCandidateId invalide : « " + raw + " »", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
