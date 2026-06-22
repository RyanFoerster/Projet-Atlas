package dev.ryanfoerster.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application Atlas.
 *
 * <p>Atlas est un modular monolith (ADR-001) : chaque sous-package direct de
 * {@code dev.ryanfoerster.atlas} est un module métier isolé, détecté et vérifié
 * par Spring Modulith. Cette classe sert aussi de racine au scan de modules dans
 * les tests d'isolation ({@code ApplicationModules.of(AtlasApplication.class)}).
 */
@SpringBootApplication
public class AtlasApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtlasApplication.class, args);
    }
}
