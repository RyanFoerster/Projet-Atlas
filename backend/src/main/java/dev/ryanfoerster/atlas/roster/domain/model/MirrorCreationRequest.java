package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.Map;
import java.util.Objects;

/**
 * Regroupe les inputs du joueur pour créer son athlète miroir. Value object qui évite une signature
 * à rallonge sur {@link Roster#addMirror} (raffinement validé au co-affinage). L'âge est validé plus
 * loin par {@link Athlete} (source unique de la règle 16–50).
 */
public record MirrorCreationRequest(
        AthleteName name,
        int age,
        Weight bodyWeight,
        Height bodyHeight,
        Gender gender,
        Map<MovementPattern, OneRepMax> oneRepMaxes) {

    public MirrorCreationRequest {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bodyWeight, "bodyWeight");
        Objects.requireNonNull(bodyHeight, "bodyHeight");
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(oneRepMaxes, "oneRepMaxes");
        oneRepMaxes = Map.copyOf(oneRepMaxes);
    }
}
