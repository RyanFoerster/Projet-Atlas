package dev.ryanfoerster.atlas.shared.domain;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Identifiant d'un athlète, UUID v7 (RFC 9562). Même pattern que les autres identifiants
 * d'aggregate/entity du projet (ADR-014).
 *
 * <p><strong>Kernel partagé</strong> (ADR-017) : promu depuis {@code roster.domain.model} au sprint 4
 * lorsqu'un 2<sup>e</sup> module (Athletics) en a eu besoin — Athletics clé son aggregate
 * {@code AthleteCondition} par {@code AthleteId} (ADR-027). L'identité d'un athlète est désormais
 * transverse (Roster le possède, Athletics et Programming le référencent), comme {@code UserId}.
 */
public record AthleteId(UUID value) {

    public AthleteId {
        if (value == null) {
            throw new IllegalArgumentException("AthleteId ne peut pas encapsuler un UUID null");
        }
    }

    public static AthleteId generate() {
        return new AthleteId(UuidCreator.getTimeOrderedEpoch());
    }

    public static AthleteId from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("AthleteId ne peut pas être construit depuis une chaîne null");
        }
        try {
            return new AthleteId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("AthleteId invalide : « " + raw + " » n'est pas un UUID", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
