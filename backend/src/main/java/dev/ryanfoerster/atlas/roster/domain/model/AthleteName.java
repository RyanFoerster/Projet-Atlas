package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidAthleteNameException;

import java.util.regex.Pattern;

/**
 * Nom d'un athlète. Même pattern que {@code DisplayName} d'identity (value object auto-validant) :
 * 2 à 50 caractères après trim, lettres (accents) / chiffres / espaces / tirets / apostrophes, avec
 * au moins une lettre. On ne factorise pas avec {@code DisplayName} : bounded contexts distincts
 * (ADR-001), critères potentiellement divergents à l'avenir.
 */
public record AthleteName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[\\p{L}\\p{N} '’-]+$");
    private static final Pattern CONTAINS_LETTER = Pattern.compile(".*\\p{L}.*");

    public AthleteName {
        if (value == null) {
            throw new InvalidAthleteNameException("Le nom ne peut pas être null");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidAthleteNameException(
                    "Le nom doit faire entre " + MIN_LENGTH + " et " + MAX_LENGTH + " caractères");
        }
        if (!ALLOWED_CHARS.matcher(value).matches()) {
            throw new InvalidAthleteNameException("Le nom contient des caractères non autorisés : « " + value + " »");
        }
        if (!CONTAINS_LETTER.matcher(value).matches()) {
            throw new InvalidAthleteNameException("Le nom doit contenir au moins une lettre : « " + value + " »");
        }
    }

    public static AthleteName of(String raw) {
        if (raw == null) {
            throw new InvalidAthleteNameException("Le nom ne peut pas être null");
        }
        return new AthleteName(raw.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
