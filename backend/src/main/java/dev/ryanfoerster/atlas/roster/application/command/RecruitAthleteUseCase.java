package dev.ryanfoerster.atlas.roster.application.command;

import dev.ryanfoerster.atlas.roster.api.events.AthleteRecruited;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.domain.model.exceptions.ScoutedCandidateNotUsableException;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.port.ScoutedCandidateRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use case : recruter un candidat scouté. On part de son <strong>id</strong> (jamais du candidat
 * ré-envoyé par le client — anti-forge, ADR-022). Orchestration : retrouver, consommer (le domaine
 * vérifie le TTL/usage unique), ajouter au roster (le domaine recrute), publier.
 */
@Service
public class RecruitAthleteUseCase {

    private final ScoutedCandidateRepository scoutedCandidateRepository;
    private final RosterRepository rosterRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public RecruitAthleteUseCase(ScoutedCandidateRepository scoutedCandidateRepository,
                                 RosterRepository rosterRepository, ApplicationEventPublisher eventPublisher,
                                 Clock clock) {
        this.scoutedCandidateRepository = scoutedCandidateRepository;
        this.rosterRepository = rosterRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public Athlete recruit(UserId ownerId, ScoutedCandidateId candidateId) {
        Instant now = clock.instant();

        ScoutedCandidate scouted = scoutedCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new ScoutedCandidateNotUsableException("Candidat introuvable ou expiré"));
        scoutedCandidateRepository.save(scouted.consume(now)); // le domaine refuse expiré/déjà recruté

        Roster roster = rosterRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new IllegalStateException("Aucune écurie pour recruter"));
        Set<AthleteId> before = roster.athletes().stream().map(Athlete::id).collect(Collectors.toSet());
        Roster saved = rosterRepository.save(roster.recruit(scouted.candidate(), now));

        Athlete recruited = saved.athletes().stream()
                .filter(athlete -> !before.contains(athlete.id()))
                .findFirst()
                .orElseThrow();
        eventPublisher.publishEvent(new AthleteRecruited(
                recruited.id().value(), saved.id().value(), recruited.rarity().name(), now));
        return recruited;
    }
}
