# ADR-002 : Stack technique Java 25 + Spring Boot 4.1 + Angular 22

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Le choix du stack technique doit équilibrer plusieurs critères :

- **Demandabilité sur le marché belge** : le projet sert aussi de vitrine CV pour une transition vers un poste salarié avec ambition tech lead. Java/Spring et Angular dominent le marché enterprise en Wallonie.
- **Modernité** : démontrer la maîtrise des versions actuelles, pas du Java 8 / Angular 14 legacy.
- **Maturité** : les versions choisies doivent être stables et supportées.
- **Cohérence interne** : compatibilité entre toutes les briques.

Au moment de la décision (juin 2026), les dernières versions stables sont :
- Java 25 LTS (septembre 2025), supporté Premier jusqu'en 2030
- Spring Boot 4.1.0 (juin 2026), bâti sur Spring Framework 7.0.8
- Angular 22 (juin 2026), signal-first, zoneless par défaut, Vitest par défaut

## Décision

Le projet utilise les versions suivantes, pinées dans le `pom.xml` et `package.json` :

**Backend**
- **Java 25 LTS** comme runtime cible
- **Spring Boot 4.1.x** comme framework applicatif
- **Spring Framework 7.0.x** (transitive)
- **Spring Security 7.0.x**
- **Spring Data JPA + Hibernate 7.2.x**
- **Spring Modulith** (dernière version compatible Spring Boot 4.1)
- **PostgreSQL 17** comme base de données
- **Flyway 11.x** pour les migrations
- **MapStruct 1.6.x**
- **Maven** comme build tool

**Frontend**
- **Angular 22** avec mode signal-first, zoneless, Selectorless Components
- **TypeScript 5.9.x**
- **Tailwind CSS** pour le styling
- **Vitest** comme test runner (par défaut depuis Angular 21)

**Pas d'Angular Material** : le projet utilise un design custom avec Tailwind pour se différencier visuellement.

## Conséquences

**Positives**
- Stack ultra-moderne, parfaitement aligné avec ce qui est demandé en 2026 sur le marché enterprise belge.
- Java 25 LTS supporté jusqu'en 2030 minimum, pas de risque d'obsolescence court terme.
- Spring Boot 4.1 et Angular 22 partagent une philosophie de "consolidation" — beaucoup de features expérimentales deviennent stables, réduisant les risques de breaking changes futurs.
- Records, sealed classes, pattern matching de Java 21+/25 réduisent significativement la verbosité.
- Angular zoneless + signals modernisent radicalement la DX par rapport à Angular 14-16.

**Négatives**
- Spring Boot 4.1.0 est récent (juin 2026). Certaines libs tierces peuvent encore avoir du retard sur les BOMs. Mitigation : fallback ponctuel sur Spring Boot 3.5 si une lib critique bloque.
- Angular 22 idem (juin 2026). Mitigation : composants custom plutôt que dépendance à des libs externes potentiellement en retard.
- Ryan doit s'approprier les évolutions Java/Spring des 3 dernières années (TDD pédagogique, ressources, lectures complémentaires).

**Neutres**
- Choix de Maven plutôt que Gradle : préférence enterprise, plus de stabilité, écosystème mieux établi. Gradle aurait été défendable aussi.
