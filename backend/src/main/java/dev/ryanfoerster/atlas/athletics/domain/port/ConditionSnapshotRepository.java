package dev.ryanfoerster.atlas.athletics.domain.port;

import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;

import java.util.List;

/**
 * Port secondaire de persistance des {@link ConditionSnapshot} (append-only). Le domaine définit
 * l'interface ; l'adapter JPA l'implémente. Les snapshots alimenteront les courbes du sprint 7.
 */
public interface ConditionSnapshotRepository {

    ConditionSnapshot save(ConditionSnapshot snapshot);

    /** Tous les snapshots d'un athlète, du plus ancien au plus récent (ordre chronologique des courbes). */
    List<ConditionSnapshot> findByAthleteId(AthleteId athleteId);
}
