package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

/** Corps de POST /api/roster/recruit : seul l'id du candidat scouté (anti-forge, ADR-022). */
public record RecruitAthleteDto(String candidateId) {
}
