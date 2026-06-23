package dev.ryanfoerster.atlas.identity.infrastructure.web.dto;

/**
 * Réponse de {@code GET /api/auth/magic-link/consume}.
 * <ul>
 *   <li>{@code newUser = false} : Player existant, session établie → le frontend va à l'accueil.</li>
 *   <li>{@code newUser = true} : email vérifié, compte pas encore créé → le frontend redirige vers
 *       l'onboarding (saisie du nom) puis appelle {@code complete-signup}.</li>
 * </ul>
 */
public record ConsumeResponseDto(boolean newUser) {
}
