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

    /**
     * Le profil de <strong>charge</strong> d'un athlète (poids de corps + 1RM par pattern), pour calculer le
     * %1RM d'une série (sprint 6, ADR-034). {@link Optional#empty()} si l'athlète n'existe pas.
     *
     * <p><strong>Contrairement à {@link #findGeneticProfile}, à relire FRAÎCHE à chaque séance</strong> : les
     * 1RM sont <em>mutables</em> (ils progressent dès la Couche 3 du sprint 6), donc on ne dénormalise pas —
     * sinon le %1RM se calculerait sur un 1RM périmé. « Ne pas dénormaliser ce qui change. »
     */
    Optional<AthleteLoadProfile> findLoadProfile(AthleteId athleteId);

    /**
     * Le <strong>plafond génétique</strong> de force d'un athlète, par pattern ({@code bodyweight ×
     * ratio_élite × strengthAffinity}), pour borner la progression structurelle (Couche 3, ADR-033).
     * {@link Optional#empty()} si l'athlète n'existe pas. Roster calcule le plafond (il possède les standards
     * de force et la génétique), Athletics le lit (T3) — sans jamais connaître les ratios élite.
     *
     * <p><strong>Immutable → dénormalisable</strong> (à l'opposé de {@link #findLoadProfile}) : la génétique
     * et le poids de corps ne bougent pas, donc Athletics le lit <em>une fois à l'initialisation</em> d'un
     * pattern, pas à chaque séance. On dénormalise ce qui ne change pas, on lit frais ce qui change (T5).
     */
    Optional<AthleteStrengthCeiling> findStrengthCeiling(AthleteId athleteId);
}
