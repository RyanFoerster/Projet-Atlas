package dev.ryanfoerster.atlas.roster.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Identifiant d'un {@link Roster}, UUID v7 (RFC 9562). Même pattern que les identifiants du module
 * identity (ADR-014) : record auto-validant, {@code generate()} / {@code from(String)}.
 */
public record RosterId(UUID value) {

    public RosterId {
        if (value == null) {
            throw new IllegalArgumentException("RosterId ne peut pas encapsuler un UUID null");
        }
    }

    public static RosterId generate() {
        return new RosterId(UuidCreator.getTimeOrderedEpoch());
    }

    public static RosterId from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("RosterId ne peut pas être construit depuis une chaîne null");
        }
        try {
            return new RosterId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("RosterId invalide : « " + raw + " » n'est pas un UUID", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
