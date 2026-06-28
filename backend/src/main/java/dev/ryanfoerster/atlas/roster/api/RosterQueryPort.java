package dev.ryanfoerster.atlas.roster.api;

import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.util.Optional;

/**
 * Port de query <strong>synchrone</strong> exposé par Roster aux autres modules — le <em>premier</em> de
 * Roster (jusqu'au sprint 3, Roster ne publiait que des events). C'est l'autre moitié de la communication
 * inter-module Modulith : les <em>events</em> portent les side-effects, les <em>ports</em> répondent aux
 * queries.
 *
 * <p>Introduit au sprint 4 pour Athletics (ADR-027) : l'event {@code WorkoutLogged} ne porte que
 * l'{@code ownerId} (le Player), pas l'{@code AthleteId}. Pour appliquer le stimulus à la
 * <strong>condition de l'athlète miroir</strong> (clé {@code AthleteId}, option 3a), Athletics résout le
 * miroir du Player via ce port, puis applique l'évolution déclenchée par l'event. Events pour les
 * side-effects, ports pour les queries — symétrique de {@code PersonalTrainingQueryPort} (sprint 3).
 */
public interface RosterQueryPort {

    /** L'athlète miroir du Player, s'il en a créé un. {@link Optional#empty()} sinon. */
    Optional<AthleteId> findMirrorAthleteId(UserId owner);

    /**
     * Le profil génétique d'un athlète (clé {@link AthleteId}), pour individualiser son modèle d'adaptation
     * (sprint 5, ADR-031). {@link Optional#empty()} si l'athlète n'existe pas. La {@code Genetics} étant
     * <strong>immutable</strong>, Athletics le résout une seule fois (à la création de la condition) et
     * dénormalise les modificateurs dérivés — pas d'appel à chaque séance.
     */
    Optional<GeneticProfile> findGeneticProfile(AthleteId athleteId);
}
