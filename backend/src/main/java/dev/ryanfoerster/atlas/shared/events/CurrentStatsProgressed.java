package dev.ryanfoerster.atlas.shared.events;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.UUID;

/**
 * Event public : la progression structurelle d'un athlète a fait <strong>monter son 1RM</strong> sur un
 * pattern de force (Couche 3, ADR-033). <strong>Publié par Athletics</strong> quand le 1RM <em>mérité</em>
 * par la charge chronique accumulée dépasse le 1RM courant (le cliquet ne propage qu'une hausse),
 * <strong>consommé par Roster</strong> pour matérialiser le nouveau 1RM dans les {@code CurrentStats} de
 * l'athlète (la carte).
 *
 * <h2>Pourquoi ce contrat vit dans le kernel {@code shared} et non dans {@code athletics.api.events}</h2>
 * Athletics dépend <em>déjà</em> de Roster (port query synchrone {@code RosterQueryPort}, ADR-027 : il tire
 * le 1RM/bodyweight/génétique frais). Si cet event vivait dans {@code athletics.api.events}, le handler
 * Roster qui le consomme créerait l'arête {@code roster → athletics} et donc un <strong>cycle</strong>
 * {@code athletics ↔ roster} (interdit, Spring Modulith). On descend donc le <strong>contrat</strong> (ce
 * record, types {@code shared} uniquement) dans le kernel OPEN : athletics publie vers {@code shared},
 * roster consomme depuis {@code shared}, plus aucune arête directe entre eux. <strong>Seul le contrat
 * descend</strong> — la logique métier (calculer la progression, décider d'émettre via le cliquet) reste
 * chez le producteur, Athletics. Écart mineur et motivé à ADR-024 (event normalement dans le module
 * producteur), réservé aux events entre modules mutuellement dépendants (ADR-032).
 *
 * <p>Types <strong>primitifs / shared</strong> uniquement (UUID, {@link MovementPattern} du kernel,
 * {@code double}, {@code Instant}) — aucun type de domaine interne (ADR-024). L'id athlète est un
 * {@link UUID} nu (convention des events).
 *
 * @param athleteId      l'athlète dont le 1RM progresse
 * @param pattern        le pattern de force concerné (un des grands lifts suivis dans CurrentStats)
 * @param newOneRepMaxKg le nouveau 1RM mérité, en kilogrammes (strictement supérieur au précédent — cliquet)
 * @param progressedAt   l'instant de la séance qui a déclenché la progression
 */
public record CurrentStatsProgressed(UUID athleteId, MovementPattern pattern, double newOneRepMaxKg,
                                     Instant progressedAt) {
}
