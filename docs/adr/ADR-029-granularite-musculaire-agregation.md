# ADR-029 : Forme par groupe musculaire + règle d'agrégation vers l'indice global

**Statut** : Accepté (GATE 1 du sprint 5 validé)
**Date** : Sprint 5
**Décideur** : Ryan Foerster

## Contexte

Le sprint 5 réalise le raffinement annoncé par **ADR-004** : la forme Fitness-Fatigue passe d'une **stat
globale** (sprint 4) à une forme **par groupe musculaire**. On veut pouvoir avoir « les jambes cuites et le
haut du corps frais ». Quatre faits du code existant cadrent la décision :

- `FitnessFatigueState` était une **paire globale** `(fitness, fatigue, lastUpdated)` (ADR-028).
- L'enum `MuscleGroup` (kernel `shared`) compte **11 groupes** : CHEST, BACK_UPPER, BACK_LOWER, QUADS,
  HAMSTRINGS, GLUTES, SHOULDERS, BICEPS, TRICEPS, CALVES, CORE.
- `Genetics` (Roster) impose un **invariant de complétude** : `hypertrophyPotentialByMuscleGroup` doit
  couvrir **tous** les `MuscleGroup` (un nouveau muscle ripplerait en migration JSONB de Genetics).
- L'event `WorkoutLogged` est **autosuffisant** : `LoggedExerciseSnapshot` porte déjà le `pattern`
  (composé) et l'`accessoryRegion` (accessoire) — Athletics n'a rien de nouveau à demander à
  PersonalTraining pour distribuer le stimulus.

Cette ADR couvre la **structure** (Couche 1). Le mapping pondéré sourcé est ADR-030, l'individualisation
génétique ADR-031.

## Décision

### 1. `FitnessFatigueState` devient une Map par muscle, l'aggregate garde sa frontière

`FitnessFatigueState` = `Map<MuscleGroup, MuscleCondition>` + **un seul** `Instant lastUpdated` (nouveau VO
`MuscleCondition(fitness, fatigue)`, sans horloge). Tous les muscles partagent le même instant de référence
(garde d'idempotence et lazy compute inchangés). La **Map est sparse** : un muscle jamais travaillé est
absent (lu comme `MuscleCondition.ZERO`), l'état initial est une Map vide. Le `BanisterModel` décroît et
stimule **muscle par muscle** (indépendants, mêmes constantes de temps — la modulation génétique de τ est la
Couche 3). L'aggregate `AthleteCondition` est **enrichi en contenu, pas en frontière** (clé `AthleteId`,
ADR-027 inchangé).

### 2. Agrégation vers l'indice global : par **somme**

L'indice de Forme 0–100 (UX du sprint 4, inchangée) est calculé sur les **sommes** :
`50 + 50·(Σperf / Σfitness)` avec `Σperf = k1·Σfitness − k2·Σfatigue`. Le ratio reste **indépendant de
`NORMALIZATION`** (numérateur et dénominateur portent la même échelle) et **pondère naturellement par le
volume entraîné** (un muscle beaucoup travaillé pèse plus). On réutilise donc telle quelle la formule
validée au sprint 4.

**Alternative « maillon faible » (min des indices par muscle) rejetée** : elle sur-pénalise un petit muscle
cuit (mollets) qui tirerait tout l'indice global à zéro, et sa nuance (« ta forme est limitée par ton muscle
le plus cuit ») relève du **détail par muscle = sprint 7 (Insights)**, pas de la jauge globale unique. La
simulation GATE 1 confirme que sous entraînement **uniforme** les deux règles coïncident (min ≈ somme,
divergence ≤ 3 points) — elles ne divergeront que sous distribution **inégale** (Couche 2). **Porte ouverte**
: si un playtest sous distribution réaliste montre que la somme masque trop (jambes mortes mais indice haut
car le haut du corps compense), on ré-ouvrira la règle.

### 3. L'enum `MuscleGroup` n'est **pas** enrichi (pas de ripple Genetics)

Vérification faite : les **11 groupes couvrent les 6 `MovementPattern` sans trou** (squat → quads/glutes/
back_lower/hamstrings/core ; deadlift → back_lower/glutes/hamstrings/back_upper… tout est présent). Aucun
muscle à ajouter → **aucune migration JSONB de Genetics**. Le risque que le prompt redoutait n'existe pas
une fois le code vérifié.

Les deux frictions de `BodyRegion` (tracées « à arbitrer sprint 4 » dans ADR-026) se résolvent **dans le
mapping (ADR-030), pas dans l'enum** : `BACK` (plus grossier que upper/lower) → majoritairement
`BACK_UPPER` ; `FOREARMS` (sans équivalent modélisé) → `BICEPS` (choix de lifter assumé, traçé ADR-030).

### 4. Convexité de `effort(rpe)` : **reportée au GATE 2** (linéaire conservé en Couche 2)

Le sprint 4 a gardé `effort(rpe) = rpe/10` linéaire. La simulation GATE 1 montre que le linéaire
**discrimine déjà assez** (le deload à RPE 6 fait bien chuter la fatigue vs les semaines dures à RPE 8, la
supercompensation émerge). On **garde le linéaire en Couche 2** pour une raison méthodologique : la Couche 2
change la distribution musculaire ; changer `effort(rpe)` en même temps mélangerait deux variables (en cas
de courbe bizarre, on ne saurait pas si c'est le mapping ou la convexité). **Une variable à la fois.**

Réévaluation au **GATE 2** (sous distribution réaliste, en simulant un split asymétrique). Candidats tracés
(intuition de lifter : la proximité de l'échec — *stimulating reps*, Nuckols/SBS — pèse **plus** que
linéairement) :

- **Candidat principal : `(rpe − 4) / 6` clampé [0,1].** Capture que les warmups (RPE ≤ 4, ~6+ reps en
  réserve) ne stimulent **quasi rien** (le linéaire leur donne 0.4, ce qui est faux). Coût : seuil à 4 =
  paramètre arbitraire, mais défendable physiologiquement.
- **Candidat de repli : `(rpe/10)^1.5`** (convexité douce, sans seuil) si le seuil produit des effets
  bizarres.
- **Écarté : `(rpe/10)²`** — trop agressif (RPE 6 → 0.36, sur-pénalise les efforts modérés qui stimulent
  quand même).

Décision finale **sur observation au GATE 2**.

### 5. Persistence JSONB + reset assumé (migration V011)

`athlete_conditions.by_muscle` stocke la `Map<MuscleGroup, MuscleCondition>` en **jsonb** (wrapper
`MuscleConditionsJson`, support JSON natif Hibernate 7, pattern sprint 2/3/4). Les conditions globales du
sprint 4 sont **supprimées sans backfill** : l'info « quels muscles ont produit cette fitness globale »
n'existe pas dans les données sprint 4 — un backfill **inventerait** une répartition (données fausses). Une
condition vide honnête qui repart proprement vaut mieux (pré-beta, seul user, déjà reset au debug sprint 4).
Les **snapshots restent agrégés** (fitness/fatigue sommées — la tendance globale dont le sprint 7 a besoin),
table inchangée.

## Conséquences

**Positives**
- La distinction **court/long terme** (sprint 4) est préservée : la forme par muscle reste de l'adaptation
  court terme, distincte des CurrentStats structurels (sprint 6).
- **Forward-compatible** : le détail par muscle (sprint 7) et l'individualisation génétique (Couche 3)
  s'appuient sur cette structure sans la rouvrir.
- **Isolation Modulith intacte**, aucun nouveau cycle, enum non touché → Genetics non rippé.

**Négatives**
- La simulation de calibration (uniforme) ne stresse pas l'agrégation **asymétrique** — ce cas est couvert à
  la Couche 2 (distribution réaliste + split simulé).

**Neutres**
- Le mapping de la Couche 1 est **trivial single-target** (un exercice → un muscle), volontairement
  jetable : il valide la structure, pas la science. Remplacé intégralement par ADR-030.

## Références

- ADR-004 (phasing global→muscle), ADR-026 (frictions BodyRegion/MuscleGroup tracées), ADR-027 (module
  Athletics, clé `AthleteId`), ADR-028 (Banister récursif discret, NORMALIZATION, calibration), ADR-017
  (critère de promotion au kernel `shared`). Suite : ADR-030 (mapping pondéré sourcé), ADR-031 (génétique).
