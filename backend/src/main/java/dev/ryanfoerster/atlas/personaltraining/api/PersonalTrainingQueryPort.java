package dev.ryanfoerster.atlas.personaltraining.api;

import dev.ryanfoerster.atlas.shared.domain.UserId;

/**
 * Port de query <strong>synchrone</strong> exposé par PersonalTraining aux autres modules. C'est l'autre
 * moitié de la communication inter-module Modulith : les <em>events</em> portent les side-effects, les
 * <em>ports</em> répondent aux queries.
 *
 * <p>Au sprint 3, sert l'option D (ADR-025) : le nombre de séances d'un Player a sa source de vérité dans
 * PersonalTraining (pas dupliqué dans Roster). Roster l'interroge à l'affichage de la fiche du miroir.
 */
public interface PersonalTrainingQueryPort {

    /** Nombre de séances loggées par ce Player (source de vérité du compteur). */
    long countSessionsFor(UserId owner);
}
