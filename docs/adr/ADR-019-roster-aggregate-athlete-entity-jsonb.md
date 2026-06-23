# ADR-019 : Aggregate Roster, Athlete entity interne, persistance JSONB, et consolidation du kernel

**Statut** : Accepté
**Date** : Sprint 2
**Décideur** : Ryan Foerster

## Contexte

Le module roster introduit un aggregate (Roster) contenant une collection d'entities (Athlete), avec des
structures de données complexes (Genetics, CurrentStats). Trois questions structurantes se posent, plus
une dette du kernel révélée en cours de route.

## Décision

### 1. Roster est le seul aggregate root ; Athlete est une entity interne

Un `Athlete` n'a pas de sens métier hors d'un `Roster` (DDD orthodoxe, cf. Vernon *IDDD* sur les
aggregates avec entities internes). Conséquences :

- **Pas de `AthleteRepository`** : on charge le `Roster` (la racine) et on navigue vers ses athlètes.
- **Pas de factory de création publique sur `Athlete`** : c'est `Roster` qui orchestre
  (`roster.addMirror(...)`, `roster.recruit(...)`). `Athlete.reconstitute` reste public (réhydratation).
- Les invariants critiques (un seul miroir ; taille max et éligibilité futures) se protègent **au niveau
  Roster**.
- `GET /api/roster/athletes/:id` charge le roster du Player authentifié et y cherche l'athlète → 404 s'il
  n'y est pas (sécurité naturelle).
- Persistance : `@OneToMany` (cascade ALL + orphanRemoval) de `RosterJpaEntity` vers `AthleteJpaEntity` ;
  `AthleteMapper` est un **helper interne** du `RosterMapper`, pas un composant indépendant.

Le coût de chargement (roster entier) est négligeable au MVP (10–50 athlètes). Si optimisation un jour,
on passera par des projections read-side via Insights (CQRS, ADR-007).

### 2. Genetics / CurrentStats stockés en JSONB

Structures complexes (Maps), sans besoin de query SQL dessus au Sprint 2. **JSONB** plutôt que 20
colonnes plates : évolutif, lisible. On utilise le **support JSON natif d'Hibernate 7.4**
(`@JdbcTypeCode(SqlTypes.JSON)` + `JacksonJsonFormatMapper` intégré) — **aucune dépendance externe**
(pas de hypersistence-utils).

**Pureté du domaine préservée (extension d'ADR-015)** — trois couches :
1. **Domaine pur** : `Genetics`/`CurrentStats` records, zéro Jackson (ADR-003).
2. **DTO d'infrastructure** (`infrastructure/persistence/json/`) annotés implicitement Jackson-sérialisables.
3. **Mapper manuel** : convertit domaine ↔ DTO JSON ; Hibernate fait DTO ↔ jsonb. *Les mappers manuels
   couvrent donc aussi la (dé)sérialisation JSON/JSONB.*

**Vérifié** (Testcontainers) : round-trip Genetics exact (égalité par valeur), et
`jsonb_typeof(genetics) = 'object'` (vrai jsonb natif, pas du text/bytea).

**Stratégie d'évolution du format JSONB** (ignorée au Sprint 2, mais tracée) : à définir au moment du
**premier breaking change**, probablement via une **migration Flyway transformant les blobs existants**.
On a au moins acté qu'on y a pensé.

### 3. Consolidation du kernel `shared` (dette latente révélée par le multi-module)

`Roster.ownerId` est un `UserId`, qui vivait dans `identity.domain.model` (package interne d'un module
CLOSED) — alors que CLAUDE.md §3 le listait depuis toujours comme VO du kernel. Le mono-module masquait
l'incohérence ; le multi-module l'a révélée (Roster aurait violé l'isolation Modulith). Corrigé en
**S0** : `UserId` déplacé vers `shared.domain`, et les VO transverses du modèle Roster (`Weight`,
`OneRepMax`, `MovementPattern`, `MuscleGroup`) créés dans `shared` — chaque promotion vérifiant les deux
critères d'ADR-017 (transverse 2+ modules ET fondamental). `Height` reste dans `roster.domain` (seul
Roster l'utilise).

### Note — FK inter-module

`rosters.owner_id` a une FK vers `users(id)` (intégrité référentielle au niveau base). C'est un couplage
de schéma entre modules : à garder en tête si un module est un jour extrait vers un service séparé
(ADR-001). Assumé pour le MVP (le DB est partagé de toute façon).

## Conséquences

**Positives**
- Modèle DDD orthodoxe et défendable ; invariants protégés au bon endroit.
- JSONB sans dépendance ni pollution du domaine ; round-trip prouvé.
- Le kernel est aligné sur son intention (CLAUDE.md §3) ; l'isolation Modulith tient.

**Négatives**
- `@OneToMany` + merge d'un graphe détaché : zone JPA délicate (mitigée par les tests d'intégration et la
  taille modeste des rosters).
- JSONB = pas de contrainte SQL sur le contenu (la validation vit dans les value objects du domaine).

**Neutres**
- La stratégie d'évolution JSONB est repoussée mais documentée.
