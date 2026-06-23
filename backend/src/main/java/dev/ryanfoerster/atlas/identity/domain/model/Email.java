package dev.ryanfoerster.atlas.identity.domain.model;

import java.util.regex.Pattern;

/**
 * Adresse email d'un {@link User}, sous forme normalisée (trim + minuscules).
 *
 * <p>Value object auto-validant : on ne peut pas construire un {@code Email} invalide.
 * La normalisation (suppression des espaces de bord, passage en minuscules) garantit
 * qu'une même adresse saisie {@code "Ryan@Example.COM "} ou {@code "ryan@example.com"}
 * produit deux {@code Email} égaux — essentiel pour la contrainte d'unicité au niveau
 * du repository.
 *
 * <p>La validation utilise une regex volontairement <em>pragmatique</em> : suffisamment
 * stricte pour rejeter les saisies manifestement invalides (pas de {@code @}, pas de
 * domaine, double {@code @}, etc.), sans prétendre implémenter toute la RFC 5322 — une
 * regex RFC complète est illisible et accepte des adresses que personne n'utilise. La
 * borne de longueur (254) vient de la RFC 5321.
 */
public record Email(String value) {

    /** Longueur max d'une adresse email selon la RFC 5321. */
    private static final int MAX_LENGTH = 254;

    private static final Pattern PATTERN =
            Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    /**
     * Constructeur canonique : valide l'invariant. Tout {@code Email} qui existe est donc
     * forcément valide et normalisé, quelle que soit la voie de construction.
     */
    public Email {
        if (value == null) {
            throw new InvalidEmailException("L'email ne peut pas être null");
        }
        if (value.isBlank()) {
            throw new InvalidEmailException("L'email ne peut pas être vide");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailException(
                    "L'email dépasse la longueur max de " + MAX_LENGTH + " caractères (RFC 5321)");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException("Format d'email invalide : « " + value + " »");
        }
    }

    /**
     * Construit un {@code Email} à partir d'une saisie brute : normalise (trim + lowercase)
     * puis valide. C'est la voie d'entrée standard.
     *
     * @throws InvalidEmailException si la valeur normalisée n'est pas un email valide
     */
    public static Email of(String raw) {
        if (raw == null) {
            throw new InvalidEmailException("L'email ne peut pas être null");
        }
        return new Email(raw.trim().toLowerCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
