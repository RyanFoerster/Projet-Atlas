package dev.ryanfoerster.atlas.roster.application.command;

import dev.ryanfoerster.atlas.roster.api.events.MirrorAthleteCreated;
import dev.ryanfoerster.atlas.roster.api.events.RosterCreated;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

/**
 * Use case : créer l'athlète miroir du Player. Pure orchestration — toute la logique métier (invariant
 * « un seul miroir », génération hybride de la génétique) vit dans le domaine ({@code Roster.addMirror}).
 */
@Service
public class CreateMirrorUseCase {

    private final RosterRepository rosterRepository;
    private final AthleteGenerator generator;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Random random;

    public CreateMirrorUseCase(RosterRepository rosterRepository, AthleteGenerator generator,
                               ApplicationEventPublisher eventPublisher, Clock clock, Random random) {
        this.rosterRepository = rosterRepository;
        this.generator = generator;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.random = random;
    }

    @Transactional
    public Athlete createMirror(UserId ownerId, MirrorCreationRequest request) {
        Instant now = clock.instant();

        Optional<Roster> existing = rosterRepository.findByOwnerId(ownerId);
        Roster roster = existing.orElseGet(() -> Roster.createFor(ownerId, now));

        Roster withMirror = roster.addMirror(request, generator, random.nextLong(), now);
        Roster saved = rosterRepository.save(withMirror);

        if (existing.isEmpty()) {
            eventPublisher.publishEvent(new RosterCreated(saved.id().value(), ownerId.value(), saved.createdAt()));
        }
        Athlete mirror = saved.mirrorAthlete().orElseThrow();
        eventPublisher.publishEvent(new MirrorAthleteCreated(mirror.id().value(), saved.id().value(), now));
        return mirror;
    }
}
