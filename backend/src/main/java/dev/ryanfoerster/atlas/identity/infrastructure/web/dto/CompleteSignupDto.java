package dev.ryanfoerster.atlas.identity.infrastructure.web.dto;

/**
 * Corps de {@code POST /api/auth/complete-signup}. {@code locale}/{@code timezone} sont optionnels
 * (détectés navigateur côté frontend) ; à défaut, le serveur applique des valeurs par défaut.
 * L'email n'est PAS dans le DTO : il vient de la session temporaire ouverte à la consommation du
 * lien (l'utilisateur ne peut pas le fournir librement).
 */
public record CompleteSignupDto(String displayName, String locale, String timezone) {
}
