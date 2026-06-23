package dev.ryanfoerster.atlas.identity.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Configuration Spring Security 7 du module identity (ADR-011, ADR-012).
 *
 * <p>Authentification <strong>par session</strong> (cookie {@code JSESSIONID} HttpOnly), pas de
 * JWT : révocation triviale (invalider la session), rien de sensible exposé au JS. La session est
 * établie programmatiquement par {@link AuthController} après consommation d'un lien magique.
 *
 * <p><strong>CSRF</strong> activé (obligatoire dès qu'on a des cookies de session). Pattern SPA
 * pour Angular : le jeton est déposé dans un cookie {@code XSRF-TOKEN} lisible par le JS
 * ({@link CookieCsrfTokenRepository#withHttpOnlyFalse}), Angular le renvoie tel quel dans l'en-tête
 * {@code X-XSRF-TOKEN} → on utilise le {@link CsrfTokenRequestAttributeHandler} simple (pas le
 * handler XOR, que le client Angular ne sait pas reproduire). Le {@code CsrfCookieFilter} force le
 * rendu du jeton à chaque requête pour que le cookie soit toujours présent côté client.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        // Charge le jeton de façon eager (opt-out du deferred) afin que le CsrfCookieFilter le voie.
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
                .authorizeHttpRequests(auth -> auth
                        // Endpoints d'auth nécessitant une session établie :
                        .requestMatchers("/api/auth/me").authenticated()
                        // Le reste de l'auth (request, consume, complete-signup, logout) est public :
                        .requestMatchers("/api/auth/**").permitAll()
                        // Infra publique :
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Tout le reste de l'API exige une authentification :
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                // On référence explicitement notre source CORS : Spring MVC en publie une autre
                // (mvcHandlerMappingIntrospector), donc l'injection par type serait ambiguë.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, authn) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                // API : pas de page de login ni de Basic auth ; un accès non authentifié renvoie 401.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /** Persistance du SecurityContext en session — injectée par {@link AuthController} pour ouvrir la session. */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200")); // dev ; à élargir/configurer en prod
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // nécessaire pour envoyer le cookie de session cross-origin
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * Force le rendu du jeton CSRF à chaque requête, garantissant que le cookie {@code XSRF-TOKEN}
     * est toujours présent côté client (sinon le tout premier POST d'un client neuf échouerait,
     * faute de cookie). Pattern documenté pour les SPA.
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken(); // déclenche le dépôt du cookie
            }
            filterChain.doFilter(request, response);
        }
    }
}
