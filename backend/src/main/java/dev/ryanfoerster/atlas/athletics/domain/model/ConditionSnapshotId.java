package dev.ryanfoerster.atlas.athletics.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/** Identifiant d'un {@link ConditionSnapshot}, UUID v7 (RFC 9562). Pattern d'identité du projet (ADR-014). */
public record ConditionSnapshotId(UUID value) {

    public ConditionSnapshotId {
        if (value == null) {
            throw new IllegalArgumentException("ConditionSnapshotId ne peut pas encapsuler un UUID null");
        }
    }

    public static ConditionSnapshotId generate() {
        return new ConditionSnapshotId(UuidCreator.getTimeOrderedEpoch());
    }
}
