package dev.ryanfoerster.atlas.personaltraining.infrastructure.web;

import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutUseCase;
import dev.ryanfoerster.atlas.personaltraining.application.query.GetWorkoutHistoryUseCase;
import dev.ryanfoerster.atlas.personaltraining.application.query.GetWorkoutSessionUseCase;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSessionId;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto.LogWorkoutDto;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto.WorkoutHistoryDto;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto.WorkoutSessionDto;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints des séances IRL : logger une séance, lister l'historique, voir le détail d'une séance. */
@RestController
@RequestMapping("/api/personal-training/sessions")
class WorkoutSessionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final LogWorkoutUseCase logWorkout;
    private final GetWorkoutHistoryUseCase getHistory;
    private final GetWorkoutSessionUseCase getSession;

    WorkoutSessionController(LogWorkoutUseCase logWorkout, GetWorkoutHistoryUseCase getHistory,
                            GetWorkoutSessionUseCase getSession) {
        this.logWorkout = logWorkout;
        this.getHistory = getHistory;
        this.getSession = getSession;
    }

    @PostMapping
    ResponseEntity<WorkoutSessionDto> log(@RequestBody LogWorkoutDto body, Authentication authentication) {
        UserId owner = UserId.from(authentication.getName());
        WorkoutSession session = logWorkout.logWorkout(owner, LogWorkoutDtoMapper.toCommand(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutSessionDto.from(session));
    }

    @GetMapping
    ResponseEntity<WorkoutHistoryDto> history(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              Authentication authentication) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        UserId owner = UserId.from(authentication.getName());
        GetWorkoutHistoryUseCase.WorkoutHistory history = getHistory.historyFor(owner, safePage, safeSize);
        return ResponseEntity.ok(WorkoutHistoryDto.of(history.sessions(), safePage, safeSize, history.total()));
    }

    @GetMapping("/{id}")
    ResponseEntity<WorkoutSessionDto> detail(@PathVariable String id, Authentication authentication) {
        WorkoutSessionId sessionId;
        try {
            sessionId = WorkoutSessionId.from(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // id malformé = inexistant
        }
        return getSession.forOwner(UserId.from(authentication.getName()), sessionId)
                .map(WorkoutSessionDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
