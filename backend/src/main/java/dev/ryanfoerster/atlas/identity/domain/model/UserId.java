package dev.ryanfoerster.atlas.identity.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Identifiant d'un {@link User}, encapsulant un {@link UUID} version 7 (RFC 9562).
 *
 * <p>Value object : immutable (record), comparable par valeur, auto-validant (impossible
 * d'en construire un autour d'un {@code null}). On ne manipule jamais un {@code UUID} nu
 * dans le domaine — le wrapper rend le type explicite (on ne peut pas passer un {@code AthleteId}
 * là où un {@code UserId} est attendu) et porte le sens métier.
 *
 * <p><strong>Pourquoi UUID v7 ?</strong> Le v7 est ordonné dans le temps (préfixe =
 * timestamp Unix en millisecondes). Inséré en clé primaire PostgreSQL, il produit des
 * index B-tree quasi séquentiels — bien moins de fragmentation et de réécritures de pages
 * qu'un v4 purement aléatoire, tout en gardant l'unicité décentralisée (généré côté
 * application, pas par la base). Détails et justification dans ADR-014.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId ne peut pas encapsuler un UUID null");
        }
    }

    /**
     * Génère un nouvel identifiant unique (UUID v7, ordonné dans le temps).
     */
    public static UserId generate() {
        return new UserId(UuidCreator.getTimeOrderedEpoch());
    }

    /**
     * Reconstruit un {@code UserId} à partir de sa représentation textuelle.
     *
     * @throws IllegalArgumentException si {@code raw} est null ou n'est pas un UUID valide
     */
    public static UserId from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("UserId ne peut pas être construit depuis une chaîne null");
        }
        try {
            return new UserId(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UserId invalide : « " + raw + " » n'est pas un UUID", e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
