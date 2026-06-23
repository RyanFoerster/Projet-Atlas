package dev.ryanfoerster.atlas.shared.domain;

import java.util.Objects;

/**
 * Un 1RM (charge maximale sur une répétition). Value object du kernel partagé.
 *
 * <p>Précise s'il est {@link Source#MEASURED} (testé en vrai) ou {@link Source#ESTIMATED}
 * (calculé par formule Epley/Brzycki) : la confiance accordée varie selon la source. Au Sprint 2,
 * les 1RM saisis par le joueur pour son athlète miroir sont {@code MEASURED}.
 */
public record OneRepMax(Weight weight, Source source) {

    public enum Source {
        MEASURED,
        ESTIMATED
    }

    public OneRepMax {
        Objects.requireNonNull(weight, "weight");
        Objects.requireNonNull(source, "source");
    }

    public static OneRepMax measured(Weight weight) {
        return new OneRepMax(weight, Source.MEASURED);
    }

    public static OneRepMax estimated(Weight weight) {
        return new OneRepMax(weight, Source.ESTIMATED);
    }
}
