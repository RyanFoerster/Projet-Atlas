package dev.ryanfoerster.atlas.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Lien magique à usage unique : un secret ({@link MagicLinkToken}) envoyé par email,
 * valable un court instant, qui authentifie son porteur quand il le consomme.
 *
 * <p><strong>Entity, pas aggregate root.</strong> Le {@code MagicLink} a une identité
 * (son {@code token}) et un cycle de vie propre (émis → consommé/expiré), donc égalité
 * <b>par identité</b> comme {@link User}. Mais il n'est pas racine d'aggregate : il vit
 * dans le contexte de l'authentification d'un Player. On le manipule néanmoins comme une
 * entity autonome car, au moment de l'émission, le {@link User} peut ne pas exister encore.
 *
 * <p><strong>On stocke {@code userEmail}, pas un {@code UserId}.</strong> Le premier login
 * vaut signup : à l'émission du lien, l'utilisateur n'a peut-être pas encore de compte.
 * On lie donc le lien à l'email saisi ; la résolution (trouver ou créer le User) se fait
 * à la consommation.
 *
 * <p>Immutable et fonctionnel comme l'aggregate : {@link #consume} retourne une nouvelle
 * instance, l'original n'est pas muté.
 */
public final class MagicLink {

    private final MagicLinkToken token;
    private final Email userEmail;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant consumedAt;   // null tant que non consommé
    private final String ipAddress;     // null si non capturé
    private final String userAgent;     // null si non capturé

    private MagicLink(MagicLinkToken token, Email userEmail, Instant createdAt, Instant expiresAt,
                      Instant consumedAt, String ipAddress, String userAgent) {
        this.token = Objects.requireNonNull(token, "token");
        this.userEmail = Objects.requireNonNull(userEmail, "userEmail");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt (" + expiresAt
                    + ") doit être strictement après createdAt (" + createdAt + ")");
        }
        if (consumedAt != null && consumedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("consumedAt ne peut pas précéder createdAt");
        }
        this.consumedAt = consumedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    /**
     * Émet un nouveau lien magique non consommé. {@code expiresAt} est calculé en amont par
     * la {@link dev.ryanfoerster.atlas.identity.domain.service.MagicLinkExpirationPolicy}.
     *
     * @param ipAddress contexte de sécurité optionnel ({@code null} accepté)
     * @param userAgent contexte de sécurité optionnel ({@code null} accepté)
     */
    public static MagicLink issue(MagicLinkToken token, Email userEmail, Instant createdAt,
                                  Instant expiresAt, String ipAddress, String userAgent) {
        return new MagicLink(token, userEmail, createdAt, expiresAt, null, ipAddress, userAgent);
    }

    /** {@code true} si le lien a expiré à l'instant {@code now}. */
    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(expiresAt); // now >= expiresAt
    }

    /** {@code true} si le lien a déjà été consommé. */
    public boolean isConsumed() {
        return consumedAt != null;
    }

    /** {@code true} si le lien peut être consommé à {@code now} : ni expiré, ni déjà consommé. */
    public boolean canBeConsumed(Instant now) {
        return !isExpired(now) && !isConsumed();
    }

    /**
     * Consomme le lien : retourne une nouvelle instance marquée consommée à {@code now}.
     *
     * @throws MagicLinkNotUsableException si le lien est expiré ou déjà consommé (règle métier)
     */
    public MagicLink consume(Instant now) {
        Objects.requireNonNull(now, "now");
        if (isConsumed()) {
            throw new MagicLinkNotUsableException("Ce lien magique a déjà été utilisé");
        }
        if (isExpired(now)) {
            throw new MagicLinkNotUsableException("Ce lien magique a expiré");
        }
        return new MagicLink(token, userEmail, createdAt, expiresAt, now, ipAddress, userAgent);
    }

    public MagicLinkToken token() {
        return token;
    }

    public Email userEmail() {
        return userEmail;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> consumedAt() {
        return Optional.ofNullable(consumedAt);
    }

    public Optional<String> ipAddress() {
        return Optional.ofNullable(ipAddress);
    }

    public Optional<String> userAgent() {
        return Optional.ofNullable(userAgent);
    }

    /** Égalité par identité (le {@link MagicLinkToken}). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MagicLink other)) {
            return false;
        }
        return token.equals(other.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public String toString() {
        return "MagicLink[token=" + token + ", userEmail=" + userEmail
                + ", expiresAt=" + expiresAt + ", consumed=" + isConsumed() + "]";
    }
}
