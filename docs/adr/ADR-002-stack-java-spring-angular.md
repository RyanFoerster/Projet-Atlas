# ADR-002 : Stack technique Java 25 + Spring Boot 4.1 + Angular 22

**Statut** : Révisé sprint 0
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

## Révisions

### Révision sprint 0 — corrections de versions (vérifiées sur Maven Central)

Au moment d'écrire le `pom.xml`, plusieurs versions pinées dans la décision initiale se sont révélées incompatibles avec Spring Boot 4.1 (qui impose, via son BOM `spring-boot-dependencies`, des versions plus récentes). Les versions ont été vérifiées comme **réellement publiées et compatibles** sur Maven Central. Corrections appliquées :

| Dépendance | Version initiale (erronée) | Version retenue | Raison |
|------------|----------------------------|-----------------|--------|
| Spring Modulith | « dernière compatible » (non figée) | **2.1.0** | La ligne 2.1 est alignée sur Boot 4.1 (la 2.0 vise Boot 4.0, la 1.x vise Boot 3). |
| springdoc-openapi | 2.7.x | **3.0.3** | La ligne 2.x ne supporte que Spring Framework 6 / Boot 3. La ligne **3.0** est celle qui cible Spring Framework 7 / Boot 4. |
| Flyway | 11.x | **12.4.0** | Version gérée par le BOM Boot 4.1. Pinner 11.x risquait l'incompatibilité. |
| Hibernate | 7.2.x | **7.4.1.Final** | Version gérée par le BOM Boot 4.1. |
| Testcontainers | 1.20.x | **2.0.5** | Version gérée par le BOM Boot 4.1 (saut majeur 1.x → 2.x). |
| PostgreSQL (driver JDBC) | non précisée | **42.7.11** | Version gérée par le BOM Boot 4.1. |
| MapStruct | 1.6.x | **1.6.3** | Dernier patch de la ligne 1.6 (conforme). |
| jqwik | présent dès sprint 0 | **différé au sprint 4** | Bâti sur l'ancien JUnit Platform 1.x ; compat avec JUnit Platform 6 (amené par Boot 4.1) à revérifier. Non utilisé avant les tests de domaine du sprint 4. |

La **stratégie générale** de gestion de ces versions (hériter du BOM vs pinner explicitement, et figer les versions héritées dans `<properties>` pour la traçabilité) est formalisée dans **ADR-009**.
