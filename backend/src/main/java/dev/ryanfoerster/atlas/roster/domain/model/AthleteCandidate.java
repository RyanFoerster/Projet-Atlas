package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.Map;
import java.util.Objects;

/**
 * Candidat à scouter : tout ce qu'il faut pour créer un {@link Athlete} virtuel, présenté au joueur
 * <em>avant</em> qu'il accepte. Value object immutable produit par {@code AthleteGenerator}.
 *
 * <p>Réservé aux athlètes <strong>virtuels</strong> (l'athlète miroir, lui, est créé directement
 * depuis la saisie du joueur, sans passer par un candidat).
 */
public record AthleteCandidate(
        AthleteName name,
        int age,
        Weight bodyWeight,
        Height bodyHeight,
        Gender gender,
        Genetics genetics,
        Map<MovementPattern, OneRepMax> baseOneRepMaxes,
        Rarity rarity) {

    public AthleteCandidate {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bodyWeight, "bodyWeight");
        Objects.requireNonNull(bodyHeight, "bodyHeight");
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(genetics, "genetics");
        Objects.requireNonNull(baseOneRepMaxes, "baseOneRepMaxes");
        Objects.requireNonNull(rarity, "rarity");
        baseOneRepMaxes = Map.copyOf(baseOneRepMaxes); // immutabilité réelle
    }
}
