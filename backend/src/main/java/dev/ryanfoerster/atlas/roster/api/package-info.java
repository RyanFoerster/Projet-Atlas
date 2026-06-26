/**
 * API publique du module roster : interfaces (ports) et events exposés aux autres modules.
 *
 * <p><strong>Named interface Modulith</strong> : par défaut, Spring Modulith n'expose que le package
 * racine d'un module ; un sous-package comme {@code api} reste interne sauf déclaration explicite.
 * {@code @NamedInterface("api")} marque ce package comme frontière publique. Posé au sprint 4 quand
 * Athletics est devenu le premier consommateur de {@code RosterQueryPort} (ADR-027). Les events
 * ({@code api.events}) restent internes tant qu'aucun module ne les consomme (anti-dette) — on les
 * exposera quand un consommateur réel apparaîtra.
 */
@org.springframework.modulith.NamedInterface("api")
package dev.ryanfoerster.atlas.roster.api;
