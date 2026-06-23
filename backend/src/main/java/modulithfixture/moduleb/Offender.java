package modulithfixture.moduleb;

import modulithfixture.modulea.internal.InternalThing;

/**
 * Fixture de test : viole délibérément l'isolation en référençant un type INTERNE du module A
 * ({@link InternalThing}, dans un sous-package non exposé) au lieu de son API publique.
 * Spring Modulith doit détecter cette dépendance interdite.
 */
public class Offender {

    @SuppressWarnings("unused")
    private InternalThing illegalDependency;
}
