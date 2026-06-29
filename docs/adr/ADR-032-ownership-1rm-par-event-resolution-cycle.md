# ADR-032 : Ownership du 1RM matérialisé par event, et résolution du cycle athletics↔roster

**Statut** : Accepté (GATE 3b du sprint 6 validé)
**Date** : Sprint 6
**Décideur** : Ryan Foerster

## Contexte

La Couche 3 fait progresser le 1RM structurel (ADR-033 décide la *dynamique* : cible convergente + cliquet).
Reste à décider **qui possède le 1RM matérialisé** et **comment une progression calculée par Athletics
atteint Roster**. Deux faits cadrent la décision :

1. Le 1RM (la « carte » de l'athlète) est dans `CurrentStats`, **possédé par Roster** (aggregate `Athlete`,
   ADR-019). Athletics ne possède pas le 1RM — il possède la *physiologie* (le modèle de Banister, et
   maintenant l'accumulateur chronique).
2. Athletics **dépend déjà de Roster** : le port query synchrone `RosterQueryPort` (ADR-027, sprint 4) — il
   tire le 1RM/bodyweight/génétique **frais** pour calculer stimulus et plafond.

## Décision

### 1. Ownership par event (two-store)

**Athletics possède la dynamique, Roster possède le 1RM matérialisé.** Deux stores qui ne se chevauchent
pas :

- **Athletics** : l'accumulateur chronique par pattern (`StructuralProgress` sur `AthleteCondition`) + le
  calcul du mérité (ADR-033). Quand le mérité dépasse le 1RM courant (cliquet), Athletics **publie**
  `CurrentStatsProgressed(athleteId, pattern, newOneRepMaxKg, progressedAt)`.
- **Roster** : le 1RM matérialisé (`CurrentStats`, source de vérité de la carte). Un handler Modulith
  (`CurrentStatsProgressedHandler`, `@ApplicationModuleListener`) consomme l'event et applique une mutation
  **copy-on-write** : `Roster.progressAthleteStat → Athlete.progressOneRepMax → CurrentStats.with`.

Les deux modules **ne s'écrivent jamais l'un l'autre** : ils communiquent par cet event, exactement comme
`WorkoutLogged` (PersonalTraining → Roster/Athletics) en sens inverse. Side-effect async, durable,
at-least-once via l'event publication registry (ADR-023).

### 2. Le cliquet gardé jusqu'au point de matérialisation (idempotence)

`Athlete.progressOneRepMax` n'applique qu'une **hausse** du 1RM (no-op si la valeur ≤ courante). Conséquence :
rejouer l'event (restart, échec, réordonnancement — la livraison est at-least-once, pas exactly-once) ne peut
**ni faire reculer ni doubler** un 1RM. Le cliquet d'ADR-033 (« le 1RM ne descend jamais ») devient un
invariant de l'aggregate Roster, pas seulement une règle d'émission. Même esprit que l'écrasement monotone de
`TrainingHistory` (ADR-025) : un handler event-driven idempotent par construction.

### 3. Roster calcule le plafond, Athletics le lit (T3)

Le plafond génétique (`bodyweight × ratio_élite × strengthAffinity`) a besoin des **standards de force**, qui
appartiennent à Roster (`StrengthStandards`, extrait de `ProceduralAthleteGenerator` pour être source unique).
Roster l'expose via un nouveau port `findStrengthCeiling(athleteId) → AthleteStrengthCeiling` ; Athletics le
lit sans **jamais** connaître les ratios élite. Cycle de vie distinct de `findLoadProfile` : le plafond est
**immutable → dénormalisable** (lu une fois à l'init d'un pattern), le 1RM courant est **mutable → lu frais**
(chaque séance). On dénormalise ce qui ne change pas, on lit frais ce qui change (T5, ADR-034 §5).

### 4. Résolution du cycle : le contrat d'event descend dans `shared`

**Le problème.** Athletics dépend de Roster (port query, §contexte). Si `CurrentStatsProgressed` vivait dans
`athletics.api.events`, le handler Roster qui le consomme créerait l'arête `roster → athletics` → **cycle
`athletics ↔ roster`** (interdit par Spring Modulith, vérifié au build par `AtlasApplicationModulesTest`).

**La résolution.** Quand deux modules se querient mutuellement, le **contrat** d'event partagé descend dans le
kernel `shared` (module OPEN, ADR-017, qui prévoit déjà « base d'events » dans `shared/events`). Athletics
*publie* vers `shared`, Roster *consomme* depuis `shared` — plus aucune arête directe entre eux, cycle cassé.

**Seul le contrat descend, la logique reste chez le producteur.** `CurrentStatsProgressed` est un record de
types `shared` uniquement (UUID, `MovementPattern`, double, Instant). La connaissance métier — calculer la
progression, décider d'émettre via le cliquet — **reste dans Athletics**. Le kernel reste des
**contrats/types** partagés, jamais de la logique (discipline du kernel minimal, ADR-017).

C'est un **écart mineur et motivé à ADR-024** (un event vit normalement dans le `api/events` du module
producteur). L'exception est cadrée : *elle ne vaut que pour les events entre modules mutuellement
dépendants*. Un event sans dépendance inverse (ex. `WorkoutLogged`) reste dans son module.

### 5. Option B (port de commande synchrone) rejetée — un piège, pas un second choix

L'alternative « Athletics pousse via un `RosterCommandPort.progressStat(...)` synchrone » résout le cycle au
sens littéral (sens athletics → roster, déjà existant) mais **trahit l'architecture** :

- Faire progresser un 1RM est un **side-effect** (mutation), pas une query → doit être un **event**
  (CLAUDE.md §3.6), pas un appel de port.
- Concrètement : un handler **asynchrone** (réagissant à `WorkoutLogged` en AFTER_COMMIT) ferait une écriture
  **synchrone cross-module** dans Roster — mélange des modèles, couplage transactionnel à travers une
  frontière de module. C'est précisément le couplage que l'event publication registry existe pour éviter.

B résout le **symptôme** (cycle) en créant un **problème pire** (side-effect sync cross-module déguisé en
query). Recul architectural. Rejeté.

## Conséquences

**Positives** : ownership net (dynamique chez Athletics, 1RM chez Roster), aucune écriture croisée ; cliquet
garanti jusqu'à la matérialisation (idempotent, sûr au rejeu) ; cycle cassé proprement **sans** sacrifier
l'event-driven ; Roster reste seul détenteur des standards de force (pas de duplication des ratios élite).

**Coûts assumés** : un contrat d'event vit hors du module producteur (écart cadré à ADR-024) ; le kernel
`shared` gagne un type — à surveiller (discipline du minimalisme : seuls les contrats *entre modules
mutuellement dépendants* y descendent, pas tous les events). Cohérence éventuelle entre la dynamique
(Athletics) et la carte (Roster) — négligeable à l'échelle mois/années du 1RM.

## Alternatives écartées

- **Event dans `athletics.api.events`** : crée le cycle `athletics ↔ roster`. Impossible.
- **Port de commande synchrone `roster`** (Option B) : violerait CLAUDE.md §3.6 (side-effect via event, pas
  via query) et injecterait une écriture sync cross-module dans un flux async. Rejeté (cf. §5).
- **Dénormaliser le 1RM dans Athletics** (athletics tient sa propre copie via events Roster) : supprimerait
  l'arête athletics → roster et donc le cycle, mais contredit la lecture **fraîche** du 1RM mutable (ADR-034)
  et impose à Athletics un read-model d'athlète — surdimensionné, hors-scope.
