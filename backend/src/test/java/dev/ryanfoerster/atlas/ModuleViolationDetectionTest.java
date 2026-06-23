package dev.ryanfoerster.atlas;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Preuve <strong>active</strong> que la vérification d'isolation Spring Modulith détecte bien une
 * violation — et ne fait pas que passer faute de violation à détecter.
 *
 * <p>{@link AtlasApplicationModulesTest} prouve le cas vert (l'app réelle respecte l'isolation).
 * Mais un test vert sur une app sans violation ne prouve pas que l'outil <em>détecterait</em> un
 * manquement. Ici on construit une mini-app fixture à deux modules où le module B référence un type
 * <em>interne</em> (non exposé) du module A, et on vérifie que {@code verify()} échoue.
 *
 * <p>La fixture vit dans le package racine {@code modulithfixture} (hors {@code dev.ryanfoerster.atlas})
 * exprès : sinon le scan de l'app réelle l'inclurait comme module et ferait échouer le test
 * d'isolation principal.
 */
class ModuleViolationDetectionTest {

    @Test
    void detects_a_controlled_cross_module_violation() {
        ApplicationModules fixture = ApplicationModules.of("modulithfixture");

        assertThatExceptionOfType(Violations.class)
                .isThrownBy(fixture::verify)
                .withMessageContaining("non-exposed");
    }
}
