package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Genetics;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.Map;

/**
 * Domain service stateless de génération procédurale d'athlètes (ADR-021).
 *
 * <p><strong>Hasard injecté, jamais interne</strong> : tout repose sur un {@code seed} passé en
 * paramètre, jamais sur un {@code new Random()} caché — sinon impossible de tester les invariants
 * de façon reproductible (« même seed → même résultat »).
 */
public interface AthleteGenerator {

    /**
     * Génère un candidat virtuel cohérent avec un tier de rareté donné (la rareté est tirée en amont
     * par {@link RarityRoller}). Déterministe : {@code (seed, rarity)} identiques → candidat identique.
     */
    AthleteCandidate generateCandidate(long seed, Rarity rarity);

    /**
     * Génère la génétique de l'athlète <strong>miroir</strong> : base aléatoire (seed) dont les axes
     * de force sont <em>influencés</em> par les ratios force/poids des 1RM saisis (ADR-021).
     */
    Genetics generateGeneticsForMirror(Map<MovementPattern, OneRepMax> oneRepMaxes, Weight bodyWeight,
                                       Gender gender, long seed);
}
