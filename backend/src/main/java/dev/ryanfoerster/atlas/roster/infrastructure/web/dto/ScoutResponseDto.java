package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.application.command.ScoutResult;

/** Réponse de POST /api/roster/scout : l'id (pour recruter) + le candidat (pour afficher). */
public record ScoutResponseDto(String candidateId, AthleteCandidateDto candidate) {

    public static ScoutResponseDto from(ScoutResult result) {
        return new ScoutResponseDto(result.candidateId().toString(), AthleteCandidateDto.from(result.candidate()));
    }
}
