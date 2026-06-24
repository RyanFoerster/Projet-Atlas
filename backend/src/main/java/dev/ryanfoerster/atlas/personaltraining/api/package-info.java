/**
 * API publique du module personaltraining : interfaces (ports) et events exposés aux autres modules.
 *
 * <p><strong>Named interface Modulith</strong> : par défaut, Spring Modulith n'expose que les types du
 * package racine d'un module ; un sous-package comme {@code api} reste interne sauf déclaration explicite.
 * {@code @NamedInterface("api")} marque ce package (et {@code api.events}, même nom → fusionnés) comme
 * frontière publique, autorisant les autres modules (ici roster) à importer {@code PersonalTrainingQueryPort}
 * et l'event {@code WorkoutLogged} — et UNIQUEMENT eux. C'est la matérialisation de la règle CLAUDE.md
 * « les modules externes ne peuvent importer que de api/ ».
 */
@org.springframework.modulith.NamedInterface("api")
package dev.ryanfoerster.atlas.personaltraining.api;
