package dev.ryanfoerster.atlas.personaltraining.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Identifiant d'une {@link WorkoutSession}, UUID v7 (RFC 9562). Même pattern que les autres
 * identifiants d'aggregate du projet (ADR-014) : record auto-validant, {@code generate()} /
 * {@code from(String)}.
 */
public record WorkoutSessionId(UUID value) {

    public WorkoutSessionId {
        if (value == null) {
            throw new IllegalArgumentException("WorkoutSessionId ne peut pas encapsuler un UUID null");
        }
    }

    public static WorkoutSessionId generate() {
        return new WorkoutSessionId(UuidCreator.getTimeOrderedEpoch());
    }

    public static WorkoutSessionId from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("WorkoutSessionId ne peut pas être construit depuis une chaîne null");
        }
        try {
            return new WorkoutSessionId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("WorkoutSessionId invalide : « " + raw + " » n'est pas un UUID", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
