# Architecture Decision Records

Ce dossier contient les **Architecture Decision Records** (ADRs) du projet Atlas. Chaque ADR documente une décision structurante : son contexte, les alternatives envisagées, la décision retenue, et ses conséquences.

## Format

Les ADRs suivent le format Michael Nygard, condensé pour rester lisibles (~1 page chacun) :
- **Contexte** : pourquoi cette décision est nécessaire
- **Décision** : ce qui est retenu
- **Conséquences** : positives, négatives, neutres

Voir `ADR-template.md` pour le squelette à dupliquer.

## Liste des ADRs

| # | Titre | Statut |
|---|-------|--------|
| 001 | Architecture en Modular Monolith avec Spring Modulith | Accepté |
| 002 | Stack technique Java 25 + Spring Boot 4.1 + Angular 22 | Révisé sprint 0 |
| 003 | DDD tactique avec domaine pur (zéro dépendance framework) | Révisé sprint 1 |
| 004 | Modèle Fitness-Fatigue de Banister par groupe musculaire | Accepté |
| 005 | Lien IRL ↔ jeu via athlète miroir et déblocage de programmes | Accepté |
| 006 | Lazy compute pour la game loop idle | Accepté |
| 007 | Module Insights séparé en CQRS pour le read-side analytics | Accepté |
| 008 | PostgreSQL + Flyway + Testcontainers pour le data layer | Accepté |
| 009 | Stratégie de gestion des versions de dépendances | Accepté |
| 010 | Structure monorepo symétrique (backend/ + frontend/) | Accepté |
| 014 | UUID v7 pour les aggregates + libs purement utilitaires autorisées dans le domaine | Accepté |

## Règles

- Toute décision structurante (architecture, choix de lib majeure, pattern transversal) doit avoir son ADR.
- Numérotation séquentielle, jamais réutilisée.
- L'ADR est rédigé **avant** ou **pendant** l'implémentation, jamais après coup.

## Règles de révision

Comment faire évoluer un ADR existant dépend de la nature du changement :

- **La décision de fond change** (on revient sur le choix, on adopte une autre approche) → on crée un **nouvel ADR** qui *supersede* l'ancien. L'original passe au statut **« Remplacé par ADR-XXX »** et n'est plus modifié sur le fond ; le nouvel ADR explique pourquoi la décision change.
- **Correction factuelle ou de sous-version** (numéros de version précisés, typos, clarification de wording, détail technique affiné — sans remettre en cause la décision de fond) → **révision en place** : statut **« Révisé \<date/sprint\> »** et ajout d'une section **« Révisions »** datée en bas de l'ADR, qui liste ce qui a été corrigé et pourquoi.
- **En cas de doute entre les deux** → créer un **nouvel ADR**. On préfère la traçabilité à la concision : un ADR de trop est moins coûteux qu'une décision dont l'historique est perdu.

*Exemple : ADR-002 a été « Révisé sprint 0 » pour corriger des numéros de version de dépendances devenus incompatibles avec Spring Boot 4.1 — la décision de fond (Java 25 / Boot 4.1 / Angular 22) restant inchangée.*
