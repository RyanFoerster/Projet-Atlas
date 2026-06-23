package dev.ryanfoerster.atlas.roster.infrastructure.web;

import dev.ryanfoerster.atlas.roster.application.command.CreateMirrorUseCase;
import dev.ryanfoerster.atlas.roster.application.query.GetAthleteUseCase;
import dev.ryanfoerster.atlas.roster.application.query.GetRosterUseCase;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteId;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.AthleteDto;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.CreateMirrorDto;
import dev.ryanfoerster.atlas.roster.infrastructure.web.dto.RosterDto;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/** Endpoints de l'écurie : créer le miroir, voir l'écurie, voir un athlète. */
@RestController
@RequestMapping("/api/roster")
class RosterController {

    private final CreateMirrorUseCase createMirror;
    private final GetRosterUseCase getRoster;
    private final GetAthleteUseCase getAthlete;

    RosterController(CreateMirrorUseCase createMirror, GetRosterUseCase getRoster, GetAthleteUseCase getAthlete) {
        this.createMirror = createMirror;
        this.getRoster = getRoster;
        this.getAthlete = getAthlete;
    }

    @PostMapping("/mirror")
    ResponseEntity<AthleteDto> createMirror(@RequestBody CreateMirrorDto body, Authentication authentication) {
        UserId owner = UserId.from(authentication.getName());
        Athlete mirror = createMirror.createMirror(owner, toRequest(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(AthleteDto.from(mirror));
    }

    @GetMapping
    ResponseEntity<RosterDto> getRoster(Authentication authentication) {
        return getRoster.forOwner(UserId.from(authentication.getName()))
                .map(RosterDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/athletes/{id}")
    ResponseEntity<AthleteDto> getAthlete(@PathVariable String id, Authentication authentication) {
        AthleteId athleteId;
        try {
            athleteId = AthleteId.from(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // id malformé = inexistant
        }
        return getAthlete.forOwner(UserId.from(authentication.getName()), athleteId)
                .map(AthleteDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Construit le VO de demande à partir du DTO web (validation des value objects → 400). */
    private MirrorCreationRequest toRequest(CreateMirrorDto dto) {
        Map<MovementPattern, OneRepMax> oneRepMaxes = new EnumMap<>(MovementPattern.class);
        for (Map.Entry<String, BigDecimal> entry : dto.oneRepMaxes().entrySet()) {
            oneRepMaxes.put(MovementPattern.valueOf(entry.getKey()),
                    OneRepMax.measured(Weight.ofKilograms(entry.getValue())));
        }
        return new MirrorCreationRequest(AthleteName.of(dto.name()), dto.age(),
                Weight.ofKilograms(dto.bodyWeightKg()), Height.ofCentimeters(dto.bodyHeightCm()),
                Gender.valueOf(dto.gender()), oneRepMaxes);
    }
}
