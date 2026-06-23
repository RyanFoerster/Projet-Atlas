# Notes brutes pour le mini-cours `sprint-02-roster-and-procgen.md`

> Capturé au fil de l'eau. Mini-cours plus court que le sprint 1 (concepts DDD posés, on capitalise).

## 1. Réutilisation d'un template DDD (l'objectif du sprint)
- Le module Roster a copié la structure Identity sans réinventer : value objects auto-validants,
  aggregate immutable (égalité par identité), `reconstitute()`, ports/adapters, **mappers manuels**,
  hiérarchie `DomainException`, events en types primitifs.
- **Cas emblématique** : `ScoutedCandidate` a réutilisé **consciemment le pattern `MagicLink`** (objet
  temporaire à TTL, consommé une seule fois : `isExpired`/`canBeConsumed`/`consume`). On n'a pas inventé
  un nouveau pattern — on a reconnu un pattern existant et on l'a appliqué. C'est exactement « capitaliser
  un template » vs « réinventer à chaque module ».
- Ce qui se COPIE (structure, patterns), ce qui s'ADAPTE (aggregate avec collection d'entités, VO à Maps
  avec copie défensive, JSONB).

## 2. Génération procédurale déterministe
- Hasard TOUJOURS injecté (seed/Random en paramètre), jamais `new Random()` caché → « même seed → même
  résultat », sans quoi on ne peut pas tester les invariants ni la distribution.
- Test de distribution 10 000 tirages seedés (déterministe, pas flaky) vs cible à ±0.5%.

## 3. Rareté par spécialisation (ADR-020)
- Différenciation par NOMBRE d'axes spécialisés ET magnitude (pas magnitude seule, sinon frontière floue).
- Specialist = 2 axes francs, Prodigy = 1 axe exceptionnel + base plus variable (génie déséquilibré).

## 4. Aggregate avec collection d'entités (vs aggregate « simple »)
- Roster contient `List<Athlete>` → invariant AU NIVEAU AGGREGATE (un seul miroir), Athlete sans factory
  publique (orchestré par Roster). Diffère de `User` (pas d'entity enfant).
- Persistence `@OneToMany` cascade + orphanRemoval, adapter `@Transactional` (collection lazy au mapping).

## 5. JSONB sans polluer le domaine (extension ADR-015)
- 3 couches : domaine pur / DTO infra `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 7 natif, zéro dép) /
  mapper manuel qui fait AUSSI la (dé)sérialisation. Prouvé : `jsonb_typeof = 'object'`, round-trip exact.

## 6. REST design : 200 vs 201
- `POST /scout` → **200** : proposition non adressable (pas d'URL `GET /candidate/:id`), un calcul, pas
  une ressource créée côté API.
- `POST /mirror` et `POST /recruit` → **201** : une ressource (l'athlète) est créée et adressable via
  `GET /api/roster/athletes/:id`. Distinction REST orthodoxe.
- Bonus contrat : `409` (miroir déjà créé), `404` (candidat expiré/recruté, athlète hors roster),
  `400` (validation VO), `401` (pas de session) — pilotés par un `@RestControllerAdvice` à précédence haute.

## 7. Décomposer une responsabilité technique en plusieurs domain services (SRP)
- `RarityRoller` (tire le tier) + `AthleteGenerator` (génère selon le tier) : deux services à
  responsabilité unique plutôt qu'un `generateCandidate(rarityRoll: double)` monolithique. Le use case
  compose. Bel exemple de Single Responsibility appliqué aux domain services.
