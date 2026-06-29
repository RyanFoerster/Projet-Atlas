# Sport science d'Atlas

> Source de vérité **scientifique** du projet : ce que la littérature dit, ce qu'Atlas en retient, et — surtout — ce qu'Atlas **assume** là où la littérature s'arrête. Les *décisions* d'ingénierie correspondantes vivent dans les ADRs ; ce document explique la *science*.
>
> État : sprint 6 (le moteur est **complet**). Charge **%1RM** dans le stimulus (3ᵉ variable de dose) et **progression structurelle** du 1RM (montée lente quasi-irréversible vers un plafond génétique). S'ajoute à la forme par groupe musculaire des sprints 4–5. **Trois échelles de temps** coexistent désormais.

---

## 1. Les trois échelles de temps de l'adaptation

Atlas modélise **trois** dynamiques temporelles distinctes dans le même athlète. Les confondre — par exemple
laisser un deload grignoter le 1RM — casserait la crédibilité du jeu. La règle d'or : **ne jamais re-mélanger
ces horloges**.

| | Fatigue | Fitness (forme) | CurrentStats (1RM) |
|---|---|---|---|
| **Échelle** | jours (τ ≈ 7) | semaines (τ ≈ 42) | mois → années (**quasi-irréversible**) |
| **Nature** | dette de récupération aiguë | affûtage neuromusculaire | capacités structurelles (1RM réel, masse) |
| **Dynamique** | monte vite, descend vite | monte/descend lentement | se construit lentement, ne se perd **pas** vite |
| **En deload** | s'efface vite (repos) | **dip** (l'affûtage redescend) | **inchangé** |
| **Sprint 6** | modélisé (Banister) | modélisé (Banister) | **fait progresser** (cible convergente + cliquet) |

La **performance exprimée un jour donné** = f(potentiel structurel, état de forme du moment). Sprints 4–5 :
l'état de forme (Banister, fatigue + fitness). Sprint 6 : le potentiel structurel (`CurrentStats`) se met
enfin à **progresser**, à sa propre échelle de temps (τ_chronic ≈ 90 j), sans être contaminé par les deux
autres.

> **Preuve end-to-end** (test du gate conceptuel, Clock contrôlée) : 12 sem d'entraînement → squat 140 →
> **144,72 kg** ; puis 6 sem de repos → fitness **1,264 → 0,465** (perte d'un facteur e sur une constante de
> temps) **mais 1RM = 144,72 kg, exactement**. Un deload te rend rouillé, pas faible.

---

## 2. Le modèle Fitness-Fatigue de Banister

Modèle *impulse-response* (Banister 1975 ; Calvert et al. 1976). Chaque séance est une **impulsion** `S`
qui génère deux réponses antagonistes :

- **Fitness** : adaptation positive. Monte modérément, décroît **lentement** (constante de temps
  `τ_fitness ≈ 42 j`).
- **Fatigue** : effet négatif. Monte fortement, décroît **vite** (`τ_fatigue ≈ 7 j`).
- **Performance disponible** = `k1 · fitness − k2 · fatigue` (poids classiques `k1 = 1`, `k2 = 2`).

### Formulation retenue (récursive discrète)

On garde l'état courant `(fitness, fatigue, lastUpdated)` et on décroît exponentiellement depuis le
timestamp, puis on ajoute le stimulus :

```
fitness ← fitness · exp(−Δt/τ_fitness) ;   fatigue ← fatigue · exp(−Δt/τ_fatigue)
fitness += S ;                              fatigue += S
performance = k1 · fitness − k2 · fatigue
```

La même impulsion `S` alimente fitness ET fatigue ; **l'asymétrie vient des τ et des k**, pas de l'entrée.
Pas de ré-intégration de l'historique (lazy compute, ADR-006). Détails et sources : **ADR-028**.

### Supercompensation (émergente)

Juste après une séance, la fatigue domine → performance basse (« cuit »). Quelques jours plus tard, la
fatigue (τ court) s'est effacée plus que la fitness (τ long) → la performance dépasse le niveau initial.
C'est le fondement de la périodisation et du **deload**. Vérifié par simulation 12 semaines : un deload en
semaine 7 fait **bondir la performance de +28 %**, et tout le bloc suivant s'installe plus haut. Cette
périodisation n'est **pas codée** : elle émerge de la dynamique.

### ⚠️ Le dip de fitness en deload est attendu et correct

Pendant un deload, la fitness redescend (~12 % sur une semaine dans la simulation). **C'est voulu.** La
fitness de Banister modélise l'**adaptation aiguë court terme** (l'affûtage), qui *doit* redescendre quand
on baisse le volume — c'est distinct de la **force structurelle** (`CurrentStats`), qui ne dip pas. Ce dip
est la **matérialisation visible de la distinction court/long terme**, cœur conceptuel du sprint 4.
Allonger `τ_fitness` pour « adoucir » le dip brouillerait justement cette distinction. *Feature, pas bug.*

---

## 3. De la séance au stimulus

`S = NORMALIZATION × Σ_séries ( reps × effort(rpe) × load(%1RM) )`

Atlas retient depuis le sprint 6 les **trois** variables de dose de la musculation :

1. **Volume** (`reps`) — driver primaire de l'adaptation (Schoenfeld & Krieger).
2. **Intensité d'effort** (`effort(rpe)`) — proximité de l'échec (Helms, RIR ; Nuckols, *stimulating reps*).
   RPE absent → effort **neutre** (RPE 7 supposé ; l'omission n'est ni récompensée ni pénalisée).
3. **Intensité de charge** (`load(%1RM)`) — tension mécanique, proximité du 1RM (sprint 6, ADR-034). %1RM
   absent (accessoire, ou composé sans 1RM de référence) → `load` au **plancher** (le travail léger compte
   quand même, mais sans crédit de tension).

### `effort(rpe)` : seuil convexe doux `(rpe − 4) / 6` (sprint 5, ADR-031)

Le sprint 4 utilisait `effort = rpe/10` (linéaire). Réévalué au sprint 5 sur observation : adopté
`effort(rpe) = clamp((rpe − 4) / 6, 0, 1)`.

| RPE | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|
| linéaire `rpe/10` | 0.30 | **0.40** | 0.50 | 0.60 | 0.70 | 0.80 | 0.90 | 1.00 |
| adopté `(rpe−4)/6` | 0.00 | **0.00** | 0.17 | 0.33 | 0.50 | 0.67 | 0.83 | 1.00 |

Deux propriétés voulues : les **warmups (RPE ≤ 4, ~6+ reps en réserve) ne stimulent quasi rien** — le
linéaire leur donnait 0.40, physiologiquement faux ; et une **convexité douce** qui reflète les *stimulating
reps* (les dernières reps avant l'échec recrutent les fibres rapides, disproportionnellement stimulantes)
sans écraser les efforts modérés comme le ferait `(rpe/10)²`. Le seuil 4 est défendable (sous RPE 4,
stimulus négligeable), pas un magic number. `NORMALIZATION` recalibrée 0.01 → 0.013 (la nouvelle formule
réduit les magnitudes ~×0.78 ; l'échelle des courbes est préservée).

> **Honnêteté épistémique.** Il n'existe **aucune valeur de littérature** pour convertir une séance de
> force en impulsion de Banister (le modèle d'origine est endurance, impulsion = TRIMP cardiaque). Ce
> mapping est un **choix de modélisation Atlas**, assumé comme « calibration par défaut, raffinée par
> simulation ». Modéliser honnêtement là où la science s'arrête est ce qui rend le modèle défendable.

### `load(%1RM)` : charge orthogonale à l'effort (sprint 6, ADR-034)

`load(%1RM) = LOAD_FLOOR + (1 − LOAD_FLOOR) × clamp((%1RM − 0,30) / (0,90 − 0,30))`, avec **plancher 0,40**,
montée linéaire entre **30 %** et **90 %** du 1RM, plafond à ≥ 90 % (tension maximale).

| %1RM | < 30 % | 50 % | 70 % | 90 %+ | absent |
|---|---|---|---|---|---|
| `load` | 0,40 | 0,53 | 0,80 | 1,00 | 0,40 (plancher) |

**Charge et effort sont orthogonaux.** `effort(rpe)` mesure la **proximité de l'échec** (reps en réserve),
`load(%1RM)` la **tension mécanique** (proximité du 1RM). Ils se dissocient : un 5×5 à 70 % mené à RPE 9 est
effort élevé / charge moyenne ; un single à 95 % à RPE 7 est l'inverse. Atlas les garde comme **deux facteurs
distincts multipliés** — jamais fondus en un seul nombre. Le **plancher 0,40** évite d'annuler le travail
léger haut-volume et donne sa valeur d'effort à un accessoire sans 1RM connu (crédit d'effort conservé, crédit
de tension au plancher). Le %1RM se calcule `charge totale / 1RM du pattern`, le 1RM étant lu **frais** dans
Roster à chaque séance (§8). `NORMALIZATION` recalibrée **0,013 → 0,014** (l'ajout de `load`, souvent < 1,
abaisse les magnitudes ; l'échelle verticale est préservée).

Cette 3ᵉ variable **résout le déséquilibre composé/isolation** laissé ouvert au sprint 5 : un squat à 140 kg
porte une charge absolue qu'un curl à 20 kg n'a pas, et reprend son ascendant.

---

## 4. Forme par groupe musculaire (sprint 5)

La paire globale du sprint 4 devient une forme **par `MuscleGroup`** (`Map<MuscleGroup, MuscleCondition>` +
un seul timestamp) : on peut avoir les jambes cuites et le haut frais. Chaque muscle suit Banister
indépendamment (mêmes τ, sauf modulation génétique de τ_fatigue). ADR-029.

### Agrégation vers l'indice global : par **somme**

L'indice de Forme 0–100 (UX inchangée) est calculé sur les sommes : `50 + 50·(Σperf/Σfitness)`, indépendant
de `NORMALIZATION`, pondéré par le volume entraîné. L'alternative « **maillon faible** » (l'indice du muscle
le plus cuit) est rejetée pour la jauge globale : elle sur-pénalise un petit muscle cuit. **Insight** : les
deux règles ne divergent que sous **asymétrie temporelle** (muscles entraînés à des moments différents —
dans une même séance, tous partagent le même ratio fitness/fatigue). Ex. jambes J0 / bras J+5, vu J+6 :
somme 24 (« globalement modéré, jambes OK ») vs maillon-faible 11 (« tes bras sont morts »). Le détail par
muscle relève du **sprint 7 (Insights)**, pas de la jauge globale unique.

## 5. Distribution du stimulus sur les muscles (mapping sourcé, sprint 5)

Le stimulus d'un exercice est réparti sur les muscles via des **tables de pondération** (somme = 1 par
exercice). ADR-030.

> **Statut épistémique.** L'EMG mesure l'**activation**, pas le stimulus pour l'adaptation. L'utiliser comme
> proxy de répartition est un **choix de modélisation Atlas**. Le *classement* des muscles (primaire/
> secondaire) est **sourcé** (EMG, Stronger By Science, biomécanique) ; les *nombres exacts* sont une
> **interprétation Atlas** calibrée pour la plausibilité — même honnêteté que la calibration Banister.

**Composés** (`MovementPattern`) :

| Pattern | Pondérations |
|---|---|
| SQUAT | QUADS 0.42, GLUTES 0.30, BACK_LOWER 0.10, CORE 0.10, HAMSTRINGS 0.08 |
| BENCH_PRESS | CHEST 0.50, TRICEPS 0.25, SHOULDERS 0.25 |
| DEADLIFT | BACK_LOWER 0.25, GLUTES 0.25, HAMSTRINGS 0.20, BACK_UPPER 0.15, QUADS 0.10, CORE 0.05 |
| OVERHEAD_PRESS | SHOULDERS 0.55, TRICEPS 0.25, CHEST 0.10, CORE 0.10 |
| ROW | BACK_UPPER 0.55, BICEPS 0.20, SHOULDERS 0.10, BACK_LOWER 0.10, CORE 0.05 |
| CHIN_UP | BACK_UPPER 0.55, BICEPS 0.30, CORE 0.10, SHOULDERS 0.05 |

Notes : ischios faibles au squat (contraction quasi-isométrique). Deadlift = conventionnel, chin-up =
supination (génériques assumés). **Accessoires** (`BodyRegion`) : cible directe (poids 1.0), sauf deux
frictions assumées — `BACK` → BACK_UPPER 0.80 / BACK_LOWER 0.20 (plus grossier que le modèle), et `FOREARMS`
→ BICEPS 1.0 (pas de muscle avant-bras modélisé ; fléchisseurs adjacents).

### ✓ Déséquilibre composé vs isolation (résolu au sprint 6 par la charge)

Avec somme = 1 et (jusqu'au sprint 5) charge exclue, à reps/RPE égaux un curl déposait plus sur ses biceps
qu'un squat sur ses quads (l'isolé concentre, le composé répartit). Le sprint 6 le **résout comme prévu** :
`load(%1RM)` (§3) donne au squat à 140 kg la charge absolue qu'un curl à 20 kg n'a pas — pas via un facteur
d'ampleur arbitraire, mais via le vrai mécanisme (la tension). Limite tracée au sprint 5, **close** au sprint 6.

## 6. Individualisation génétique (sprint 5)

La `Genetics` (immutable) module le Banister de chaque athlète. ADR-031. **Deux leviers distincts** :

- **`baseRecoveryRate` (0.85–1.20) → τ_fatigue** : `τ_fatigue_eff = 7 / recoveryRate`. Récupère vite ⇒
  fatigue décroît vite ⇒ supercompense plus vite. Agit sur la **fraîcheur** (perf disponible), pas sur le
  plafond de fitness. N'affecte **pas** τ_fitness.
- **`trainingResponseSensitivity` (0.85–1.15) → multiplicateur de magnitude** : high/low responder
  (HERITAGE/Bouchard). Agit sur la **construction** (plus de fitness bâtie). Réinterprété comme un
  multiplicateur déterministe de réponse, pas un bruit stochastique.

`fiberTypeProfile` est **réservé** : il toucherait la résistance à la fatigue, **redondant avec recovery**
(éviter le double-comptage). Les axes **structurels** (`hypertrophyPotential`, `strengthAffinity`) sont
réservés au **sprint 6** (progression des CurrentStats) — les utiliser sur le Banister court terme
re-brouillerait la distinction court/long terme.

---

## 7. Progression structurelle du 1RM (sprint 6)

Le 1RM (`CurrentStats`) progresse vers un **plafond génétique** par une **cible convergente**, verrouillée
par un **cliquet** (montée seulement). ADR-033.

```
mérité(C) = plafond − (plafond − départ) · exp(−C / SCALE)        SCALE = 20
1RM = max(1RM, mérité)                                            (cliquet, n'émet que les hausses)
plafond = poids_de_corps × ratio_ÉLITE(pattern, sexe) × strengthAffinity(pattern)
```

`C` est la **charge chronique** accumulée par pattern, qui décroît à **τ_chronic = 90 jours** (l'arrêt de
l'entraînement **fige** la progression, ne la fait pas régresser — d'où le « quasi-irréversible » de la 3ᵉ
échelle de temps). Le **plafond** individualise le potentiel : les `StrengthStandards` (ratios de force par
lift/sexe, source Roster) donnent la bande, l'axe génétique **structurel** `strengthAffinity` (réservé au
sprint 5) la module enfin — c'est sa place naturelle (long terme).

### Comportements physiologiques **émergents** (non codés)

Trois comportements bien connus **tombent** de cette forme convergente, sans aucun cas particulier :

- **Newbie gains.** Loin du plafond, `exp` est raide → gros gains ; près du plafond, elle s'aplatit → gains
  minuscules. Les **rendements décroissants émergent de l'écart au plafond**, pas d'un `if trainingAge`.
  Calibration : débutant **+19 kg/12 sem**, avancé **+7 kg**, écart génétique **×2,3**, tous sous plafond.
- **Plateau à volume constant = feature.** À charge fixe, `C` se stabilise (accumulation ≈ décroissance
  τ=90 j) → `mérité` plafonne. Reproduit la **surcharge progressive** : il faut augmenter la charge pour
  reprogresser. « Mon athlète stagne à 120 kg » est correct, pas un bug.
- **Boucle d'auto-régulation.** Le 1RM↑ → la même charge absolue devient un **%1RM plus bas** → `load`↓ →
  stimulus↓ → progression↓. *Negative feedback* émergent, **stable** en calibration (convergence, pas
  d'emballement). Il n'est stable que parce que le 1RM est relu **frais** (§8).

> **Scope assumé** (ADR-033 §5) : seuls les patterns ayant un 1RM de référence dans `CurrentStats` (les gros
> lifts) progressent structurellement. ROW/CHIN_UP sans 1RM matérialisé ne progressent pas — **choix conscient**,
> pas un oubli.

## 8. Ownership et lectures inter-modules (sprint 6)

Le 1RM **vit dans Roster** (identité de l'athlète, ADR-019), mais **Athletics porte le modèle** de
progression. Résolution (ADR-032) : Athletics **émet** `CurrentStatsProgressed`, Roster **consomme**
(copy-on-write). Le cycle `athletics ↔ roster` que cela crée est cassé en descendant **le seul contrat de
l'event** dans `shared/events` — la **logique** (le `StructuralProgressionModel`, le cliquet) reste dans
athletics.

**Deux régimes de lecture** d'Athletics vers Roster, selon la mutabilité :

- **Plafond génétique** (immuable, dérivé de la `Genetics`) → **lu une fois et dénormalisé** dans
  l'accumulateur de progression.
- **1RM courant** (mutable, il progresse) → **relu frais à chaque séance** pour le %1RM. Le mettre en cache
  casserait l'auto-régulation : c'est précisément la donnée que la boucle fait bouger.

## 9. À venir (sprint 7 — Insights)

- **Courbes de progression** (les `ConditionSnapshot` append-only attendent depuis le sprint 4) et
  **trajectoire 1RM → plafond**.
- **Détail par muscle** (l'agrégation « maillon-faible » rouvrable y trouve sa place).
- **`fiberTypeProfile`** possiblement réactivé si un levier distinct émerge.

---

## Sources

- Banister, E.W. (1975). *A systems model of training for athletic performance*.
- Calvert, T.W., Banister, E.W., et al. (1976). *A systems model of the effects of training on physical performance*.
- Schoenfeld, B., Krieger, J. — méta-analyses dose-réponse (volume).
- Helms, E. et al. — *The RPE Pyramid* (RPE/RIR).
- Nuckols, G. — Stronger By Science (« stimulating reps », activation EMG, méta-analyses).
- Zatsiorsky, V., Kraemer, W. — *Science and Practice of Strength Training* (adaptation aiguë vs structurelle).
- McDonald, L. — *years of training* et gains de force attendus par niveau (forme convergente empirique).
- Standards de force par lift/sexe (powerlifting) — bandes débutant→élite, base des `StrengthStandards`.
- Bouchard, C. et al. — étude HERITAGE (variabilité inter-individuelle de la réponse à l'entraînement).
