package dev.ryanfoerster.atlas.identity.infrastructure.web.dto;

/** Corps de {@code POST /api/auth/magic-link/request}. */
public record RequestMagicLinkDto(String email) {
}
