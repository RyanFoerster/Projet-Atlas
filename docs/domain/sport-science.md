# Sport science d'Atlas

> Source de vérité **scientifique** du projet : ce que la littérature dit, ce qu'Atlas en retient, et — surtout — ce qu'Atlas **assume** là où la littérature s'arrête. Les *décisions* d'ingénierie correspondantes vivent dans les ADRs ; ce document explique la *science*.
>
> État : sprint 5 (Fitness-Fatigue **par groupe musculaire**, mapping stimulus sourcé, individualisation génétique). Le sprint 4 avait posé la stat globale. Sera enrichi au sprint 6 (progression structurelle des CurrentStats, charge absolue).

---

## 1. Les deux temporalités de l'adaptation

Atlas modélise **deux** échelles de temps distinctes. Les confondre casserait la crédibilité du jeu.

| | Fitness / Fatigue | CurrentStats |
|---|---|---|
| **Échelle** | jours → semaines (court terme) | mois → années (long terme) |
| **Nature** | adaptation aiguë, affûtage neuromusculaire | capacités structurelles (1RM réel, masse musculaire) |
| **Dynamique** | monte et **redescend** vite (surtout la fatigue) | se construit lentement, ne se perd **pas** vite |
| **En deload** | la fitness **dip** (l'affûtage redescend) | **inchangé** |
| **Sprint 4** | modélisé (Banister, global) | stable (lu, pas encore fait progresser → sprint 5) |

La **performance exprimée un jour donné** = f(potentiel structurel, état de forme du moment). Au sprint 4
on modélise l'état de forme (Banister) ; le potentiel structurel (`CurrentStats`) est posé mais stable.

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

`S = NORMALIZATION × Σ_séries ( reps × effort(rpe) )`

Atlas retient deux des trois variables de dose de la musculation :

1. **Volume** (`reps`) — driver primaire de l'adaptation (Schoenfeld & Krieger).
2. **Intensité d'effort** (`effort(rpe)`) — proximité de l'échec (Helms, RIR ; Nuckols, *stimulating reps*).
   RPE absent → effort **neutre** (RPE 7 supposé ; l'omission n'est ni récompensée ni pénalisée).

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

La 3ᵉ variable, l'**intensité de charge (%1RM)**, reste **volontairement absente** : le RPE capture déjà
l'intensité *relative à la capacité* (RPE 8 = 2 reps en réserve, quel que soit le poids absolu). La charge
absolue arrive au **sprint 6** (et résoudra le déséquilibre composé/isolation, ci-dessous).

> **Honnêteté épistémique.** Il n'existe **aucune valeur de littérature** pour convertir une séance de
> force en impulsion de Banister (le modèle d'origine est endurance, impulsion = TRIMP cardiaque). Ce
> mapping est un **choix de modélisation Atlas**, assumé comme « calibration par défaut, raffinée par
> simulation ». Modéliser honnêtement là où la science s'arrête est ce qui rend le modèle défendable.

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

### ⚠️ Déséquilibre composé vs isolation (limite assumée sprint 6)

Avec somme = 1 et charge exclue, à reps/RPE égaux un **curl dépose plus sur ses biceps qu'un squat sur ses
quads** (l'isolé concentre, le composé répartit). Ce qui rend un squat « plus gros » est la **charge** (140
vs 20 kg), modélisée au sprint 6 — pas un facteur d'ampleur arbitraire ajouté maintenant. Par muscle, le
poids < 1 du prime mover d'un composé capture aussi qu'il n'est pas pris aussi près de SON échec individuel
qu'un isolé. Limite tracée, résolue naturellement par la charge.

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

## 7. À venir (sprint 6)

- **Progression structurelle des `CurrentStats`** : accumulateur de stimulus chronique → montée lente
  quasi-irréversible du 1RM. Pilotée par les axes génétiques structurels (hypertrophie, affinité de force).
- **Charge absolue / %1RM** dans le stimulus (résout le déséquilibre composé/isolation) + distinction
  poids de corps / leste / charge externe.
- **`fiberTypeProfile`** possiblement réactivé si un levier distinct émerge.

---

## Sources

- Banister, E.W. (1975). *A systems model of training for athletic performance*.
- Calvert, T.W., Banister, E.W., et al. (1976). *A systems model of the effects of training on physical performance*.
- Schoenfeld, B., Krieger, J. — méta-analyses dose-réponse (volume).
- Helms, E. et al. — *The RPE Pyramid* (RPE/RIR).
- Nuckols, G. — Stronger By Science (« stimulating reps », activation EMG, méta-analyses).
- Bouchard, C. et al. — étude HERITAGE (variabilité inter-individuelle de la réponse à l'entraînement).
