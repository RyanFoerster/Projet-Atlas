package dev.ryanfoerster.atlas.athletics.domain.port;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;

import java.util.Optional;

/**
 * Port secondaire de persistance de l'{@link AthleteCondition} (hexagonal, ADR-003). Le domaine définit
 * l'interface ; l'adapter JPA l'implémente en infrastructure. Aggregate autonome, clé par {@link AthleteId}.
 */
public interface AthleteConditionRepository {

    AthleteCondition save(AthleteCondition condition);

    Optional<AthleteCondition> findByAthleteId(AthleteId athleteId);
}
