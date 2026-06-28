# Rétrospective — Sprint 5 (Athletics : granularité musculaire + génétique)

*Date : 2026-06-29*

## Objectif du sprint (rappel)

Densifier le modèle de forme du sprint 4 : passer d'une **stat globale** à une forme **par groupe
musculaire** (ADR-004), distribuée par un **mapping stimulus sourcé** (EMG/SBS), et **individualisée par la
génétique** de chaque athlète. C'est le sprint qui rentabilise le système génétique procédural du sprint 2 :
jusqu'ici un profil affiché, il pilote désormais vraiment la simulation.

## Résultat

- **Atteint, de bout en bout, en 3 couches isolées.** Forme par muscle (`Map<MuscleGroup, MuscleCondition>`),
  mapping pondéré sourcé (composés + accessoires), individualisation génétique (recovery → τ_fatigue,
  sensitivity → magnitude), le tout event-driven + lazy compute inchangés.
- **347 tests verts** (deux runOrder), JaCoCo `athletics.domain` à **96,5 %**, isolation Spring Modulith
  verte **sans cycle** (Athletics lit `roster.api` uniquement).
- **3 ADRs écrits à leur gate, à chaud** (029 GATE 1, 030 GATE 2, 031 GATE 3).
- **GATE 1** : structure + agrégation, supercompensation conservée en agrégé. **GATE 2** : distribution
  prouvée (squat → jambes/lombaires pas biceps), convexité tranchée. **GATE 3** : 2 génétiques → progressions
  cohérentes, prouvé end-to-end (la Genetics réelle du miroir arrive dans la condition).

## Ce qui s'est bien passé

- **Les 3 couches franchies proprement.** L'isolation (structure → mapping → génétique) a permis de valider
  chaque effet séparément. Quand l'agrégation paraissait dégénérée au GATE 1, on savait que c'était la
  symétrie de la sim, pas un bug de structure.
- **Le mapping validé par expertise lifter.** Faire revoir les pondérations (part fessiers du squat, biceps
  tirage vertical vs horizontal, split du soulevé) par un œil qui connaît la salle est un atout rare pour un
  modèle sport-science. C'est exactement la critiquabilité qui rend le modèle crédible.
- **La boucle génétique ↔ simulation, bouclée.** Deux leviers **distincts** et non-redondants vérifiés par
  simulation : recovery agit sur la *fraîcheur* (perf disponible), sensitivity sur la *construction*
  (fitness bâtie). L'aboutissement de la vision : la génétique procédurale (sprint 2) pilote enfin le
  Banister (sprint 4).
- **La dénormalisation immutable, propre.** Stocker les `GeneticModifiers` dans `AthleteCondition` (résolus
  une fois) est justifié *parce que* la Genetics est immutable — distinction nette avec l'option D du
  sprint 3, qui refusait de dupliquer du **mutable**. La règle « ne pas dupliquer ce qui change » ne
  s'applique pas à l'immuable.
- **Insight d'agrégation sur observation.** La divergence somme / maillon-faible n'apparaît que sous
  asymétrie **temporelle** (pas dans une séance uniforme). On l'a découvert en corrigeant une observation
  naïve, pas en raisonnant a priori.

## Ce qui a coincé (et qu'on a tranché à temps)

- **Le point ampleur composé vs isolation.** Avec somme=1 et charge exclue, un curl dépose plus sur ses
  biceps qu'un squat sur ses quads. Tentation d'ajouter un facteur d'ampleur tout de suite. Tranché :
  **limite assumée résolue par la charge au sprint 6** (le vrai mécanisme), pas un proxy arbitraire — et par
  muscle ce n'est même pas faux (le poids <1 du prime mover capture la modération). Observé sans absurdité.
- **La convexité de `effort(rpe)`.** Reportée du sprint 4, puis du GATE 1 au GATE 2 (« une variable à la
  fois » : ne pas la changer en même temps que la distribution). Tranchée au GATE 2 sur un tableau
  comparatif — la **ligne warmup (RPE 4)** décisive : le linéaire donnait 0.40 à un échauffement, faux.
  Adopté `(rpe−4)/6`, NORM recalibré, courbe re-roulée et re-validée avant de committer.
- **Le ripple de promotion `BodyRegion → shared`.** Mécanique mais large (event, mapper, DTO, persistence,
  tests). Tenu en gardant l'event en `String` (le handler traduit à sa frontière) plutôt que de propager
  l'enum partout — moins de couplage, ripple contenu.

## Notes tracées pour plus tard (pas une correction immédiate)

- **`fiberTypeProfile` à rouvrir.** Réservé ce sprint (il toucherait la résistance à la fatigue, redondant
  avec recovery → double-comptage). À réactiver si un levier **distinct** émerge (fiber → plafond de fitness,
  ou poids k1/k2, ou côté structurel au sprint 6). Transporté dans `GeneticProfile`, prêt.
- **Agrégation maillon-faible rouvrable.** Si un playtest sous distribution réaliste montre que la somme
  masque trop (jambes mortes mais indice haut car le haut compense), on rouvrira. Le maillon-faible relève
  du détail par muscle = sprint 7.
- **Facteur d'ampleur composé/isolation.** Si la charge (sprint 6) ne suffit pas à rééquilibrer, reconsidérer
  un facteur — sur observation.

## Ce qui part au sprint 6 (voir la note dédiée plus bas)

Progression structurelle des CurrentStats (+ ownership), charge absolue / %1RM (+ poids de corps/leste/
externe), axes génétiques structurels (hypertrophie, affinité), fiber éventuellement réactivé.

## Sur la collaboration Claude ↔ Ryan

- Le rythme **gate par gate** a de nouveau attrapé des décisions réelles avant de coder : règle d'agrégation
  par somme, pondérations du mapping (revue lifter **avant** câblage), axes génétique→Banister, convexité.
- **La table de pondération présentée AVANT câblage** (STEP 0 de la Couche 2), challengée puis ajustée
  (squat CORE 0.07→0.10) — exactement le mode prof-élève, et l'expertise lifter de Ryan comme atout.
- **Décision sur observation, pas anticipation** : ampleur composé/isolation et convexité toutes deux
  tranchées sur des simulations, pas par dogme.

## Micro-amélioration sprint 4 : **respectée**

Le micro-point de la rétro sprint 4 était « écrire l'ADR à son gate, à chaud » (ADR-027 avait glissé à la
clôture). Cette fois la discipline a **tenu** : **ADR-029 rédigé au GATE 1, ADR-030 au GATE 2, ADR-031 au
GATE 3**, chacun au moment où la décision tombait, avec le raisonnement et les alternatives à chaud. À
acter comme acquis.

---

## 📌 Note sprint 6 (reprise propre)

Le sprint 6 attaque la **progression structurelle** — le cœur long terme, complément de la forme court terme
des sprints 4–5.

1. **Progression des `CurrentStats`** : un accumulateur de stimulus chronique → montée **lente et
   quasi-irréversible** du 1RM (distincte de la fitness aiguë, qui dippe en deload). **Arbitrage d'ownership
   à trancher** : Athletics produit le stimulus mais les `CurrentStats` vivent dans **Roster** (ADR-019). Qui
   fait progresser quoi ? Event (`StructuralGainOccurred` ?) → Roster applique ? Port ? Déplacement des
   CurrentStats ? C'est le type de décision structurante à trancher en lecture critique, comme l'ownership de
   la condition au sprint 5 (ADR-027).
2. **Charge absolue / %1RM dans le stimulus** : la 3ᵉ variable de dose, jusqu'ici exclue. Elle **résout le
   déséquilibre composé/isolation** (un squat lourd reprend l'ascendant sur un curl léger). Implique la
   distinction **poids de corps / leste / charge externe** (la remarque « traction » : une traction au poids
   de corps, lestée, ou à la poulie ne chargent pas pareil).
3. **Axes génétiques structurels** : `hypertrophyPotentialByMuscleGroup` et `strengthAffinityByPattern`,
   réservés au sprint 5, **pilotent cette progression structurelle** — c'est leur place naturelle (long
   terme), cohérent avec la séparation court/long terme.
4. **`fiberTypeProfile` à rouvrir** si un levier distinct émerge dans ce cadre structurel.
5. **Le facteur d'ampleur composé/isolation** : vérifier que la charge le résout ; sinon, reconsidérer.
