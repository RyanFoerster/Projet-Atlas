# ADR-034 : La charge (%1RM) dans le stimulus, sans double comptage avec le RPE

**Statut** : Accepté (GATE de la Couche 2 du sprint 6 validé)
**Date** : Sprint 6
**Décideur** : Ryan Foerster (validation des ordres de grandeur à l'œil de lifter, sur simulation)

## Contexte

Jusqu'au sprint 5, le stimulus d'une série = `reps × effort(rpe)` (volume × proximité de l'échec). La
**charge** était hors-scope, ce qui laissait le **point d'ampleur** non résolu (un curl léger pouvait
déposer autant qu'un squat lourd, tracé ADR-030 « résolu par la charge sprint 6 »). La Couche 1 (ADR-035) a
posé la saisie typée (poids de corps / lesté / externe). La Couche 2 fait **entrer la charge** dans le calcul.

## Décision

### 1. Troisième facteur orthogonal : `load(%1RM)`

`stimulus_série = reps × effort(rpe) × load(%1RM)`. Les deux facteurs d'intensité mesurent des choses
**orthogonales**, donc **pas de double comptage** :
- `effort(rpe)` = **proximité de l'échec** (ce que le RPE voit) — inchangé, `clamp((rpe−4)/6)` ;
- `load(%1RM)` = **tension mécanique** (ce que le RPE *ignore* : à RPE 8, le RPE ne sait pas le poids sur la
  barre). À RPE égal, 20 reps @50 % et 2 reps @90 % ont le même `effort` mais un `load` différent → la charge
  ajoute une info réelle. C'est aussi ce qui **résout l'ampleur** : un squat lourd > un curl léger,
  naturellement.

### 2. Forme de `load` : clampé-linéaire, plancher 0.40 (parallèle à `effort`)

`load(p) = 0.40 + 0.60 × clamp((p − 0.30) / (0.90 − 0.30), 0, 1)` (p = %1RM). Plancher **0.40** sous 30 %
(le travail léger haut-volume stimule réellement — ne pas l'annuler), plafond **1.0** à ≥90 % (tension
maximale). Choisi **linéaire** plutôt que convexe « zone force 70-90 % » (SBS/Helms) : ici c'est un stimulus
**général** (forme) ; la convexité force-spécifique est réservée à la **progression structurelle** (Couche 3).
Symétrie volontaire avec `effort` (deux rampes clampées, deux axes orthogonaux).

### 3. Accessoires (sans 1RM) → plancher

Un accessoire (cible = `BodyRegion`, pas de pattern) n'a **pas de 1RM de référence** → `%1RM` indéfini →
`load` au **plancher** (0.40). Logique : on crédite l'**effort** (RPE connu) mais pas une **tension** qu'on ne
peut mesurer. Un composé sans 1RM connu (CurrentStats sparse) tombe au même plancher.

**Vérification d'ampleur (simulation, œil de lifter)** : squat lourd 5×5 @80 % RPE8 → quads ≈ 6.3 ;
curl **léger** 3×12 RPE6 → biceps ≈ 4.8 → **le composé lourd domine** (critère du gate tenu). **Honnêteté
assumée** : un curl **dur** (3×12 à l'échec) dépasse le squat *sur son muscle* (≈ 12 vs 6.3). C'est
défendable — 36 reps à l'échec matraquent réellement les biceps *localement* — et c'est une propriété de la
**distribution sum=1** (l'isolation concentre 100 %, le composé étale 42 % sur les quads), **pas** de `load`.
Le `%1RM` étant *relatif*, il n'encode pas la charge *absolue* (140 kg vs 15 kg) : l'ampleur « lourd vs léger »
est résolue, « lourd vs isolation-dure » reste comparable. Accepté.

### 4. Charge totale résolue côté Athletics, depuis la saisie brute + le bodyweight

L'event porte la saisie brute (loadType + valeur, ADR-035). Athletics résout la **charge totale déplacée** :
`EXTERNAL` → valeur seule ; `WEIGHTED` → bodyWeight + leste ; `BODYWEIGHT` → bodyWeight seul. Puis
`%1RM = charge totale / 1RM(pattern)`. PersonalTraining reste découplé de Roster ; Athletics centralise la
physiologie (il lit déjà Roster).

### 5. Lecture FRAÎCHE du 1RM + bodyWeight (pas de dénormalisation)

Le `RosterQueryPort` est enrichi d'un `findLoadProfile(athleteId)` → `AthleteLoadProfile(bodyWeightKg,
1RM par pattern)`, relu **à chaque calcul de stimulus**. **Différence clé avec la Genetics du sprint 5** : les
1RM sont **mutables** (ils progressent dès la Couche 3), donc on **ne dénormalise pas** — sinon le %1RM se
calculerait sur un 1RM périmé. « Ne pas dénormaliser ce qui change » (leçon option D du sprint 3), à l'opposé
de la dénormalisation immutable de la Genetics (ADR-031). Boucle d'auto-régulation à surveiller au GATE
Couche 3 : 1RM ↑ → même charge = %1RM ↓ → moins de stimulus (physiologiquement correct, stabilité à vérifier).

### 6. Recalibrage NORM : 0.013 → 0.014

L'ajout de `load` réduit légèrement les magnitudes. Mais le travail utile vit à 70-90 % où `load ≈ 0.8-1.0`,
donc le bump est **minime** (sim 12 sem : quad fitness J84 1.40 à NORM=0.013 → ≈1.51 à 0.014, ≈ la cible
sprint 5). Recalibrage légitime, bien plus discret que la convexité du sprint 5.

## Conséquences

**Positives** : les trois variables de dose (volume, effort, charge) sont modélisées ; le point d'ampleur est
résolu ; `effort` et `load` orthogonaux (pas de double comptage, prouvé par test) ; lecture fraîche prête pour
des 1RM qui progressent (Couche 3).

**Coûts assumés** : `%1RM` relatif n'encode pas la charge absolue → une isolation à fort volume rivalise par
muscle avec un composé lourd (accepté, propriété de la distribution). Le 1RM d'un pattern au poids de corps
(chin-up) suppose un 1RM exprimé en charge *système* cohérente avec la charge totale — calibration à affiner
si besoin. Lecture Roster à chaque séance (coût d'une query synchrone, acceptable, lazy compute).

## Alternatives écartées

- **`load` convexe (zone force 70-90 %)** : réservée à la progression structurelle force-spécifique (Couche 3),
  surdimensionnée pour un stimulus général.
- **Accessoires = `load` neutre élevé** : les ferait dominer les composés (sim : curl léger > squat). Écarté
  au profit du plancher.
- **Dénormaliser le 1RM** (comme la Genetics) : impossible, le 1RM est mutable (progresse Couche 3) → staleness.
