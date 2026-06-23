/**
 * <strong>Fixture de test d'architecture — PAS du code applicatif.</strong>
 *
 * <p>Mini-application à deux modules ({@code modulea}, {@code moduleb}) contenant une violation
 * d'isolation délibérée, utilisée par {@code ModuleViolationDetectionTest} pour prouver que Spring
 * Modulith détecte bien les manquements.
 *
 * <p>Pourquoi en sources <em>main</em> et non <em>test</em> ? Spring Modulith exclut les classes de
 * test de son scan ({@code DO_NOT_INCLUDE_TESTS}) : depuis {@code src/test} elles seraient
 * invisibles. Et pourquoi hors du package {@code dev.ryanfoerster.atlas} ? Pour que le scan de
 * l'application réelle ({@code AtlasApplicationModulesTest}) ne l'inclue pas comme module et
 * n'échoue pas sur la violation volontaire. Ces classes sont inertes (aucun bean, jamais scannées
 * par {@code @SpringBootApplication}).
 */
package modulithfixture;
