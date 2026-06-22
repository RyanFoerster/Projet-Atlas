# ADR-001 : Architecture en Modular Monolith avec Spring Modulith

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Atlas est un jeu de coaching fitness avec un domaine métier riche (simulation Fitness-Fatigue, programmation, génétique, compétitions). Plusieurs sous-domaines distincts coexistent : gestion d'athlètes, programmes, séances IRL du joueur, compétitions, analytics.

Trois options d'architecture étaient envisagées :

1. **Monolithe layered classique** (Controller → Service → Repository) — simple mais favorise un couplage fort entre sous-domaines et des services qui grossissent indéfiniment.
2. **Microservices distribués** — overkill pour un projet solo, complexité opérationnelle injustifiée à ce stade.
3. **Modular Monolith** — une seule application déployée mais découpée en modules métier strictement isolés. Permet une évolution future vers du distribué sans tout réécrire.

Le projet vise aussi à démontrer une maîtrise architecturale en vue d'un poste tech lead. L'archi doit être à la fois pragmatique pour un dev solo ET impressionnante en revue de code.

## Décision

Le projet adopte une architecture **Modular Monolith** outillée par **Spring Modulith**. Chaque sous-domaine métier devient un module strictement isolé avec son propre `domain/`, `application/`, `infrastructure/` et une `api/` publique limitée à ce que les autres modules peuvent consommer.

Spring Modulith vérifie automatiquement les règles d'isolation à la compilation et au build :
- Aucun module ne peut importer le `domain/`, `application/` ou `infrastructure/` d'un autre module.
- Seul l'`api/` d'un module est exposé.
- Les dépendances circulaires entre modules font échouer le build.

La communication inter-modules suit le pattern : queries via ports synchrones (interfaces dans `api/`), side effects via events Spring Modulith (event publication registry pour la durabilité et le passage progressif à l'asynchrone).

## Conséquences

**Positives**
- Discipline architecturale forcée par l'outillage, pas par la volonté.
- Migration future vers des microservices possible module par module sans rewrite.
- Très lisible et bien structuré pour des reviews techniques et entretiens.
- Spring Modulith fournit du free tooling (vérification, génération de docs C4, tests par module).

**Négatives**
- Courbe d'apprentissage Spring Modulith (nouveau pour Ryan).
- Plus de boilerplate au démarrage qu'un monolithe layered classique.
- Tentation possible de "tricher" sur l'isolation quand on est pressé — l'outillage automatique mitige ce risque.

**Neutres**
- Spring Modulith est relativement jeune mais mature et activement maintenu par l'équipe Spring.
