package dev.ryanfoerster.atlas;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Vérifie l'isolation des modules (ADR-001).
 *
 * <p>Spring Modulith analyse statiquement (via ArchUnit) la structure de packages
 * à partir de {@link AtlasApplication} : chaque sous-package direct est un module.
 * {@link ApplicationModules#verify()} échoue le build si une règle d'isolation est
 * violée — dépendance vers le {@code domain}/{@code application}/{@code infrastructure}
 * d'un autre module, ou dépendance circulaire entre modules.
 *
 * <p>Ce test tourne à chaque build : c'est le garde-fou qui transforme la discipline
 * architecturale en contrainte outillée plutôt qu'en bonne volonté.
 */
class AtlasApplicationModulesTest {

    static final ApplicationModules modules = ApplicationModules.of(AtlasApplication.class);

    @Test
    void verifiesModuleIsolation() {
        modules.verify();
    }

    /**
     * Génère la documentation C4 des modules (diagrammes PlantUML + module canvases)
     * dans {@code target/spring-modulith-docs/} (ADR-001). Donne un artefact tangible
     * de la structure modulaire, régénéré à chaque build.
     */
    @Test
    void generatesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
