# Sprint 5 — Granularité musculaire & individualisation génétique

> Mini-cours (format C). Le sprint 4 avait posé le moteur Banister sur une stat globale. Le sprint 5 le rend
> **réaliste** (par muscle, distribué par les exercices) et **individuel** (modulé par la génétique). Cinq
> concepts denses, chacun isolé pour bien le comprendre hors d'Atlas.

## Ce qu'on a appris

Enrichir un aggregate sans casser sa frontière ; traduire de la science incomplète en code défendable ;
agréger N états en un indice lisible ; individualiser un modèle par paramétrage ; et trancher une décision
sur observation plutôt que par anticipation.

---

## Concept 1 — Enrichir un value object sans casser la frontière de l'aggregate

### Définition
Faire évoluer la **structure interne** d'un VO porté par un aggregate (ici `FitnessFatigueState` : d'une
paire globale `(fitness, fatigue)` à une `Map<MuscleGroup, MuscleCondition>`) **sans changer qui possède
quoi** ni les frontières inter-modules.

### Pourquoi c'est important
En DDD, la **frontière de l'aggregate** est le contrat de cohérence. La toucher ripple partout (events,
ports, autres modules). Pouvoir densifier le *contenu* sans toucher la *frontière* est ce qui rend un modèle
évolutif. Ici : `AthleteCondition` reste clé par `AthleteId`, ses events et son port ne changent pas — seul
son intérieur se raffine.

### Comment c'est utilisé dans Atlas
`FitnessFatigueState` devient `Map<MuscleGroup, MuscleCondition> + un seul lastUpdated`. Le `BanisterModel`
boucle par muscle. L'aggregate `AthleteCondition`, sa clé, ses méthodes publiques (`applyStimulus`,
`projectedTo`, `acceptsStimulusAt`) gardent leur **signature de frontière**. Décision clé : **un seul
timestamp** partagé par tous les muscles (pas un par muscle) — sinon la garde d'idempotence et le lazy
compute se compliquent.

### Exemple minimal hors Atlas
Un `Panier` (aggregate) qui passe d'un `total: Money` à une `Map<Devise, Money>` pour gérer le multi-devise.
Les méthodes `ajouter(article)`, `valider()` gardent leur signature ; seul le calcul interne du total change.
Le code appelant le `Panier` ne voit rien.

### Pièges classiques
- **Map mutable qui fuit** : sans copie défensive (`Map.copyOf`), une référence externe peut muter l'état
  « immutable ». Atlas recopie dans le constructeur canonique.
- **Map dense au lieu de sparse** : créer une entrée à zéro pour chaque muscle non travaillé alourdit et
  brouille « jamais travaillé » vs « travaillé puis décru ». Atlas garde la map **sparse**.

### Pour aller plus loin
Vaughn Vernon, *Implementing DDD*, ch. 10 (Aggregates) — la règle « modéliser de vraies invariantes
dans une frontière de cohérence ».

---

## Concept 2 — Traduire une science incomplète en code défendable (honnêteté épistémique)

### Définition
Construire un mapping quantitatif (`MovementPattern/BodyRegion → poids par muscle`) **là où la littérature
donne un classement mais pas des nombres**, en distinguant explicitement ce qui est sourcé de ce qui est
une interprétation maison.

### Pourquoi c'est important
L'EMG mesure l'**activation** (amplitude électrique), pas le **stimulus pour l'adaptation**. Prétendre que
« squat = 42 % quads » est « scientifique » serait malhonnête. Mais ne rien faire serait pire. La posture
défendable : **sourcer le classement** (quads dominants au squat, biceps secondaires au row — ça, l'EMG le
dit), **assumer les nombres** comme calibration Atlas. C'est ce qui rend le modèle critiquable — donc
crédible — face à une communauté qui connaît la physiologie.

### Comment c'est utilisé dans Atlas
`MuscleStimulusMapping` : des tables de pondération (somme = 1 par exercice) avec, en commentaire et en ADR,
le statut `[source EMG/SBS]` vs `[interprétation Atlas]`. Les deux frictions connues sont assumées
explicitement : `BACK` (plus grossier que upper/lower) → 80/20, `FOREARMS` (sans muscle modélisé) → biceps.

### Exemple minimal hors Atlas
Un score de risque crédit qui combine des features dont certaines ont une littérature solide (ratio
dette/revenu) et d'autres une heuristique maison (ancienneté du compte). Documenter quelle pondération vient
d'une étude et laquelle est un choix calibré — au lieu de tout présenter comme « data-driven ».

### Pièges classiques
- **Faux precision** : donner 3 décimales à un nombre inventé fait croire à une mesure. Rester sur des
  pondérations rondes et tracer leur statut.
- **Confondre activation et stimulus** : l'EMG d'un mollet peut être élevé sans que ce soit le facteur
  limitant. Le classement reste indicatif, pas absolu.

### Pour aller plus loin
Greg Nuckols (Stronger By Science) sur les limites de l'EMG comme proxy d'hypertrophie ; Schoenfeld &
Krieger sur le volume comme driver dose-réponse.

---

## Concept 3 — Agréger N états en un indice lisible (la règle, et pourquoi)

### Définition
Passer de N `MuscleCondition` à **un** indice global 0–100. Deux règles candidates : la **somme**
(`50 + 50·Σperf/Σfitness`) ou le **maillon-faible** (le plus petit indice par muscle).

### Pourquoi c'est important
Le choix de la règle d'agrégation **est une décision de modélisation**, pas un détail technique. Elle change
ce que l'indice *signifie*. La somme dit « ton état moyen pondéré par le volume » ; le maillon-faible dit
« ton point le plus cuit te limite ».

### Comment c'est utilisé dans Atlas
Atlas retient la **somme** pour la jauge globale : robuste, indépendante de l'échelle interne NORM, pondérée
par le volume entraîné, et elle réutilise la formule validée au sprint 4. **Insight de la simulation** : les
deux règles **coïncident sous entraînement uniforme** (dans une séance, tous les muscles partagent le même
ratio fitness/fatigue → même indice). Elles ne **divergent que sous asymétrie temporelle** : jambes
entraînées lundi, bras vendredi, vu samedi → jambes récupérées (indice 51), bras cuits (indice 11) →
**somme 24** vs **maillon-faible 11**. Pour la jauge globale unique, « globalement modéré, jambes OK » (24)
est plus juste que « cuit » (11) pour un seul groupe. Le maillon-faible relèvera du **détail par muscle
(sprint 7)**.

### Exemple minimal hors Atlas
La « santé » d'un système distribué : moyenne pondérée des services (vue d'ensemble) vs le service le plus
dégradé (SLA réel). On veut souvent les deux, à deux niveaux d'affichage différents — exactement la
distinction jauge globale / détail.

### Pièges classiques
- **Tester l'agrégation sur un cas trop symétrique** : la première simulation, uniforme, faisait croire que
  somme ≈ maillon-faible toujours. Il a fallu un scénario **asymétrique dans le temps** pour révéler la
  divergence. *Tester une règle d'agrégation exige un cas où les entrées diffèrent vraiment.*
- **Choisir le min « parce que c'est conservateur »** sans voir qu'un petit muscle cuit fait paniquer la
  jauge globale à tort.

### Pour aller plus loin
Théorie de l'agrégation de préférences (moyenne vs min/max) ; en pratique, la conception d'indices composites
(ex. indices de santé publique) documente toujours sa règle d'agrégation.

---

## Concept 4 — Individualisation paramétrique d'un modèle

### Définition
Faire varier le **comportement** d'un modèle entre individus en **modulant ses paramètres** (et non en
changeant le modèle), à partir d'un profil (ici la `Genetics` procédurale).

### Pourquoi c'est important
C'est le pont entre deux systèmes d'Atlas : la **génétique procédurale** (sprint 2, jusqu'ici un simple
profil affiché) et la **simulation Banister** (sprint 4). L'individualisation paramétrique la fait enfin
*piloter* la simulation, sans dupliquer le modèle par athlète.

### Comment c'est utilisé dans Atlas
Deux leviers **distincts** (vérifiés non-redondants par simulation) :
- `baseRecoveryRate → τ_fatigue` (`τ_eff = 7/recovery`) : agit sur la **fraîcheur** (un récupérateur a moins
  de fatigue résiduelle → plus de perf disponible), pas sur le plafond de fitness.
- `trainingResponseSensitivity → multiplicateur de magnitude` : agit sur la **construction** (un fort
  répondeur bâtit plus de fitness).

Les `GeneticModifiers` dérivés sont **dénormalisés** dans `AthleteCondition` (voir piège ci-dessous).
`fiberTypeProfile` est **réservé** (il toucherait aussi la fatigue → redondant avec recovery, double-comptage
à éviter). Les axes **structurels** (hypertrophie, affinité) sont réservés au sprint 6 — les brancher sur le
court terme casserait la distinction court/long terme.

### Exemple minimal hors Atlas
Un moteur de tarification où le même modèle de risque a ses **coefficients** modulés par le profil client
(âge, historique), au lieu d'un modèle par segment. Un profil → des paramètres → un comportement
individualisé, modèle unique.

### Pièges classiques
- **Double-comptage d'axes** : deux paramètres qui agissent sur le même mécanisme (ici recovery et fiber sur
  la fatigue) se cumulent de façon incontrôlée. Atlas réserve fiber tant qu'aucun levier *distinct* n'est
  trouvé.
- **Mélanger court et long terme** : utiliser un axe structurel (hypertrophie) pour moduler la forme aiguë
  brouille la sémantique. Garder chaque axe sur sa temporalité.

### Pour aller plus loin
L'étude HERITAGE (Bouchard) sur la variabilité inter-individuelle de la réponse à l'entraînement (high/low
responders), qui justifie `trainingResponseSensitivity` comme multiplicateur réel.

---

## Concept 5 — Trancher une décision sur observation (la convexité de effort(rpe))

### Définition
Reporter une décision de modélisation jusqu'à disposer d'une **observation** qui la tranche, plutôt que de
la prendre par anticipation théorique.

### Pourquoi c'est important
La convexité de `effort(rpe)` avait été **considérée et écartée au sprint 4** (pas de besoin observé), puis
**reportée au GATE 2** (« une variable à la fois » : ne pas la changer en même temps que la distribution,
sinon une courbe bizarre serait inattribuable). On la tranche au moment où on peut *l'observer*.

### Comment c'est utilisé dans Atlas
Au GATE 2, un tableau comparatif a tranché. La **ligne décisive : RPE 4 (warmup)**. Le linéaire `rpe/10`
donne 0.40 à un échauffement à ~6 reps en réserve — physiologiquement faux (un warmup ne stimule quasi
rien). Le candidat `(rpe−4)/6` clampé le met à **0**, et introduit une convexité **douce** (RPE 8 → 0.67)
qui reflète les *stimulating reps* sans écraser les efforts modérés comme le ferait `(rpe/10)²`. Conséquence
mécanique : la magnitude baisse (~×0.78), donc **NORM recalibré 0.01 → 0.013** pour garder l'échelle — la
courbe re-roulée garde sa supercompensation. Recalibrage légitime d'une échelle libre, pas un fudge.

### Exemple minimal hors Atlas
Choisir une fonction de perte (L1 vs L2) non pas a priori, mais après avoir tracé les résidus sur des
données réelles et vu lequel gère mieux les outliers du domaine. La donnée tranche, pas le dogme.

### Pièges classiques
- **Changer deux variables à la fois** : modifier `effort(rpe)` ET la distribution simultanément aurait
  rendu tout diagnostic ambigu. Isoler les changements.
- **Oublier de recalibrer l'échelle** : durcir `effort` baisse les magnitudes ; sans ajuster NORM, les
  courbes paraissent « plates » et on conclut à tort que la formule est mauvaise.

### Pour aller plus loin
La distinction « decision under uncertainty » : différer une décision a une valeur quand une observation
peu coûteuse va lever l'incertitude (value of information).

---

## Auto-évaluation

1. Pourquoi un **seul** `lastUpdated` au niveau du `FitnessFatigueState`, et pas un par muscle ?
2. Qu'est-ce que l'EMG mesure exactement, et pourquoi est-ce un *proxy* du stimulus et non le stimulus ?
3. Dans quel cas précis l'agrégation par somme et par maillon-faible **divergent**-elles, et pourquoi pas
   dans une séance uniforme ?
4. Quels deux axes génétiques pilotent le Banister au sprint 5, et sur quoi agit chacun (fraîcheur vs
   construction) ?
5. Pourquoi `fiberTypeProfile` est-il réservé plutôt que câblé ?
6. Pourquoi la dénormalisation des `GeneticModifiers` dans `AthleteCondition` est-elle justifiée ici alors
   que l'option D du sprint 3 refusait de dupliquer ? (indice : immutabilité)
7. Pourquoi un curl dépose-t-il plus de stimulus sur ses biceps qu'un squat sur ses quads, et pourquoi
   est-ce une limite **assumée** plutôt qu'un bug ?
8. Quelle ligne du tableau `effort(rpe)` a tranché la convexité, et pourquoi le linéaire y était-il faux ?
9. Pourquoi a-t-il fallu recalibrer NORM après avoir changé `effort(rpe)` ?
10. Pourquoi réserver les axes génétiques **structurels** (hypertrophie, affinité) au sprint 6 plutôt que
    de les brancher maintenant ?
