# Sport science d'Atlas

> Source de vérité **scientifique** du projet : ce que la littérature dit, ce qu'Atlas en retient, et — surtout — ce qu'Atlas **assume** là où la littérature s'arrête. Les *décisions* d'ingénierie correspondantes vivent dans les ADRs ; ce document explique la *science*.
>
> État : sprint 4 (Fitness-Fatigue de Banister, stat globale). Sera enrichi au sprint 5 (par groupe musculaire, mapping stimulus, génétique).

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
2. **Intensité d'effort** (`effort(rpe) = rpe/10`, linéaire) — proximité de l'échec (Helms, RIR).
   RPE absent → effort **neutre 0.7** (RPE moyen réel ; l'omission n'est ni récompensée ni pénalisée).

La 3ᵉ variable, l'**intensité de charge (%1RM)**, est **volontairement absente au sprint 4** : le RPE
capture déjà l'intensité *relative à la capacité* (RPE 8 = 2 reps en réserve, quel que soit le poids
absolu). La charge absolue et la distribution par muscle arrivent au **sprint 5**.

> **Honnêteté épistémique.** Il n'existe **aucune valeur de littérature** pour convertir une séance de
> force en impulsion de Banister (le modèle d'origine est endurance, impulsion = TRIMP cardiaque). Ce
> mapping est un **choix de modélisation Atlas**, assumé comme « calibration par défaut, raffinée par
> simulation ». Modéliser honnêtement là où la science s'arrête est ce qui rend le modèle défendable.

---

## 4. À venir (sprint 5+)

- Fitness/Fatigue **par groupe musculaire** (ADR-004) + mapping `MovementPattern`/`BodyRegion → MuscleGroup`.
- **Individualisation génétique** des constantes (recovery rate, response sensitivity).
- **Progression structurelle des `CurrentStats`** (le 1RM monte quand le stimulus chronique le justifie).
- Convexité de `effort(rpe)` (« stimulating reps ») si des programmes à difficulté variable le justifient.

---

## Sources

- Banister, E.W. (1975). *A systems model of training for athletic performance*.
- Calvert, T.W., Banister, E.W., et al. (1976). *A systems model of the effects of training on physical performance*.
- Schoenfeld, B., Krieger, J. — méta-analyses dose-réponse (volume).
- Helms, E. et al. — *The RPE Pyramid* (RPE/RIR).
- Nuckols, G. — Stronger By Science (« stimulating reps », méta-analyses).
