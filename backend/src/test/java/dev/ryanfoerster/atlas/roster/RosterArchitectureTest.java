package dev.ryanfoerster.atlas.roster;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de RÉUTILISATION DU TEMPLATE (objectif central du sprint 2) : vérifie par l'architecture que le
 * module roster respecte le même squelette hexagonal + DDD que identity, plutôt que de l'attester à la
 * main. ArchUnit est déjà au classpath de test (transitif via Spring Modulith) — aucune dépendance ajoutée.
 */
class RosterArchitectureTest {

    private static final String ROOT = "dev.ryanfoerster.atlas.roster";

    private final JavaClasses rosterClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void has_the_four_hexagonal_layers() {
        Set<String> layers = new HashSet<>();
        for (JavaClass clazz : rosterClasses) {
            String suffix = clazz.getPackageName().substring(ROOT.length());
            if (suffix.startsWith(".")) {
                layers.add(suffix.split("\\.")[1]);
            }
        }
        assertThat(layers).contains("api", "domain", "application", "infrastructure");
    }

    @Test
    void domain_is_pure_no_spring_jpa_jackson_hibernate() {
        noClasses().that().resideInAPackage(ROOT + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..",
                        "com.fasterxml.jackson..", "org.hibernate..")
                .because("le domaine doit rester pur (ADR-003)")
                .check(rosterClasses);
    }

    @Test
    void persistence_uses_manual_mappers_not_mapstruct() {
        noClasses().that().resideInAPackage(ROOT + "..")
                .should().dependOnClassesThat().resideInAPackage("org.mapstruct..")
                .because("on fait des mappers manuels (ADR-015), pas de MapStruct")
                .check(rosterClasses);
    }

    @Test
    void public_events_are_records_in_api_events() {
        classes().that().resideInAPackage(ROOT + ".api.events..")
                .should().beRecords()
                .because("les events publics sont des records immuables")
                .check(rosterClasses);
    }

    /**
     * Preuve d'isolation event-driven (sprint 3) : Roster consomme PersonalTraining UNIQUEMENT via son
     * API publique ({@code personaltraining.api..} — l'event {@code WorkoutLogged} et le
     * {@code PersonalTrainingQueryPort}), jamais via son domaine/application/infrastructure internes.
     * C'est ce qui prouve que la communication inter-module préserve l'isolation (Spring Modulith
     * l'impose aussi globalement ; on l'explicite ici pour ce couplage précis).
     */
    @Test
    void roster_depends_on_personaltraining_only_through_its_api() {
        noClasses().that().resideInAPackage(ROOT + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "dev.ryanfoerster.atlas.personaltraining.domain..",
                        "dev.ryanfoerster.atlas.personaltraining.application..",
                        "dev.ryanfoerster.atlas.personaltraining.infrastructure..")
                .because("Roster ne doit dépendre de PersonalTraining que via personaltraining.api.. "
                        + "(events + query port), jamais de son interne (isolation Modulith)")
                .check(rosterClasses);
    }
}
