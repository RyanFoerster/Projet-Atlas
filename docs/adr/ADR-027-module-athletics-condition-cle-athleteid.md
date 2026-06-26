# ADR-027 : Module Athletics — où vit l'état dynamique, clé `AthleteId`, `RosterQueryPort`

**Statut** : Accepté (GATE 2 du sprint 4 validé)
**Date** : Sprint 4
**Décideur** : Ryan Foerster

## Contexte

Le sprint 4 introduit l'état d'adaptation dynamique d'un athlète (Fitness-Fatigue de Banister). Quatre
faits du code existant cadrent la décision :

- `Athlete` est une **entity interne** à l'aggregate `Roster` (ADR-019) — pas de repository propre.
  `CurrentStats` vit à l'intérieur.
- Roster n'exposait **aucun query port** (uniquement des events).
- L'event `WorkoutLogged` est clé par **`ownerId` (le User)**, pas par `AthleteId`. `MirrorAthleteCreated`
  porte `athleteId + rosterId` mais pas d'`ownerId`.
- `AthleteId` vivait dans `roster.domain.model` (pas dans `shared`, contrairement à `UserId`).

Question structurante : **où vit le `FitnessFatigueState` et comment Athletics retrouve l'athlète à mettre
à jour ?**

## Décision

### 1. Athletics = module séparé qui possède l'état dynamique (option 3)

`AthleteCondition` est l'**aggregate root du module athletics**, clé par `AthleteId`. Roster garde
l'identité **statique** (nom, génétique, rareté, `CurrentStats`). L'athlète « complet » est une
**composition à l'affichage**. Séparation de bounded contexts : Roster = identité, Athletics = état
d'adaptation.

Option 2 (enrichir l'`Athlete` de Roster) **rejetée** : gonflerait Roster d'une logique de simulation
lourde.

### 2. Clé par `AthleteId`, pas par `UserId` (option 3a)

Atlas simule **N athlètes par roster**. Cléer par `UserId` (3b) encoderait « un user = un athlète qui
s'entraîne » — vrai au sprint 4 (seul le miroir s'entraîne) mais structurellement faux dès le **sprint 6**
(athlètes virtuels avec programmes). On ne réintroduit pas une hypothèse simplificatrice qui sera fausse
plus tard quand la corriger maintenant coûte peu. Implique deux ripples maîtrisés :

- **Promotion de `AthleteId` vers `shared/`** (critère ADR-017 : transverse à 2+ modules — Roster +
  Athletics, puis Programming au sprint 6). Le 2ᵉ consommateur apparaît, c'est le bon moment.
- **`RosterQueryPort.findMirrorAthleteId(UserId)`** — le **premier query port de Roster**. L'event ne
  porte que l'`ownerId` ; Athletics résout le miroir via ce port synchrone, puis applique le stimulus
  déclenché par l'event. **Events pour les side-effects, ports pour les queries** — symétrique de
  `PersonalTrainingQueryPort` (sprint 3, option D).

### 3. `CurrentStats` reste dans Roster, stable ce sprint

La progression structurelle des `CurrentStats` part au sprint 5. Au sprint 4 ils restent dans Roster et
Athletics ne les lit même **pas** : la performance disponible est exposée comme un **indice de Forme
adimensionnel** (`k1·fitness − k2·fatigue`, normalisé 0–100 à l'affichage), pas comme un kg. Donc aucun port
de lecture de `CurrentStats` n'est nécessaire ce sprint. L'ownership de la *progression* `CurrentStats`
sera tranché au sprint 5.

### 4. Pas de composition backend (éviter le cycle)

Athletics dépend déjà de `roster.api` (`RosterQueryPort`). Si Roster composait la condition dans son
`AthleteDto` (via un `athletics.api`), on aurait **Roster → Athletics → Roster = cycle Modulith**. Donc :
**endpoint Athletics séparé** (`GET /api/athletes/:id/condition`) et **composition côté frontend** (2 fetchs
sur la fiche). Cohérent avec la doctrine sprint 3 (option D).

## Conséquences

**Positives**
- Séparation des bounded contexts propre et **forward-compatible** (athlètes virtuels au sprint 6 sans
  rework du modèle d'identité).
- Roster expose enfin un query port → la dualité events/ports se généralise.
- `AthleteId` au kernel partagé, au bon timing (anti-sur-anticipation respectée).

**Négatives**
- 2ᵉ fetch frontend (composition côté client) plutôt qu'un payload unique. Acceptable, et impose la
  **dégradation gracieuse** (un échec du fetch condition n'enlève pas la fiche).
- La promotion `AthleteId → shared` touche ~10 fichiers (ripple maîtrisé, fait en une passe).

**Neutres**
- L'idempotence du handler repose sur une garde monotone (`acceptsStimulusAt`, voir ADR-028 / le handler) —
  pas de compteur dupliqué.

## Références

- ADR-017 (critère de promotion au kernel `shared`), ADR-019 (Athlete entity interne à Roster),
  ADR-023 (event-driven), ADR-025 (option D, sprint 3), ADR-028 (modèle Banister).
