package dev.ryanfoerster.atlas.roster.infrastructure.web;

import dev.ryanfoerster.atlas.roster.application.command.RecruitAthleteUseCase;
import dev.ryanfoerster.atlas.roster.application.command.ScoutAthleteUseCase;
import dev.ryanfoerster.atlas.roster.application.command.ScoutResult;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.domain.model.exceptions.ScoutedCandidateNotUsableException;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.AthleteDto;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.RecruitAthleteDto;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.ScoutResponseDto;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de scouting : proposer un candidat, recruter un candidat par son id. */
@RestController
@RequestMapping("/api/roster")
class ScoutingController {

    private final ScoutAthleteUseCase scoutAthlete;
    private final RecruitAthleteUseCase recruitAthlete;

    ScoutingController(ScoutAthleteUseCase scoutAthlete, RecruitAthleteUseCase recruitAthlete) {
        this.scoutAthlete = scoutAthlete;
        this.recruitAthlete = recruitAthlete;
    }

    @PostMapping("/scout")
    ResponseEntity<ScoutResponseDto> scout(Authentication authentication) {
        ScoutResult result = scoutAthlete.scout();
        return ResponseEntity.ok(ScoutResponseDto.from(result));
    }

    @PostMapping("/recruit")
    ResponseEntity<AthleteDto> recruit(@RequestBody RecruitAthleteDto body, Authentication authentication) {
        UserId owner = UserId.from(authentication.getName());
        ScoutedCandidateId candidateId;
        try {
            candidateId = ScoutedCandidateId.from(body.candidateId());
        } catch (IllegalArgumentException e) {
            throw new ScoutedCandidateNotUsableException("Candidat introuvable ou expiré"); // id malformé → 404
        }
        Athlete recruited = recruitAthlete.recruit(owner, candidateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(AthleteDto.from(recruited));
    }
}
