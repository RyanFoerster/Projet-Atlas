package dev.ryanfoerster.atlas.athletics.infrastructure.web;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrat HTTP du endpoint condition : forme du JSON, indice/état, 404 id malformé, 401 non authentifié. */
@AutoConfigureMockMvc
class AthleteConditionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private AthleteConditionRepository conditionRepository;

    private UserId someUser() {
        User user = userRepository.save(User.register(
                Email.of("viewer-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Viewer"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        return user.id();
    }

    @Test
    void an_athlete_without_a_condition_reads_neutral() throws Exception {
        UserId viewer = someUser();

        mockMvc.perform(get("/api/athletes/" + UUID.randomUUID() + "/condition").with(user(viewer.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fitness").value(0.0))
                .andExpect(jsonPath("$.fatigue").value(0.0))
                .andExpect(jsonPath("$.formIndex").value(50))
                .andExpect(jsonPath("$.formState").value("FRAIS"));
    }

    @Test
    void a_trained_mirror_exposes_its_form() throws Exception {
        UserId owner = someUser();
        AthleteId mirrorId = createMirror(owner);
        // Fatigue nulle → reste nulle sous décroissance → performance = fitness → indice 100 (affûté),
        // déterministe quel que soit le « maintenant » de lecture (lazy compute).
        conditionRepository.save(AthleteCondition.reconstitute(mirrorId,
                new FitnessFatigueState(20.0, 0.0, Instant.parse("2026-01-01T00:00:00Z"))));

        mockMvc.perform(get("/api/athletes/" + mirrorId + "/condition").with(user(owner.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteId").value(mirrorId.toString()))
                .andExpect(jsonPath("$.formState").value("AFFUTE"))
                .andExpect(jsonPath("$.formIndex").value(100));
    }

    @Test
    void a_malformed_athlete_id_returns_404() throws Exception {
        mockMvc.perform(get("/api/athletes/not-a-uuid/condition").with(user(someUser().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void an_unauthenticated_request_is_rejected() throws Exception {
        mockMvc.perform(get("/api/athletes/" + UUID.randomUUID() + "/condition"))
                .andExpect(status().is4xxClientError());
    }

    private AthleteId createMirror(UserId owner) {
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(80), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(owner, NOW).addMirror(request, new ProceduralAthleteGenerator(), 42L, NOW));
        return rosterRepository.findByOwnerId(owner).orElseThrow().mirrorAthlete().orElseThrow().id();
    }
}
