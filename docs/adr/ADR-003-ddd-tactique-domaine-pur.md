# ADR-003 : DDD tactique avec domaine pur (zéro dépendance framework)

**Statut** : Révisé sprint 1
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Le cœur métier d'Atlas — la simulation Fitness-Fatigue, le calcul d'adaptations, les politiques d'interférence — est complexe et critique pour la crédibilité du jeu. Cette logique doit être :

- **Testable rapidement et de façon exhaustive** (property-based tests, scénarios de calibration sur 12-16 semaines simulées)
- **Lisible** sans avoir à connaître Spring ou JPA
- **Évolutive** sans casser de tests d'infrastructure à chaque changement
- **Citable scientifiquement** (la JavaDoc des services référencera les sources Banister, Helms, Israetel, etc.)

Deux approches étaient envisagées :

1. **Approche pragmatique** : entités JPA = entités du domaine, annotations Spring partout, services qui mélangent logique métier et orchestration technique.
2. **Approche DDD tactique stricte** : domaine pur isolé, entités JPA séparées en infrastructure, mapping explicite entre les deux.

## Décision

Le projet adopte **DDD tactique strict avec domaine pur**. Dans chaque module :

- Le package `domain/` ne contient que de la logique métier pure : aggregates, entities du domaine, value objects, domain services stateless, policies.
- **Imports autorisés dans `domain/`** : `java.*`, libs mathématiques/temporelles pures (BigDecimal, Instant, Duration), le `shared/domain/`, et les **libs purement utilitaires** au sens des quatre critères définis dans ADR-014 (pas d'I/O, pas de framework, pas d'effet de bord global, implémente une spec publique).
- **Imports interdits dans `domain/`** : Spring (toutes annotations), JPA / Hibernate (toutes annotations), Jackson, Lombok, toute lib d'infrastructure.
- Les **entités JPA** vivent dans `infrastructure/persistence/`, séparées des entités du domaine.
- Le mapping domain ↔ JPA se fait via **MapStruct** ou des mappers explicites.
- Les **aggregates** sont des classes immutables qui retournent de nouvelles instances après mutation (style fonctionnel).
- Les **domain services** sont stateless et purs : ils prennent un état + une commande, retournent un nouvel état. Pas d'effet de bord.

L'application des side effects (persistence, events) se fait dans la couche `application/`, qui orchestre les appels au domain et à l'infrastructure.

## Conséquences

**Positives**
- Le domaine est testable à 100% sans booter Spring ni démarrer une DB. Les tests unitaires de domaine s'exécutent en millisecondes.
- Les property-based tests (jqwik) et les scénarios de calibration deviennent triviaux à écrire.
- Le domaine est lisible comme un livre de game design / sport science, pas comme du code Spring.
- L'évolution du modèle Fitness-Fatigue se fait sans toucher à l'infrastructure.
- Démonstration claire de maîtrise DDD en revue de code et entretien.

**Négatives**
- Boilerplate supplémentaire : duplication entre entités du domaine et entités JPA, code de mapping.
- Courbe d'apprentissage pour Ryan (premier projet DDD).
- Tentation de prendre des raccourcis sous la pression du temps — discipline à tenir.

**Neutres**
- L'approche pure fonctionnelle (immutabilité, services stateless) demande un mindset différent du Java OO classique. Bonne occasion d'apprentissage.

## Révisions

- **Sprint 1** — Précision de la liste des imports autorisés dans `domain/` : ajout des **libs purement utilitaires** (au sens des quatre critères d'ADR-014). La décision de fond est inchangée — le domaine reste sans framework et sans I/O ; on assouplit uniquement l'absolutisme « zéro dépendance externe » pour admettre des fonctions pures implémentant une spec publique (ex. génération d'UUID v7 RFC 9562 via `uuid-creator`, futures libs mathématiques du modèle Banister). Voir ADR-014.
