# ADR-030 : Mapping stimulus pondéré sourcé (MovementPattern / BodyRegion → MuscleGroup)

**Statut** : Accepté (GATE 2 du sprint 5 validé)
**Date** : Sprint 5
**Décideur** : Ryan Foerster (validation des pondérations à l'œil de lifter)

## Contexte

La Couche 1 (ADR-029) a posé la structure par muscle avec un mapping **trivial single-target** (jetable). La
Couche 2 le remplace par un **mapping pondéré sourcé** : où va le stimulus d'un squat, d'un curl ? C'est **la
décision scientifique du sprint**. L'event `WorkoutLogged` est autosuffisant (`pattern` composé +
`accessoryRegion` accessoire), donc Athletics a tout pour distribuer sans re-query.

## Décision

### 1. Promotion de `BodyRegion` vers `shared`, l'event reste en String

`BodyRegion` (était interne à PersonalTraining) est **promu au kernel `shared`** (critère ADR-017 :
transverse à PersonalTraining qui le produit + Athletics qui le consomme ; promotion déjà anticipée par
ADR-026). Athletics l'utilise comme **type** pour un mapping exhaustif et sûr.

**L'event `WorkoutLogged` garde `accessoryRegion` en `String`** (ADR-024, « types primitifs ») ; le handler
Athletics le résout en `BodyRegion` via `valueOf` **à sa frontière** (anti-corruption). Le nom vient
toujours de `BodyRegion.name()` côté producteur → `valueOf` est sûr. *Ceci raffine l'intention d'ADR-029
(« porter l'enum dans l'event ») vers **moins de couplage** : l'event reste décorrélé du domaine consumer,
l'interprétation vit chez le consommateur.*

### 2. Statut épistémique (honnêteté assumée)

L'**EMG mesure l'activation**, pas le stimulus pour l'adaptation. L'utiliser comme proxy de répartition est
un **choix de modélisation Atlas**. Donc : le **classement** des muscles (primaire / secondaire) est
**sourcé** (volume = Schoenfeld & Krieger ; activation = études EMG ; *stimulating reps* = Nuckols/Stronger
By Science) ; les **nombres exacts** sont une **interprétation Atlas** calibrée pour la plausibilité. Même
honnêteté que la calibration Banister (ADR-028).

### 3. Tables de pondération (somme = 1 par exercice)

Les poids répartissent la magnitude d'un exercice (`reps × effort`), ils ne la créent pas.

**Composés — `MovementPattern → {MuscleGroup: poids}`**

| Pattern | Pondérations |
|---|---|
| SQUAT | QUADS 0.42, GLUTES 0.30, BACK_LOWER 0.10, CORE 0.10, HAMSTRINGS 0.08 |
| BENCH_PRESS | CHEST 0.50, TRICEPS 0.25, SHOULDERS 0.25 |
| DEADLIFT | BACK_LOWER 0.25, GLUTES 0.25, HAMSTRINGS 0.20, BACK_UPPER 0.15, QUADS 0.10, CORE 0.05 |
| OVERHEAD_PRESS | SHOULDERS 0.55, TRICEPS 0.25, CHEST 0.10, CORE 0.10 |
| ROW | BACK_UPPER 0.55, BICEPS 0.20, SHOULDERS 0.10, BACK_LOWER 0.10, CORE 0.05 |
| CHIN_UP | BACK_UPPER 0.55, BICEPS 0.30, CORE 0.10, SHOULDERS 0.05 |

Notes : ischios faibles au squat (contraction quasi-isométrique, EMG bas). Deadlift = **conventionnel**
assumé (sumo serait plus quads). Chin-up = **supination** assumée (biceps 0.30 > row) ; une « traction »
pronation-dominante justifierait ~0.25 — nuance tracée, on garde 0.30 pour le `CHIN_UP` générique.

**Accessoires — `BodyRegion → {MuscleGroup: poids}`** : cible directe (1.0) sauf les deux frictions :
- **`BACK`** (plus grossier que upper/lower) → BACK_UPPER 0.80, BACK_LOWER 0.20 `[interp. Atlas]`
- **`FOREARMS`** (sans équivalent modélisé) → **BICEPS 1.0** `[interp. Atlas assumée]` (fléchisseurs
  adjacents, le grip co-charge le biceps ; choix de lifter validé — un accessoire d'avant-bras doit produire
  *quelque chose*, pas un trou).

### 4. Point « ampleur composé vs isolation » : limite assumée, résolue au sprint 6 (PAS de facteur d'ampleur)

Avec somme = 1 et magnitude `reps × effort` (charge absolue exclue), à reps/RPE égaux un **curl dépose plus
sur ses biceps qu'un squat sur ses quads** (l'isolé concentre, le composé répartit). On **n'ajoute PAS** de
multiplicateur composé/accessoire, pour trois raisons :

1. **Le mécanisme manquant est la CHARGE** (140 kg vs 20 kg), explicitement le **sprint 6**. Un facteur
   d'ampleur serait un proxy arbitraire d'un trou déjà planifié pour se fermer proprement.
2. **Par muscle, ce n'est pas faux** : le poids du prime mover d'un composé (< 1) capture qu'il n'est pas
   pris aussi près de SON échec individuel qu'un isolé (un squat à RPE 8 prend le *système* à 2 RIR, pas
   forcément les quads).
3. **Une variable à la fois** + décision sur observation (même doctrine que la convexité).

**Observé au GATE 2** (simulation d'asymétrie temporelle) : pas de comportement absurde. Si un playtest
futur montre qu'un isolé domine de façon irréaliste, on rouvrira **sur observation** (facteur composé, ou
attendre la charge). Tracé aussi dans la note sprint 6.

## Conséquences

**Positives**
- Distribution **défendable et sourcée** : un squat charge jambes + lombaires, **pas** les biceps ; un curl
  isole les biceps. Prouvé par test (cœur du GATE 2).
- `BodyRegion` au kernel, mapping **exhaustif et typé** (switch enum), event toujours décorrélé.
- Honnêteté épistémique préservée (sources où elles existent, interprétation Atlas assumée sinon).

**Négatives**
- Stimulus relatif composé/isolation imparfait tant que la charge n'entre pas (sprint 6) — limite tracée.

**Neutres**
- L'agrégation **somme** (ADR-029) diverge maintenant du « maillon-faible » sous asymétrie temporelle
  (observé : Somme 24 vs Maillon-faible 11) — confirme que la somme est le bon choix pour la jauge globale,
  le maillon-faible relevant du détail par muscle (sprint 7).

## Références

- ADR-029 (granularité, agrégation somme, convexité reportée), ADR-026 (frictions BodyRegion), ADR-024
  (events primitifs/shared), ADR-017 (promotion kernel), ADR-028 (calibration, honnêteté épistémique).
  Suite : ADR-031 (génétique + décision convexité). Détail des sources dans `docs/domain/sport-science.md`.
