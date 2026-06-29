package dev.ryanfoerster.atlas;

import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.roster.api.AthleteStrengthCeiling;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.StrengthStandards;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import dev.ryanfoerster.atlas.shared.events.CurrentStatsProgressed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Test end-to-end de l'<strong>ownership par event</strong> de la Couche 3 (GATE 3b, ADR-032) : Athletics
 * publie {@code CurrentStatsProgressed} → <strong>Roster</strong> le consomme en async → le 1RM matérialisé
 * de l'athlète ({@code CurrentStats}, la carte) monte. On publie l'event directement (le câblage de
 * l'émission côté Athletics est la Couche 3c) pour prouver la moitié <em>matérialisation</em> en isolation.
 *
 * <p>Vérifie aussi le port {@code findStrengthCeiling} : Roster calcule le plafond génétique
 * ({@code bodyweight × ratio_élite × strengthAffinity}) pour les grands lifts, et rien pour ROW/CHIN_UP (T3).
 */
@EnableScenarios
class CurrentStatsProgressedEventDrivenTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final Instant PROGRESSED_AT = Instant.parse("2026-06-25T18:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();
    private static final double BODYWEIGHT_KG = 80.0;

    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RosterQueryPort rosterQueryPort;

    @Test
    void a_progression_event_materializes_a_higher_one_rep_max_on_the_mirror(Scenario scenario) {
        UserId owner = createOwnerWithMirror();
        AthleteId mirrorId = mirror(owner).id();
        assertThat(squatKg(mirrorId)).isEqualTo(140.0); // 1RM de départ du miroir

        scenario.publish(new CurrentStatsProgressed(mirrorId.value(), MovementPattern.SQUAT, 150.0, PROGRESSED_AT))
                .andWaitForStateChange(() -> squatKg(mirrorId), kg -> kg >= 149.0)
                .andVerify(kg -> assertThat(kg).isEqualTo(150.0));
    }

    @Test
    void roster_computes_the_genetic_ceiling_from_elite_standards_for_big_lifts_only() {
        UserId owner = createOwnerWithMirror();
        Athlete mirror = mirror(owner);
        AthleteStrengthCeiling ceiling = rosterQueryPort.findStrengthCeiling(mirror.id()).orElseThrow();

        double expectedSquat = BODYWEIGHT_KG
                * StrengthStandards.eliteRatio(MovementPattern.SQUAT, Gender.MALE)
                * mirror.genetics().strengthAffinity(MovementPattern.SQUAT);
        assertThat(ceiling.ceilingOneRmKg(MovementPattern.SQUAT)).isCloseTo(expectedSquat, within(1e-9));
        // Pas de standard → pas de plafond (ROW/CHIN_UP ne progressent pas, ADR-033 §5).
        assertThat(ceiling.ceilingOneRmKg(MovementPattern.ROW)).isNull();
        assertThat(ceiling.ceilingOneRmKg(MovementPattern.CHIN_UP)).isNull();
        // Le plafond élite est bien au-dessus du 1RM de départ (intermédiaire) — il y a du headroom.
        assertThat(ceiling.ceilingOneRmKg(MovementPattern.SQUAT)).isGreaterThan(squatKg(mirror.id()));
    }

    private UserId createOwnerWithMirror() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(BODYWEIGHT_KG), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(user.id(), NOW).addMirror(request, GENERATOR, 42L, NOW));
        return user.id();
    }

    private Athlete mirror(UserId owner) {
        return rosterRepository.findByOwnerId(owner).orElseThrow().mirrorAthlete().orElseThrow();
    }

    private double squatKg(AthleteId athleteId) {
        return rosterRepository.findByAthleteId(athleteId).orElseThrow()
                .findAthlete(athleteId).orElseThrow()
                .currentOneRepMax(MovementPattern.SQUAT).orElseThrow()
                .weight().toKilograms().doubleValue();
    }
}
