# ADR-031 : Individualisation génétique du modèle de Banister + convexité de effort(rpe)

**Statut** : Accepté (GATE 3 du sprint 5 validé)
**Date** : Sprint 5
**Décideur** : Ryan Foerster

## Contexte

La Couche 3 fait enfin **piloter la simulation par la génétique** (rentabilise le système procédural du
sprint 2). `Genetics` (Roster) a 5 axes, est **immutable** (fixée à la création). Athletics lit Roster via
`roster.api` (ADR-027). Question : quels axes modulent quels paramètres Banister, comment Athletics lit la
génétique, et où l'applique-t-il (le decay = chemin de lecture). On en profite pour acter la **convexité de
`effort(rpe)`** reportée du GATE 2 (ADR-029).

## Décision

### 1. Axes génétique → paramètres Banister : recovery + sensitivity (court terme uniquement)

- **`baseRecoveryRate` (0.85–1.20) → τ_fatigue** : `τ_fatigue_eff = τ_fatigue / recoveryRate` (5.83–8.24 j).
  Récupère vite ⇒ fatigue (τ7) décroît plus vite ⇒ supercompense plus vite. **N'affecte pas τ_fitness**
  (arbitrage : la récupération est un phénomène court terme).
- **`trainingResponseSensitivity` (0.85–1.15) → multiplicateur de magnitude du stimulus** : `S_eff = S ×
  sensitivity` (±15 %). High/low responder (HERITAGE/Bouchard). **Revérification de l'axe** (il portait
  « à revérifier sprint 4 ») : on l'**interprète comme un multiplicateur déterministe de réponse**, pas un
  bruit stochastique — cohérent avec un modèle déterministe et reproductible.

**Rejetés ce sprint : `hypertrophyPotentialByMuscleGroup` et `strengthAffinityByPattern`.** Ce sont des axes
**structurels / long terme** : les utiliser pour moduler le Banister (court terme) re-brouillerait la
distinction court/long terme durement établie au sprint 4. Ils piloteront la **progression structurelle des
CurrentStats au sprint 6** — c'est leur place naturelle. La séparation des axes suit la séparation
court/long terme.

### 2. `fiberTypeProfile` : réservé (redondance avec recovery)

`fiberTypeProfile` (0–1, endurance↔force) toucherait la **résistance à la fatigue** (fibres lentes =
fatigue-résistantes) — ce qui **chevauche `baseRecoveryRate`** (les deux agissent sur la dynamique de
fatigue). Pour éviter un **double-comptage** de la résistance à la fatigue, on le **réserve** : aucun levier
distinct identifié ce sprint. À rouvrir si un mécanisme propre émerge (ex. fiber → plafond/gain de fitness,
ou → poids de sortie k1/k2, ou côté structurel au sprint 6). `GeneticProfile` le transporte déjà (prêt),
`GeneticModifiers` ne l'utilise pas encore.

### 3. Ampleur calibrée par simulation (GATE 3)

Validé sur le programme 12 semaines, deux génétiques, même programme → **progressions différentes et
cohérentes** :
- recovery 1.20 vs 0.85 : même ΣFitness (recovery n'agit pas sur la fitness), mais **perf disponible plus
  haute** pour le récupérateur (perf post-deload 7.96 vs 6.36 — moins de fatigue résiduelle).
- sensitivity 1.15 vs 0.85 : **plus de fitness construite** (ΣFitness 17.7 vs 13.1).

Les fourchettes (±15–18 %) rendent la génétique **sensible sans casser la comparabilité** entre athlètes.

### 4. Comment Athletics lit la génétique : port + dénormalisation

- **`RosterQueryPort.findGeneticProfile(AthleteId)` → `GeneticProfile`** (snapshot api-level, types
  primitifs — ne fait pas fuiter le VO `Genetics`). Résolution via `RosterRepository.findByAthleteId`
  (jointure sur la collection d'athlètes). Le **mapping `Genetics → paramètres Banister` reste chez
  Athletics** (`GeneticModifiers` dérivé par le handler) — Roster expose la donnée brute, pas la
  connaissance Banister.
- **Dénormalisation** : `AthleteCondition` porte ses `GeneticModifiers`, **résolus une seule fois à la
  création** de la condition (branche `orElseGet` du handler), persistés (migration **V012**, colonnes
  `recovery_rate` / `stimulus_multiplier`). La `Genetics` étant **immutable**, c'est le cas où dénormaliser
  est justifié (la règle « ne pas dupliquer ce qui change » — option D sprint 3 — ne s'applique pas à une
  donnée immutable). **Conséquence clé** : la génétique module aussi le **decay** (τ_fatigue), donc le
  **chemin de lecture (lazy compute)** ; les modifiers stockés sont relus à chaque lecture **sans rappeler
  Roster**.

### 5. Convexité `effort(rpe)` : `(rpe − 4) / 6` clampé (actée)

Réévaluée sur observation (GATE 2). Adoptée :

```
RPE   3     4     5     6     7     8     9    10
lin. 0.30  0.40  0.50  0.60  0.70  0.80  0.90  1.00
adop 0.00  0.00  0.17  0.33  0.50  0.67  0.83  1.00
```

Raison décisive : le **warmup à RPE 4** (~6 RIR) ne stimule essentiellement rien — le linéaire lui donnait
0.40 (faux) ; `(rpe−4)/6` le met à **0**. Convexité **douce** (RPE 8 → 0.67) qui reflète les *stimulating
reps* (Nuckols/SBS) sans écraser les efforts modérés comme le ferait `(rpe/10)²` (écarté). Le seuil 4 est
défendable (sous RPE 4, stimulus négligeable), pas un magic number. **NORM recalibré 0.01 → 0.013** (la
nouvelle formule réduit les magnitudes ~×0.78) — la courbe re-roulée garde la supercompensation et la même
échelle (ΣFitness S12 ≈ 15.4). Recalibrage légitime d'une échelle libre, pas un fudge.

## Conséquences

**Positives**
- La génétique **pilote enfin la simulation** (aboutissement de la vision sport-science : procédural ↔
  Banister). Court/long terme préservé.
- Dénormalisation propre (immutable → zéro staleness), lazy compute respecté, isolation Modulith verte
  (Athletics lit `roster.api` uniquement).

**Négatives**
- `fiberTypeProfile` reste un axe Genetics **dormant** jusqu'à ce qu'un levier distinct soit trouvé.
- `RosterRepository` gagne une 3ᵉ méthode de lecture (`findByAthleteId`) — coût mineur, justifié.

**Neutres**
- `GeneticModifiers` est un VO Athletics pur ; le mapping depuis `GeneticProfile` vit dans le handler
  (application), pas dans le domaine.

## Références

- ADR-029 (granularité, convexité reportée), ADR-030 (mapping, ampleur composé/isolation), ADR-027 (port
  Roster, clé AthleteId), ADR-028 (Banister, NORM, honnêteté épistémique), ADR-017 (kernel shared),
  ADR-006 (lazy compute), ADR-025 (option D, immutabilité vs dénormalisation). Sprint 6 : CurrentStats
  structurels (hypertrophy/strength), charge absolue, fiber peut-être réactivé.
