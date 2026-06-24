package dev.ryanfoerster.atlas.personaltraining.infrastructure.web;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.persistence.WorkoutSessionJpaRepository;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WorkoutSessionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkoutSessionJpaRepository jpaRepository;

    private UserId createUser() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), Instant.now()));
        return user.id();
    }

    // Séance riche : 1 composé (SQUAT, 2 séries chargées) + 1 accessoire (BICEPS, dont une au poids de corps).
    private static final String RICH_PAYLOAD = """
            {
              "performedAt": "2026-06-23T18:30:00Z",
              "durationMinutes": 75,
              "notes": "Bonne séance, sensations propres",
              "exercises": [
                {
                  "name": "Back Squat",
                  "pattern": "SQUAT",
                  "sets": [
                    { "reps": 5, "weightKg": 140, "rpe": 7.5 },
                    { "reps": 5, "weightKg": 140, "rpe": 8.5 }
                  ]
                },
                {
                  "name": "Barbell Curl",
                  "region": "BICEPS",
                  "sets": [
                    { "reps": 12, "weightKg": 20, "rpe": null },
                    { "reps": 12, "weightKg": null, "rpe": null }
                  ]
                }
              ]
            }
            """;

    @Test
    void post_logs_a_session_and_returns_201_with_full_shape() throws Exception {
        UserId owner = createUser();

        MvcResult result = mockMvc.perform(post("/api/personal-training/sessions")
                        .with(user(owner.toString())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(RICH_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.totalSets").value(4))
                .andExpect(jsonPath("$.totalReps").value(34))                  // 5+5 + 12+12
                .andExpect(jsonPath("$.patternsCovered.length()").value(1))    // accessoire NON compté
                .andExpect(jsonPath("$.patternsCovered[0]").value("SQUAT"))
                .andExpect(jsonPath("$.exercises[0].category").value("COMPOUND_FORCE"))
                .andExpect(jsonPath("$.exercises[0].pattern").value("SQUAT"))
                .andExpect(jsonPath("$.exercises[1].category").value("ACCESSORY"))
                .andExpect(jsonPath("$.exercises[1].region").value("BICEPS"))
                .andExpect(jsonPath("$.exercises[1].sets[1].reps").value(12))  // série au poids de corps
                .andReturn();
        System.out.println("=== POST 201 BODY ===\n" + result.getResponse().getContentAsString());
    }

    @Test
    void get_history_returns_200_paginated_most_recent_first() throws Exception {
        UserId owner = createUser();
        logSession(owner, "2026-06-20T10:00:00Z"); // plus ancienne
        logSession(owner, "2026-06-22T10:00:00Z"); // plus récente

        MvcResult result = mockMvc.perform(get("/api/personal-training/sessions")
                        .param("page", "0").param("size", "20").with(user(owner.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.sessions[0].performedAt").value("2026-06-22T10:00:00Z")) // DESC
                .andExpect(jsonPath("$.sessions[0].exerciseCount").value(1))
                .andReturn();
        System.out.println("=== GET history BODY ===\n" + result.getResponse().getContentAsString());
    }

    @Test
    void get_detail_returns_200_for_own_session() throws Exception {
        UserId owner = createUser();
        String id = logSession(owner, "2026-06-21T10:00:00Z");

        MvcResult result = mockMvc.perform(get("/api/personal-training/sessions/" + id)
                        .with(user(owner.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andReturn();
        System.out.println("=== GET detail BODY ===\n" + result.getResponse().getContentAsString());
    }

    @Test
    void get_detail_of_another_users_session_returns_404() throws Exception {
        UserId owner = createUser();
        UserId other = createUser();
        String id = logSession(owner, "2026-06-21T10:00:00Z");

        mockMvc.perform(get("/api/personal-training/sessions/" + id).with(user(other.toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_detail_of_unknown_or_malformed_id_returns_404() throws Exception {
        UserId owner = createUser();
        mockMvc.perform(get("/api/personal-training/sessions/" + UUID.randomUUID()).with(user(owner.toString())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/personal-training/sessions/not-a-uuid").with(user(owner.toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_empty_session_returns_400() throws Exception {
        UserId owner = createUser();
        mockMvc.perform(post("/api/personal-training/sessions")
                        .with(user(owner.toString())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"performedAt\":\"2026-06-23T18:30:00Z\",\"exercises\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void post_session_performed_in_the_future_returns_400() throws Exception {
        UserId owner = createUser();
        String futurePayload = """
                {
                  "performedAt": "2099-01-01T00:00:00Z",
                  "exercises": [
                    { "name": "Back Squat", "pattern": "SQUAT", "sets": [ { "reps": 5, "weightKg": 100 } ] }
                  ]
                }
                """;
        mockMvc.perform(post("/api/personal-training/sessions")
                        .with(user(owner.toString())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(futurePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void post_without_authentication_returns_401() throws Exception {
        mockMvc.perform(post("/api/personal-training/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(RICH_PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    /** Logge une séance minimale (1 squat) à une date donnée, renvoie l'id créé. */
    private String logSession(UserId owner, String performedAt) throws Exception {
        String payload = """
                {
                  "performedAt": "%s",
                  "exercises": [
                    { "name": "Back Squat", "pattern": "SQUAT", "sets": [ { "reps": 5, "weightKg": 100, "rpe": 8 } ] }
                  ]
                }
                """.formatted(performedAt);
        MvcResult result = mockMvc.perform(post("/api/personal-training/sessions")
                        .with(user(owner.toString())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
