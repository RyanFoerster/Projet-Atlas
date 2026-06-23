package dev.ryanfoerster.atlas.shared.domain;

/**
 * Patterns moteurs principaux du lifting (cf. glossaire). Value object du kernel partagé :
 * transverse (roster, athletics, programming…) et fondamental (ADR-017).
 */
public enum MovementPattern {
    SQUAT,
    BENCH_PRESS,
    DEADLIFT,
    OVERHEAD_PRESS,
    ROW,
    CHIN_UP
}
