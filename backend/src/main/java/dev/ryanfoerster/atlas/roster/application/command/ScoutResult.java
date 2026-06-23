package dev.ryanfoerster.atlas.roster.application.command;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;

/**
 * Résultat d'un scout : l'id du candidat persisté (pour le recruter ensuite) + le candidat lui-même
 * (pour l'affichage). Le client ne pourra recruter qu'avec l'id (anti-forge, ADR-022).
 */
public record ScoutResult(ScoutedCandidateId candidateId, AthleteCandidate candidate) {
}
