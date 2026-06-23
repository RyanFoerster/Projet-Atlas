package dev.ryanfoerster.atlas.identity.infrastructure.web;

import dev.ryanfoerster.atlas.identity.application.command.CompleteSignupCommand;
import dev.ryanfoerster.atlas.identity.application.command.CompleteSignupUseCase;
import dev.ryanfoerster.atlas.identity.application.command.ConsumeMagicLinkUseCase;
import dev.ryanfoerster.atlas.identity.application.command.ConsumeResult;
import dev.ryanfoerster.atlas.identity.application.command.RequestMagicLinkCommand;
import dev.ryanfoerster.atlas.identity.application.command.RequestMagicLinkUseCase;
import dev.ryanfoerster.atlas.identity.application.query.GetCurrentUserUseCase;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.identity.infrastructure.web.dto.CompleteSignupDto;
import dev.ryanfoerster.atlas.identity.infrastructure.web.dto.ConsumeResponseDto;
import dev.ryanfoerster.atlas.identity.infrastructure.web.dto.CurrentUserDto;
import dev.ryanfoerster.atlas.identity.infrastructure.web.dto.RequestMagicLinkDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Endpoints d'authentification par lien magique (ADR-011). Le logout est géré par le filtre
 * Spring Security (cf. {@link SecurityConfig}), pas ici — il n'a aucune logique métier (ADR-017
 * note réversibilité si un event PlayerLoggedOut devient utile).
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    /** Clé de session portant l'email vérifié, entre la consommation du lien et la finalisation. */
    static final String PENDING_SIGNUP_EMAIL = "atlas.pendingSignupEmail";

    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Europe/Brussels");

    private final RequestMagicLinkUseCase requestMagicLink;
    private final ConsumeMagicLinkUseCase consumeMagicLink;
    private final CompleteSignupUseCase completeSignup;
    private final GetCurrentUserUseCase getCurrentUser;
    private final SecurityContextRepository securityContextRepository;

    AuthController(RequestMagicLinkUseCase requestMagicLink, ConsumeMagicLinkUseCase consumeMagicLink,
                   CompleteSignupUseCase completeSignup, GetCurrentUserUseCase getCurrentUser,
                   SecurityContextRepository securityContextRepository) {
        this.requestMagicLink = requestMagicLink;
        this.consumeMagicLink = consumeMagicLink;
        this.completeSignup = completeSignup;
        this.getCurrentUser = getCurrentUser;
        this.securityContextRepository = securityContextRepository;
    }

    /** Demande un lien magique. Toujours 202, même si l'email est inconnu (anti-énumération). */
    @PostMapping("/magic-link/request")
    ResponseEntity<Void> request(@RequestBody RequestMagicLinkDto body, HttpServletRequest http) {
        requestMagicLink.request(new RequestMagicLinkCommand(
                body.email(), http.getRemoteAddr(), http.getHeader("User-Agent")));
        return ResponseEntity.accepted().build();
    }

    /**
     * Consomme un lien magique. Player existant → session ouverte. Nouvel email → on mémorise
     * l'email vérifié en session et on signale {@code newUser:true} (onboarding à suivre, flow A).
     */
    @GetMapping("/magic-link/consume")
    ResponseEntity<ConsumeResponseDto> consume(@RequestParam("token") String token,
                                               HttpServletRequest request, HttpServletResponse response) {
        ConsumeResult result = consumeMagicLink.consume(token);

        if (result.newPlayer()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(PENDING_SIGNUP_EMAIL, result.verifiedEmail().value());
            return ResponseEntity.ok(new ConsumeResponseDto(true));
        }

        establishSession(result.playerId(), request, response);
        return ResponseEntity.ok(new ConsumeResponseDto(false));
    }

    /** Finalise l'inscription d'un nouveau Player : nom saisi + email vérifié en session → compte + login. */
    @PostMapping("/complete-signup")
    ResponseEntity<CurrentUserDto> completeSignup(@RequestBody CompleteSignupDto body,
                                                  HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        Object pendingEmail = session == null ? null : session.getAttribute(PENDING_SIGNUP_EMAIL);
        if (pendingEmail == null) {
            // Pas d'email vérifié en session : on n'a rien à finaliser.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Locale locale = body.locale() != null ? Locale.forLanguageTag(body.locale()) : DEFAULT_LOCALE;
        ZoneId timezone = body.timezone() != null ? ZoneId.of(body.timezone()) : DEFAULT_TIMEZONE;

        User player = completeSignup.completeSignup(
                new CompleteSignupCommand(pendingEmail.toString(), body.displayName(), locale, timezone));

        session.removeAttribute(PENDING_SIGNUP_EMAIL);
        establishSession(player.id(), request, response);
        return ResponseEntity.ok(CurrentUserDto.from(player));
    }

    /** Le Player courant (session valide requise par la config sécurité). */
    @GetMapping("/me")
    ResponseEntity<CurrentUserDto> me(Authentication authentication) {
        UserId id = UserId.from(authentication.getName());
        return getCurrentUser.byId(id)
                .map(CurrentUserDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Ouvre une session Spring Security pour le Player et la persiste (cookie JSESSIONID). */
    private void establishSession(UserId playerId, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                playerId.value().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
