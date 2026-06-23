package dev.ryanfoerster.atlas.identity.infrastructure.web;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.MagicLinkJpaRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MagicLinkRepository magicLinkRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private MagicLinkJpaRepository magicLinkJpaRepository;

    @BeforeEach
    void cleanUp() {
        magicLinkJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    private MagicLinkToken issueLinkFor(String email) {
        Instant now = Instant.now();
        MagicLinkToken token = MagicLinkToken.generate();
        magicLinkRepository.save(
                MagicLink.issue(token, Email.of(email), now, now.plus(Duration.ofMinutes(15)), null, null));
        return token;
    }

    // ---- Demande de lien magique -------------------------------------------------------------

    @Test
    void request_returns_202_with_csrf() throws Exception {
        mockMvc.perform(post("/api/auth/magic-link/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ryan@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void request_without_csrf_is_forbidden() throws Exception {
        mockMvc.perform(post("/api/auth/magic-link/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ryan@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void request_with_invalid_email_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/magic-link/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- Consommation ------------------------------------------------------------------------

    @Test
    void consume_unknown_token_returns_400() throws Exception {
        mockMvc.perform(get("/api/auth/magic-link/consume").param("token", MagicLinkToken.generate().value().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void existing_player_consume_logs_in_and_me_returns_the_player() throws Exception {
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), Instant.now().minus(Duration.ofDays(1))));
        MagicLinkToken token = issueLinkFor("ryan@example.com");

        MvcResult consume = mockMvc.perform(get("/api/auth/magic-link/consume").param("token", token.value().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(false))
                .andReturn();

        MockHttpSession session = (MockHttpSession) consume.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ryan@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ryan"));
    }

    @Test
    void me_without_session_is_401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Onboarding nouveau Player (flow A) --------------------------------------------------

    @Test
    void new_email_flow_creates_account_via_complete_signup() throws Exception {
        MagicLinkToken token = issueLinkFor("newbie@example.com");

        MvcResult consume = mockMvc.perform(get("/api/auth/magic-link/consume").param("token", token.value().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newUser").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) consume.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/complete-signup")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Newbie\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newbie@example.com"))
                .andExpect(jsonPath("$.displayName").value("Newbie"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Newbie"));
    }

    @Test
    void complete_signup_without_pending_email_is_401() throws Exception {
        mockMvc.perform(post("/api/auth/complete-signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Nobody\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Logout ------------------------------------------------------------------------------

    @Test
    void logout_returns_204_and_clears_the_session() throws Exception {
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), Instant.now().minus(Duration.ofDays(1))));
        MagicLinkToken token = issueLinkFor("ryan@example.com");
        MvcResult consume = mockMvc.perform(get("/api/auth/magic-link/consume").param("token", token.value().toString()))
                .andReturn();
        MockHttpSession session = (MockHttpSession) consume.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        // la session a été invalidée → /me avec la même session retombe en 401
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
