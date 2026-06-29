package dev.ryanfoerster.atlas.roster.application.eventhandler;

import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import dev.ryanfoerster.atlas.shared.events.CurrentStatsProgressed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Modulith de l'event {@link CurrentStatsProgressed} (publié par Athletics, Couche 3). Matérialise
 * le nouveau 1RM dans les {@code CurrentStats} de l'athlète — la <strong>carte</strong>, source de vérité de
 * Roster. C'est l'autre moitié de l'ownership par event (ADR-032) : Athletics calcule la dynamique, Roster
 * possède le 1RM matérialisé ; ils ne s'écrivent jamais l'un l'autre, ils communiquent par cet event.
 *
 * <p><strong>{@link ApplicationModuleListener}</strong> = {@code @TransactionalEventListener(AFTER_COMMIT)}
 * + {@code @Async} + {@code @Transactional(REQUIRES_NEW)}. Même topologie que {@link WorkoutLoggedHandler}.
 * Cohérence éventuelle assumée (ADR-023) : la condition est déjà sauvée côté Athletics ; la carte est mise à
 * jour juste après, de façon asynchrone, dans sa propre transaction. Livraison durable, at-least-once.
 *
 * <p><strong>Idempotence + cliquet</strong> : {@code Roster.progressAthleteStat} → {@code
 * Athlete.progressOneRepMax} n'applique qu'une <em>hausse</em> du 1RM (no-op sinon). Rejouer cet event (au
 * restart, après échec, ou réordonnancé) ne peut donc pas faire reculer ni doubler un 1RM — le cliquet est
 * gardé jusqu'au point de matérialisation.
 *
 * <p><strong>Isolation Modulith</strong> : ce handler n'importe que {@code athletics.api.events.*} (l'event),
 * jamais le domaine interne d'Athletics.
 */
@Component
public class CurrentStatsProgressedHandler {

    private final RosterRepository rosterRepository;

    public CurrentStatsProgressedHandler(RosterRepository rosterRepository) {
        this.rosterRepository = rosterRepository;
    }

    @ApplicationModuleListener
    public void on(CurrentStatsProgressed event) {
        AthleteId athleteId = new AthleteId(event.athleteId());
        rosterRepository.findByAthleteId(athleteId).ifPresent(roster -> {
            OneRepMax progressed = OneRepMax.measured(Weight.ofKilograms(event.newOneRepMaxKg()));
            Roster updated = roster.progressAthleteStat(athleteId, event.pattern(), progressed);
            rosterRepository.save(updated);
        });
    }
}
