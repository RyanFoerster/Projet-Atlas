# ADR-033 : Progression structurelle du 1RM — cible convergente vers plafond génétique + cliquet

**Statut** : Accepté (calibration validée à l'œil de lifter sur simulation 12-16 sem, GATE 3a du sprint 6)
**Date** : Sprint 6
**Décideur** : Ryan Foerster (validation des ordres de grandeur et de la stabilité de boucle)

## Contexte

La Couche 2 (ADR-034) a fait entrer la charge (%1RM) dans le **stimulus** — l'adaptation **court terme**
(Banister : fitness semaines, fatigue jours). Mais le 1RM lui-même, la capacité **structurelle long terme**
(mois/années), restait **statique** : un athlète pouvait s'entraîner indéfiniment sans jamais devenir plus
fort *au sens du 1RM*. La Couche 3 fait progresser les `CurrentStats` (le 1RM par pattern) **sans re-mélanger
les trois échelles de temps** (le piège central) : Fatigue (jours, τ≈7), Fitness (semaines, τ≈42), 1RM
(mois, quasi-irréversible à court terme).

Cet ADR décide de **la dynamique** : quelle force est *méritée* par l'entraînement accumulé, et comment le
1RM y converge. L'**ownership** (qui possède le 1RM matérialisé, comment l'event traverse les modules) est
décidé séparément en ADR-032.

## Décision

### 1. Cible convergente vers un plafond génétique dérivé des standards existants

Pour chaque pattern, le 1RM **mérité** converge vers un **plafond** par saturation exponentielle de la charge
chronique accumulée :

```
plafond_p  = bodyweight × ratio_ÉLITE_p × strengthAffinity_p        (génétique, figé à la création)
départ_p   = 1RM au moment où l'athlète entre en simulation          (ancre)
C_p        = charge chronique accumulée du pattern (décroît, τ ≈ 90 j)

mérité(C)  = plafond − (plafond − départ) × exp(−C / SCALE)          SCALE = 20
```

**Le plafond n'est pas un magic number** : il réutilise les **bandes de standards de force** déjà présentes
dans `ProceduralAthleteGenerator` (qui fixe le 1RM de *départ* = `bodyweight × ratio_INTERMÉDIAIRE ×
strengthAffinity`). Le plafond prend la **borne élite** de la même bande (squat 2.3×, bench 1.65×, deadlift
2.7×, ohp 1.0× le poids de corps, × l'affinité). Conséquence forte : **les rendements décroissants
débutant/avancé tombent gratuitement de l'écart au plafond** — un débutant loin de l'élite a un gros headroom
(progresse vite), un avancé proche en a un petit (se bat pour quelques kg). Aucune logique « training age »
n'est codée ; elle **émerge** de la géométrie de l'écart.

### 2. Cliquet : le 1RM ne descend jamais à court terme

```
1RM_p = max(1RM_p, mérité(C_p))
```

La capacité structurelle (masse musculaire, efficience neuromusculaire profonde) ne s'évapore pas en un
deload. Un repos fait **chuter la fitness** (Banister) mais **pas le 1RM** — c'est la distinction
CurrentStats vs Fitness, rendue mécanique par le `max`. La charge chronique `C` décroît au repos (τ≈90 j),
mais comme `mérité` ne fait *remonter* le plancher que par le `max`, une `C` qui redescend ne fait **pas**
reculer le 1RM. L'irréversibilité court terme est **construite**, pas paramétrée.

**Honnêteté épistémique (simplification assumée)** : à *très* long terme (désentraînement de plusieurs mois),
un vrai 1RM finit par baisser. Le cliquet l'ignore volontairement — sur les horizons d'une partie (semaines à
quelques mois), modéliser une atrophie structurelle lente ajouterait de la complexité pour un gain de réalisme
marginal. Réouvrable si le playtest montre que l'absence de detraining long casse l'immersion.

### 3. SCALE = 20, et le plateau à volume constant assumé comme feature

`SCALE` est l'**unique constante de calibration** de la dynamique (combien de charge chronique accumulée pour
approcher le plafond). Fixée à **20** par simulation, validée à l'œil de lifter :

| Scénario (squat, bodyweight 80 kg, 3×/sem) | Départ → 12 sem | Δ |
|---|---|---|
| Débutant (ratio 1.25, plafond élite 184) | 100 → 119 | **+19** (newbie gains réels 15-25 kg/trimestre) |
| Avancé (ratio 2.13, plafond 202) | 170 → 177 | **+7** (un avancé se bat pour quelques kg) |
| Affinité 0.90 (plafond 166) vs 1.20 (plafond 221), même départ 120 | +10 vs +23 | spread génétique ×2.3 |

**Le plateau à routine fixe est une feature, pas un bug.** Comme `C` décroît, le plafond est une **asymptote
jamais atteinte à volume constant** : une routine figée (mêmes reps, même charge absolue) plafonne *bien
en-dessous* de l'élite (un débutant se stabilise ~129 kg, ratio 1.6, pas 184). Physiologiquement juste : une
routine figée **stagne**, seule la **surcharge progressive** (plus de charge/volume/intensité) fait approcher
le potentiel. Le modèle capture ça **émergentiellement** — quand le 1RM monte, la même charge absolue devient
un %1RM plus bas → `load` baisse → stimulus baisse → `C` croît moins (boucle d'auto-régulation, ADR-034 §5).
On **ne rend pas** le plafond plus atteignable à volume modéré : la difficulté à l'approcher **récompense la
bonne programmation**, exactement ce qu'un simulateur sérieux doit faire.

> **Nuance tracée (curseur Atlas)** : *le plateau à volume constant est qualitativement juste (routine fixe
> stagne), mais sa hauteur exacte (~129 kg pour le débutant) est une interprétation Atlas, fonction de
> `SCALE`/`τ`, réajustable si le playtest le demande.* La **cible 12 semaines**, elle, est validée et stable.

### 4. Stabilité de la boucle d'auto-régulation (vérifiée, pas postulée)

Le point de vigilance n°1 (1RM ↑ → %1RM ↓ → moins de stimulus) a été simulé dans les deux régimes :
- **charge fixe** (jamais d'overload) : 100 → 117 — la boucle **s'auto-limite doucement**, le %1RM baissant
  quand le 1RM monte ;
- **progressif** (charge à ~82.5 % du 1RM *courant*) : 100 → 119, converge vers le plafond.

Dans les deux cas : **monte puis ralentit, borné par le plafond (asymptote)**. Aucune divergence, aucun
étouffement brutal. La boucle est **stable par construction** (le `max` borné par le plafond ne peut pas
diverger ; la saturation exponentielle ne peut pas s'emballer).

### 5. Progression par pattern, pilotée par le stimulus de CE pattern (SAID)

`C_p` se nourrit du **stimulus load-aware du pattern correspondant** (les exercices composés de Couche 2,
résolus par `MovementPattern`). C'est le principe de **spécificité (SAID)** : squatter fait progresser le
squat, pas le développé. **Les accessoires (cible = `BodyRegion`, sans pattern) ne font pas progresser un 1RM
directement** — l'effet hypertrophie → force d'un composé est un effet de **2nd ordre** (le muscle grossit,
le composé suit), hors-scope de cette couche. Simplification honnête, tracée, réouvrable.

**Scope assumé — seuls les patterns avec un 1RM de référence progressent structurellement.** `CurrentStats`
ne suit, depuis le Sprint 2, que les **4 grands lifts** (`SQUAT`, `BENCH_PRESS`, `DEADLIFT`,
`OVERHEAD_PRESS` — les seuls que `ProceduralAthleteGenerator` dérive, et les seuls dotés d'un standard de
force). `ROW` et `CHIN_UP` **n'ont pas de 1RM suivi → pas de plafond de référence → pas de cible
convergente** : ils ne progressent pas structurellement. Ce n'est **pas un oubli mais un choix** — pour un
jeu centré sur le powerlifting + press, faire progresser ces 4 lifts est le bon périmètre ; le row et le
chin-up **contribuent à la forme et à l'hypertrophie** (via `distribute` côté Banister) sans avoir leur
propre 1RM affiché. Mécaniquement, le modèle l'exprime par construction (un pattern **sans référence** dans
`advance` n'est jamais initialisé → reste hors progression), pas par une liste en dur. **Réouvrable** si on
ajoute un jour des lifts de référence (le row à 1RM suivi, par ex.) : il suffirait que `CurrentStats` les
porte et qu'un standard de force existe.

### 6. Axes génétiques structurels : enrichir GeneticProfile 3/5 → 5/5, dénormalisés

Le plafond consomme `strengthAffinity_p` (par pattern). Couplé à `hypertrophyPotential` (par muscle, déjà la
matière de Couche 2), le `GeneticProfile` passe de 3 à 5 axes structurels exploités. **Ces axes sont
immutables → dénormalisables** : le plafond est calculé **une fois à la création** (génétique + bodyweight
lus frais à cet instant) puis **figé**. C'est l'exact opposé du 1RM courant, **mutable → lu frais** à chaque
calcul (ADR-034 §5). Les deux dénormalisations cohabitent : *on dénormalise ce qui ne change pas (le plafond
génétique), on lit frais ce qui change (le 1RM courant)*.

## Conséquences

**Positives** : le 1RM progresse de façon réaliste sans magic number (plafond dérivé des standards existants) ;
les rendements décroissants débutant/avancé et le spread génétique sont **émergents**, pas codés ; le cliquet
rend la distinction CurrentStats/Fitness **mécanique** ; la boucle est stable par construction ; le plateau à
routine fixe récompense la programmation sérieuse (cœur de l'identité « simulateur réaliste » d'Atlas).

**Coûts assumés** : le cliquet ignore le detraining très long terme (réouvrable) ; la hauteur exacte du
plateau à volume constant est un curseur Atlas, pas une vérité physiologique (tracé) ; les accessoires ne
nourrissent pas la progression 1RM (effet 2nd ordre hors-scope) ; le plafond est figé à la création, donc une
future variation de bodyweight (bulk/cut, hors-scope sprint 6) imposerait de recalculer le plafond.

## Alternatives écartées

- **Plafond = magic number par pattern** : écarté — les standards de force existent déjà dans le générateur,
  les réutiliser élimine un paramètre arbitraire *et* fait émerger le training age de l'écart au plafond.
- **Progression linéaire au volume (sans saturation)** : ferait diverger les 1RM sans borne et raterait les
  rendements décroissants. Écarté.
- **1RM réversible (suit `mérité` à la baisse)** : casserait la distinction CurrentStats/Fitness — un deload
  ferait fondre le 1RM. Écarté au profit du cliquet (`max`).
- **Plafond plus atteignable à volume modéré** : moins réaliste — une routine figée *doit* stagner sous le
  potentiel. Écarté pour préserver la récompense de la surcharge progressive.
