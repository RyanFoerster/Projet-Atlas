package dev.ryanfoerster.atlas.identity.domain.model;

import java.util.regex.Pattern;

/**
 * Nom affiché d'un {@link User} (le « Coach » côté métier).
 *
 * <p>Value object auto-validant. Règles : 2 à 50 caractères après {@code trim}, composé
 * de lettres (accents compris), chiffres, espaces, tirets et apostrophes, et contenant
 * <strong>au moins une lettre</strong>. On autorise les accents et apostrophes pour ne pas
 * exclure les noms réels (« Jean-Éloïse », « O'Connor »), les chiffres pour les pseudos
 * (« Coach123 », « Ryan2 »), mais on refuse les noms purement numériques (« 42 », « 12345 »)
 * ainsi que les caractères de contrôle, symboles et émojis.
 */
public record DisplayName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;

    /** Jeu de caractères autorisé : lettres Unicode (\p{L}, accents inclus), chiffres, espace, tiret, apostrophes droite et typographique. */
    private static final Pattern ALLOWED_CHARS =
            Pattern.compile("^[\\p{L}\\p{N} '’-]+$");

    /** Au moins une lettre Unicode quelque part — interdit les noms purement numériques. */
    private static final Pattern CONTAINS_LETTER =
            Pattern.compile(".*\\p{L}.*");

    public DisplayName {
        if (value == null) {
            throw new InvalidDisplayNameException("Le nom ne peut pas être null");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidDisplayNameException(
                    "Le nom doit faire entre " + MIN_LENGTH + " et " + MAX_LENGTH + " caractères");
        }
        if (!ALLOWED_CHARS.matcher(value).matches()) {
            throw new InvalidDisplayNameException(
                    "Le nom contient des caractères non autorisés : « " + value + " »");
        }
        if (!CONTAINS_LETTER.matcher(value).matches()) {
            throw new InvalidDisplayNameException(
                    "Le nom doit contenir au moins une lettre : « " + value + " »");
        }
    }

    /**
     * Construit un {@code DisplayName} à partir d'une saisie brute : {@code trim} puis valide.
     *
     * @throws InvalidDisplayNameException si la valeur trimée est hors bornes ou mal formée
     */
    public static DisplayName of(String raw) {
        if (raw == null) {
            throw new InvalidDisplayNameException("Le nom ne peut pas être null");
        }
        return new DisplayName(raw.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
