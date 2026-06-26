# ADR-028 : Modèle de Banister — formulation récursive discrète, constantes et calibration par simulation

**Statut** : Accepté (GATE 1 du sprint 4 validé)
**Date** : Sprint 4
**Décideur** : Ryan Foerster

## Contexte

Le module Athletics doit modéliser l'adaptation court terme d'un athlète à l'entraînement (le couple
Fitness/Fatigue de Banister, ADR-004), au sprint 4 sur **une stat globale** (le raffinement par
`MuscleGroup` est le sprint 5). Trois contraintes cadrent la décision :

1. **Lazy compute** (ADR-006) : pas de scheduler qui tick les athlètes ; l'état doit pouvoir se calculer
   à la volée depuis un `lastUpdated`.
2. **Event-driven** (ADR-023) : l'état est mis à jour à la réception de `WorkoutLogged`.
3. **Crédibilité scientifique** : le modèle doit être défendable face à la communauté lifting — donc
   sourcé, ou explicitement assumé là où la littérature ne tranche pas.

Or la littérature Banister/Calvert décrit le modèle pour la **performance d'endurance** (impulsion =
TRIMP = durée × intensité cardiaque). **Aucune valeur de littérature n'existe pour transposer le modèle à
une « forme » de musculation** ni pour convertir une séance de force en impulsion scalaire. Cette
transposition est un **choix de modélisation Atlas**, à assumer honnêtement.

## Décision

### 1. Formulation récursive discrète (et non convolution continue)

On stocke l'état courant `(fitness, fatigue, lastUpdated)` et on calcule par récurrence :

- **Décroissance** depuis `lastUpdated` : `fitness ← fitness · exp(−Δt/τ_fitness)`,
  `fatigue ← fatigue · exp(−Δt/τ_fatigue)`.
- **Stimulus** : `fitness += S`, `fatigue += S`.
- **Performance disponible** : `k1 · fitness − k2 · fatigue`.

On ne ré-intègre jamais tout l'historique (convolution continue) : coûteux et contradictoire avec le lazy
compute. La forme récursive ne persiste que l'état courant + le timestamp — exactement ce dont le lazy
compute a besoin (l'exponentielle est sans mémoire : décroître en deux temps = décroître en un, le
résultat est identique quel que soit le découpage).

### 2. Asymétrie en sortie uniquement

La **même** impulsion `S` est ajoutée à la fitness ET à la fatigue. L'asymétrie du modèle est portée
par **(a)** les constantes de temps (`τ_fatigue ≪ τ_fitness`) et **(b)** les poids de sortie `k2 > k1`.
C'est le Banister classique — plus défendable et plus sourçable que de pondérer l'entrée.

### 3. Constantes

| Constante | Valeur | Source / justification |
|-----------|--------|------------------------|
| `τ_fitness` | 42 jours | Banister 1975 / Calvert 1976 (décroissance lente). Défaut endurance, **assumé** pour la forme de force, calibré par simulation. |
| `τ_fatigue` | 7 jours | Idem (décroissance rapide). |
| `k1` | 1.0 | Poids de la fitness. Classique. |
| `k2` | 2.0 | Poids de la fatigue (la fatigue « masque » ~2×). Classique. |
| `NORMALIZATION` | 0.01 | **Échelle interne arbitraire**, provisoire. La forme de la courbe est invariante à NORM (modèle linéaire en l'impulsion) ; NORM ne fixe que l'échelle verticale. La normalisation vers une échelle lisible (0–100) est faite à **l'affichage** (Couche 2), pas ici. |

### 4. Formule du stimulus

```
S_séance = NORMALIZATION × Σ_séries ( reps × effort(rpe) )
```

Deux des trois variables de dose de la musculation :
- **volume** = `reps` (driver primaire — Schoenfeld & Krieger) ;
- **intensité d'effort** = `effort(rpe) = rpe / 10` (proximité de l'échec — Helms RIR), **linéaire** ;
- `rpe` absent → effort **neutre** 0.7 (= RPE moyen réel : l'omission n'est ni récompensée ni pénalisée ;
  0.6 inciterait à sur-logger des RPE optimistes).

**Choix assumés** (tracés exprès) :
- **Charge absolue (kg / %1RM) exclue** : le RPE capture déjà l'intensité *relative à la capacité*
  (RPE 8 = 2 RIR quel que soit le poids), sans avoir besoin du 1RM. La charge absolue et la distribution
  par muscle sont le **sprint 5**. (Conséquence : Athletics ne lit pas `CurrentStats` ce sprint.)
- **Durée exclue** : `volume × effort` capture déjà la dose ; la durée est un proxy faible et redondant
  (candidat futur comme modulateur de *fatigue* uniquement).
- **Linéaire en l'impulsion** (pas de saturation intra-séance) : la saturation émerge de la *dynamique*
  (accumulation de fatigue), pas de l'impulsion — fidèle au Banister classique.
- **Convexité `(rpe/10)²` (« stimulating reps », Nuckols/SBS) considérée et écartée** : la simulation ne
  montre aucun besoin de discrimination supplémentaire (cf. ci-dessous). Reste candidate **si** un jour on
  simule des programmes à difficulté variable et qu'on veut accentuer le poids des séances dures.

### 5. Calibration par simulation (preuve)

Scénario de référence (`BanisterCalibrationSimulationTest`) : 12 semaines, 4 séances/sem, deload en
semaine 7. Trajectoire hebdomadaire produite :

```
Semaine | Fitness | Fatigue | Performance
   1    |   2.900 |   1.847 |    -0.793   ← « cuit » : fatigue domine au démarrage
   6    |  11.943 |   2.914 |     6.115   ← pic d'accumulation (fatigue saturée ~2.9)
   7    |  10.546 |   1.350 |     7.846   ← DELOAD : fatigue s'effondre → perf bondit (+28%)
   8    |  11.827 |   2.343 |     7.141   ← reprise (toutes les semaines suivantes restent > pré-deload)
  12    |  15.266 |   2.911 |     9.444
```

La **supercompensation est émergente** (non codée) : pendant le deload, la fatigue (τ court) s'efface bien
plus vite que la fitness (τ long), donc la performance remonte au-dessus du pic d'accumulation — et le
bloc post-deload s'installe sur un plateau de performance plus haut. C'est la périodisation, validée par
la dynamique du modèle. L'assertion robuste du test n'est pas une valeur ponctuelle (dépendante de NORM)
mais la **forme** : `chute relative de fatigue > chute relative de fitness` pendant le deload.

### 6. Le dip de fitness en deload est attendu et correct

Pendant le deload, la fitness baisse de ~12 %. **C'est une feature, pas un bug.** La fitness de Banister
modélise l'**adaptation aiguë court terme** (l'affûtage), qui *doit* redescendre quand on réduit le
volume — distincte de la **force structurelle** (`CurrentStats`, 1RM réel) qui, elle, ne dip pas (et reste
stable tout le sprint 4 par décision ADR-004). Allonger `τ_fitness` à 50–60 j pour adoucir le dip
brouillerait précisément la distinction court/long terme que tout le sprint cherche à établir. **Le dip
rend visible que la fitness est court terme.** (Voir aussi `docs/domain/sport-science.md`.)

## Conséquences

**Positives**
- Modèle compatible lazy compute + event-driven (état minimal : 2 doubles + 1 timestamp).
- Distinction court/long terme nette et défendable, matérialisée par le comportement en deload.
- Périodisation émergente : forte crédibilité face à la communauté.
- Toutes les constantes sont soit sourcées, soit explicitement assumées (posture épistémique honnête).

**Négatives**
- Les constantes ne sont **pas empiriquement validées pour la musculation** (transposition endurance →
  force assumée). Mitigation : calibration par simulation, et raffinement possible au sprint 5+ avec la
  granularité par muscle et la génétique.
- `NORMALIZATION` reste arbitraire tant que l'échelle d'affichage 0–100 n'est pas calée (Couche 2).

**Neutres**
- Le mapping performance → indice 0–100 d'affichage devra gérer la **performance négative** (athlète
  « cuit ») : prévoir un point neutre (ex. 50 = neutre, < = sur-fatigué, > = affûté) plutôt qu'un
  écrasement brutal en 0. À décider au frontend (Couche 2).

## Références

- Banister, E.W. (1975). *A systems model of training for athletic performance*. Aust J Sports Med.
- Calvert, T.W., Banister, E.W., et al. (1976). *A systems model of the effects of training on physical performance*. IEEE Trans. Syst. Man Cybern.
- Schoenfeld, B., Krieger, J. — méta-analyses dose-réponse (volume comme driver primaire).
- Helms, E. et al. — *The RPE Pyramid* (RPE/RIR comme proximité de l'échec).
- Nuckols, G. — Stronger By Science (« stimulating reps »).
