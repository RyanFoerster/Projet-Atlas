# Sprint 04 — Athletics & le modèle de Banister

> Mini-cours (format C). Le sprint le plus exigeant **scientifiquement** : modéliser l'adaptation à l'entraînement (Fitness-Fatigue de Banister), proprement et honnêtement, dans un domaine pur testable. Au sprint 4 on pose les fondations sur une **stat globale** ; le sprint 5 densifiera (par muscle, génétique, progression structurelle).

## Ce qu'on a appris

- Modéliser un système dynamique (impulse-response) en **forme récursive discrète**, compatible lazy compute.
- La distinction **court terme (Fitness/Fatigue) vs long terme (CurrentStats)** — ce qui rend une simulation d'entraînement crédible.
- **Valider un modèle scientifique par simulation longue**, pas seulement par assertions unitaires.
- L'**honnêteté épistémique** : sourcer ce qui existe, assumer explicitement ce qui n'existe pas.
- **Dérisquer un modèle complexe par phasing** : architecture d'abord (stat globale), science ensuite (par muscle).
- Trois pièges concrets : collision de bean Spring, dépendance à NORM dans l'affichage, events non redélivrés à un listener ajouté après coup.

---

## Concept 1 : Le modèle de Banister (impulse-response) et la supercompensation

### Définition
Le modèle Fitness-Fatigue de Banister (1975) modélise chaque séance comme une **impulsion** `S` qui génère deux réponses antagonistes qui décroissent exponentiellement :
- **Fitness** : adaptation positive, décroît **lentement** (`τ_fitness ≈ 42 j`).
- **Fatigue** : effet négatif, décroît **vite** (`τ_fatigue ≈ 7 j`).
- **Performance disponible** = `k1·fitness − k2·fatigue` (k1=1, k2=2).

### Pourquoi c'est important
C'est le modèle de référence de la planification d'entraînement. Sa propriété la plus précieuse est **émergente** : juste après une séance, la fatigue domine → performance basse (« cuit ») ; après quelques jours, la fatigue s'efface plus vite que la fitness → la performance **dépasse** le niveau initial. C'est la **supercompensation**, le fondement de la périodisation et du deload. On ne la code pas : elle émerge de l'asymétrie des deux constantes de temps.

### Comment c'est utilisé dans Atlas
`BanisterModel` (domain service pur) porte `decay`, `applyStimulus`, `availablePerformance`. L'asymétrie vit dans **(a)** `τ_fatigue ≪ τ_fitness` et **(b)** `k2 > k1` — la **même** impulsion `S` est ajoutée à fitness ET fatigue (asymétrie en sortie, pas en entrée). C'est le Banister classique, plus défendable que de pondérer l'entrée.

### Exemple minimal hors Atlas
Deux comptes bancaires qui reçoivent le même dépôt mais avec des taux de « fuite » différents : l'un fuit à 2 %/jour, l'autre à 14 %/jour. Quelques jours après un gros dépôt commun, le compte qui fuit vite est presque vide, l'autre encore plein → leur **différence** (ta « performance ») est devenue positive alors qu'elle était nulle au moment du dépôt.

### Pièges classiques
- Croire qu'il faut pondérer l'**entrée** différemment pour fitness et fatigue → non, l'asymétrie est portée par τ et k.
- Oublier que la performance peut être **négative** juste après une séance (c'est correct, pas un bug).

### Pour aller plus loin
Banister, E.W. (1975), *A systems model of training for athletic performance* ; Calvert et al. (1976). Vue accessible : articles « Fitness-Fatigue model » de Stronger By Science.

---

## Concept 2 : Court terme vs long terme — et le dip de fitness en deload est une *feature*

### Définition
Atlas modélise **deux temporalités** distinctes :
- **Fitness/Fatigue** : adaptation aiguë court terme (jours/semaines), qui monte ET redescend.
- **CurrentStats** : capacités structurelles long terme (1RM réel, masse), qui se construisent lentement et ne se perdent **pas** vite.

### Pourquoi c'est important
Les confondre casserait toute la crédibilité. Un athlète qui prend une semaine de repos ne perd **pas** son 1RM structurel — il perd juste de la fitness aiguë, donc l'expression du jour baisse, mais le potentiel reste. C'est exactement la différence entre « être affûté » et « être plus fort ».

### Comment c'est utilisé dans Atlas
Au sprint 4 : `AthleteCondition` (module athletics) porte le Fitness/Fatigue dynamique ; `CurrentStats` (module roster) reste **stable** (Athletics le lit éventuellement, ne le modifie pas). Le test event-driven prouve la distinction : une séance fait bouger la forme, **les 1RM ne bougent pas**.
La simulation 12 semaines montre un **dip de fitness de ~12 % pendant le deload** : c'est **voulu**. La fitness de Banister *est* l'affûtage court terme, qui doit redescendre quand on réduit le volume. Allonger `τ_fitness` pour « adoucir » le dip brouillerait la distinction que tout le sprint cherche à établir. **Le dip rend visible que la fitness est court terme.**

### Exemple minimal hors Atlas
La forme d'un coureur la veille d'une course (taper) vs son VO2max. Le taper fait *baisser* le volume → la fraîcheur (court terme) remonte, mais la cylindrée (long terme) ne change pas en trois jours.

### Pièges classiques
- Vouloir « corriger » un comportement correct parce qu'il surprend (le dip). Toujours se demander : *qu'est-ce que cette variable modélise vraiment ?*

### Pour aller plus loin
Helms, *The Muscle and Strength Pyramids* (distinction préparation/réalisation) ; concept de **taper** en périodisation.

---

## Concept 3 : Forme récursive discrète + lazy compute

### Définition
Deux façons de calculer l'état Banister à l'instant t :
- **Convolution continue** : ré-intégrer *toute* l'histoire des séances passées à chaque lecture.
- **Forme récursive discrète** : ne stocker que l'état courant `(fitness, fatigue, lastUpdated)` et le décroître `exp(−Δt/τ)` depuis `lastUpdated` à chaque lecture/application.

### Pourquoi c'est important
La forme récursive est `O(1)` en mémoire et en calcul, et colle au **lazy compute** (ADR-006) : pas de scheduler qui tick tous les athlètes, on calcule l'état à la volée quand on le lit. L'exponentielle est **sans mémoire** : décroître en deux temps = décroître en un seul → le résultat est identique quel que soit le découpage, ce qui rend la récurrence exacte.

### Comment c'est utilisé dans Atlas
À l'écriture (handler d'event) : charger l'état → `decay` jusqu'au `performedAt` → ajouter le stimulus → sauver. À la lecture (use case) : charger → `decay` jusqu'à « maintenant » → calculer la performance. Aucune table d'historique n'est ré-intégrée.

### Exemple minimal hors Atlas
Le solde d'un compte à intérêts composés : tu ne rejoues pas toutes les transactions depuis l'ouverture pour connaître le solde — tu gardes le solde + la date, et tu appliques le taux sur le temps écoulé.

### Pièges classiques
- **Ré-intégrer l'historique à chaque lecture** (lent, et tentant quand on stocke déjà les séances).
- Décroître **vers le passé** : si `now < lastUpdated` (séance datée dans le futur), `exp(−Δt/τ)` avec Δt négatif *grossit* l'état — garder l'état tel quel dans ce cas.

### Pour aller plus loin
Notion d'« état suffisant » / propriété de Markov ; pattern *event-sourcing vs snapshot d'état*.

---

## Concept 4 : Calibration par simulation & honnêteté épistémique

### Définition
Valider un modèle scientifique non pas (seulement) par des assertions unitaires ponctuelles, mais par une **simulation longue** dont on juge la **forme de la courbe** contre les attentes de la littérature.

### Pourquoi c'est important
Un modèle peut passer 50 tests unitaires et produire une courbe absurde sur 12 semaines. Et surtout : **il n'existe aucune valeur de littérature** pour convertir une séance de musculation en impulsion de Banister (le modèle d'origine est endurance, impulsion = TRIMP cardiaque). Plutôt que d'inventer une valeur en la déguisant en science, on l'**assume explicitement** : « calibration Atlas par défaut, raffinée par simulation ». Modéliser honnêtement *là où la littérature s'arrête* est ce qui rend le modèle défendable face à une communauté experte.

### Comment c'est utilisé dans Atlas
`BanisterCalibrationSimulationTest` : 12 semaines, 4 séances/sem, deload en S7. Le test **imprime la trajectoire hebdomadaire** (fitness/fatigue/performance) pour qu'on la juge à l'œil au GATE 1, et asserte la **forme** (supercompensation après deload : `chute relative de fatigue > chute relative de fitness`) plutôt qu'une valeur ponctuelle. Au GATE 1, la courbe a montré la supercompensation **du premier coup** (+28 % de performance après le deload).
L'honnêteté est tracée dans ADR-028 et `sport-science.md` : chaque constante est soit sourcée (Banister/Calvert pour τ), soit marquée « assumée » (le mapping stimulus, NORM).

### Exemple minimal hors Atlas
Tester un moteur physique de jeu : tu ne vérifies pas seulement « la balle tombe » (unitaire), tu lances une simulation de 10 s et tu regardes si la **trajectoire** parabolique a la bonne allure.

### Pièges classiques
- Asserter une **valeur absolue** dépendante d'un paramètre d'échelle (ici NORM) → le test devient fragile. Asserter la **forme/les rapports**.
- Présenter une constante inventée comme « scientifique ». La marquer « par défaut, à raffiner ».

### Pour aller plus loin
Notion de *characterization test* ; calibration de modèles (least squares sur données réelles, post-MVP).

---

## Concept 5 : Phasing d'un modèle complexe — dérisquer l'architecture avant la science

### Définition
Plutôt que de tout construire d'un coup, on découpe par **source de complexité** et on valide chaque couche isolément, avec un gate. Ici : Banister nu (stat globale) **d'abord**, raffinement par muscle + génétique + progression structurelle **ensuite** (ADR-004).

### Pourquoi c'est important
Au sprint 4 on a validé le plus dur **architecturalement** (event-driven Athletics, lazy compute, persistence, distinction court/long terme) sur un modèle **mathématiquement simple**. Le plus dur **scientifiquement** (mapping muscle sourcé, calibration génétique) arrive au sprint 5, sur une architecture déjà éprouvée. Si un athlète progresse bizarrement plus tard, on saura que le problème est dans la *science* (la couche ajoutée), pas dans le *câblage* (déjà prouvé).
> Ce phasing avait failli être perdu : le prompt initial sur-scopait (sprint 4+5 fusionnés). La **lecture critique contre ADR-004** (qui actait « stat globale au sprint 4 ») l'a rattrapé avant tout code. Illustration nette du rôle des ADR comme garde-fous de décision.

### Comment c'est utilisé dans Atlas
`AthleteCondition` porte un `FitnessFatigueState` global ; le sprint 5 enrichira `FitnessFatigueState` (par `MuscleGroup`) **sans changer la frontière de l'aggregate**. L'individualisation génétique (`baseRecoveryRate`, `trainingResponseSensitivity`) est branchée nulle part au sprint 4 — annoncée sprint 5.

### Exemple minimal hors Atlas
Construire un compilateur : d'abord un langage jouet (lexer→parser→eval) bout en bout, puis enrichir la grammaire — pas l'inverse.

### Pièges classiques
- Suivre un prompt/une spec qui sur-scope sans le confronter aux décisions déjà prises.
- Construire la science avant d'avoir prouvé le câblage → on ne sait plus où est le bug.

### Pour aller plus loin
*Vertical slicing*, *walking skeleton* (Cockburn) ; ADR comme mémoire de décision (Nygard).

---

## Trois pièges vécus ce sprint (à retenir)

1. **Collision de bean Spring** — deux modules (roster, athletics) consommaient le même event `WorkoutLogged` et avaient chacun un `@Component WorkoutLoggedHandler`. Spring dérive le nom de bean du **nom de classe simple** → conflit au démarrage. Fix : **nommer le handler par son intention**, pas par l'event (`WorkoutStimulusHandler` côté athletics). Piège classique du modular monolith dès qu'un event a plusieurs consommateurs.

2. **Indice de Forme indépendant de NORM** — `NORM` est une échelle interne arbitraire. Mapper `fitness` brut en 0–100 aurait rendu l'affichage dépendant d'un paramètre invisible. La synthèse `50 + 50·(performance/fitness)` utilise un **ratio** qui annule NORM → mesure stable, et qui *signifie* quelque chose : « quelle proportion de mon acquis est exprimable aujourd'hui ».

3. **Events non redélivrés à un listener ajouté après coup** (le « faux bug d'accumulation »). En test manuel, une petite séance a semblé « écraser » l'historique (forme à 0, fitness==fatigue). Démarche **repro-avant-fix** : un test multi-séances a montré que l'accumulation **fonctionne** (vert). Vraie cause : les séances antérieures avaient été loggées **avant** que le module Athletics existe — Modulith ne redélivre pas les events passés à un listener nouvellement ajouté → seule la première séance *post-Athletics* avait créé une condition, de zéro → `(S,S)`. Comportement correct. Leçon : **suivre l'évidence contre l'hypothèse**, et localiser au lieu de balayer l'observation. (Note backfill pour la beta tracée en rétro.)

---

## Auto-évaluation

1. Pourquoi la même impulsion `S` est-elle ajoutée à fitness ET fatigue, et où vit l'asymétrie du modèle ?
2. Explique pourquoi la performance peut être négative juste après une séance, puis devenir positive sans nouvel entraînement.
3. Un deload fait baisser la fitness de ~12 %. Bug ou feature ? Justifie avec la distinction court/long terme.
4. Pourquoi la forme récursive discrète est-elle compatible avec le lazy compute, alors que la convolution continue ne l'est pas ?
5. Il n'existe pas de valeur de littérature pour « séance → impulsion Banister ». Comment Atlas gère-t-il ça honnêtement ?
6. Pourquoi avoir fait la stat globale avant le par-muscle, plutôt que l'inverse ?
7. Pourquoi l'indice de Forme 0–100 utilise un ratio `performance/fitness` plutôt que `fitness` brut ?
8. Deux modules consomment `WorkoutLogged` : quel problème Spring surgit, et comment le résout-on ?
9. Pourquoi un test multi-séances était nécessaire alors que le test event-driven (une séance) passait déjà ?
10. Qu'est-ce que Modulith ne fait PAS quand on ajoute un nouveau listener pour un event déjà émis par le passé ?
