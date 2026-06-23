package dev.ryanfoerster.atlas.identity.infrastructure.web.dto;

import dev.ryanfoerster.atlas.identity.domain.model.User;

import java.time.Instant;

/** Réponse de {@code GET /api/auth/me} : le Player courant, en types sérialisables. */
public record CurrentUserDto(
        String id,
        String email,
        String displayName,
        String locale,
        String timezone,
        Instant createdAt,
        Instant lastLoginAt) {

    public static CurrentUserDto from(User user) {
        return new CurrentUserDto(
                user.id().value().toString(),
                user.email().value(),
                user.displayName().value(),
                user.locale().toLanguageTag(),
                user.timezone().getId(),
                user.createdAt(),
                user.lastLoginAt().orElse(null));
    }
}
