package dev.ryanfoerster.atlas.identity.domain.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root du module identity — le « Player » côté métier (cf. glossaire).
 *
 * <p><strong>Pourquoi une classe et pas un {@code record} ?</strong> C'est la distinction
 * DDD fondamentale entre value object et entity :
 * <ul>
 *   <li>Un <b>value object</b> (UserId, Email…) est défini par sa valeur → égalité
 *       structurelle sur tous ses champs → {@code record} est parfait.</li>
 *   <li>Une <b>entity / aggregate</b> a une <b>identité</b> qui persiste à travers ses
 *       changements d'état. Deux instances de {@code User} avec le même {@link UserId}
 *       représentent le <em>même</em> Player à deux moments (avant/après un login), et
 *       doivent être <b>égales</b>. L'égalité se fait donc <b>par identité</b> ({@code id}),
 *       pas par valeur — ce qu'un {@code record} ne sait pas faire.</li>
 * </ul>
 *
 * <p><strong>Immutabilité fonctionnelle.</strong> L'aggregate ne se mute jamais en place :
 * chaque comportement métier ({@link #recordLogin}, {@link #updateDisplayName}…) retourne
 * une <em>nouvelle</em> instance. Le pattern retenu pour le projet est le
 * <b>« business method + constructeur de copie privé »</b> : les méthodes publiques portent
 * l'intention métier et garantissent les invariants, et délèguent la recopie au constructeur
 * canonique privé. (Alternatives écartées : des {@code withXxx()} publics exposeraient une
 * recopie champ par champ qui court-circuite les invariants ; un builder ajouterait de la
 * cérémonie injustifiée pour 7 champs. Décision documentée dans le mini-cours sprint-01.)
 *
 * <p><strong>Invariants garantis :</strong>
 * <ul>
 *   <li>tous les champs obligatoires sont non null ;</li>
 *   <li>{@code email}/{@code displayName} valides — garanti par leurs value objects ;</li>
 *   <li>{@code createdAt ≤ lastLoginAt} quand un login a été enregistré.</li>
 * </ul>
 */
public final class User {

    private final UserId id;
    private final Email email;
    private final DisplayName displayName;
    private final Locale locale;
    private final ZoneId timezone;
    private final Instant createdAt;
    /** Null tant qu'aucun login n'a été enregistré ; exposé via {@link #lastLoginAt()} en Optional. */
    private final Instant lastLoginAt;

    private User(UserId id, Email email, DisplayName displayName, Locale locale,
                 ZoneId timezone, Instant createdAt, Instant lastLoginAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.timezone = Objects.requireNonNull(timezone, "timezone");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        // Invariant temporel : un login enregistré ne peut pas précéder la création.
        // C'est une incohérence interne (bug / horloge), pas une saisie humaine → erreur technique.
        if (lastLoginAt != null && lastLoginAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("lastLoginAt (" + lastLoginAt
                    + ") ne peut pas précéder createdAt (" + createdAt + ")");
        }
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Inscrit un nouveau Player (signup). Génère un {@link UserId} v7, pas encore de login.
     *
     * @param now instant de référence (injecté, jamais {@code Instant.now()} dans le domaine pur)
     */
    public static User register(Email email, DisplayName displayName, Locale locale,
                                ZoneId timezone, Instant now) {
        return new User(UserId.generate(), email, displayName, locale, timezone, now, null);
    }

    /**
     * Enregistre un login : retourne une nouvelle instance avec {@code lastLoginAt = now}.
     *
     * @throws IllegalArgumentException si {@code now} précède {@link #createdAt()} (anomalie d'horloge)
     */
    public User recordLogin(Instant now) {
        Objects.requireNonNull(now, "now");
        return new User(id, email, displayName, locale, timezone, createdAt, now);
    }

    /** Retourne une nouvelle instance avec le nom mis à jour. */
    public User updateDisplayName(DisplayName newDisplayName) {
        Objects.requireNonNull(newDisplayName, "newDisplayName");
        return new User(id, email, newDisplayName, locale, timezone, createdAt, lastLoginAt);
    }

    /** Retourne une nouvelle instance avec la locale mise à jour. */
    public User updateLocale(Locale newLocale) {
        Objects.requireNonNull(newLocale, "newLocale");
        return new User(id, email, displayName, newLocale, timezone, createdAt, lastLoginAt);
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public Locale locale() {
        return locale;
    }

    public ZoneId timezone() {
        return timezone;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> lastLoginAt() {
        return Optional.ofNullable(lastLoginAt);
    }

    /** Égalité par <b>identité</b> (l'{@link UserId}), conformément au statut d'aggregate. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", displayName=" + displayName + "]";
    }
}
