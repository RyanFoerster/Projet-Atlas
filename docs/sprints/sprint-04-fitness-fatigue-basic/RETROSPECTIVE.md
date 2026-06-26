# Rétrospective — Sprint 4 (Athletics : Fitness-Fatigue basique)

*Date : 2026-06-26*

## Objectif du sprint (rappel)

Poser le **cœur scientifique** d'Atlas : le modèle Fitness-Fatigue de Banister, sur une **stat globale**
(ADR-004), branché en event-driven sur les vraies séances IRL. Le vrai enjeu : dérisquer l'**architecture**
(Athletics consomme `WorkoutLogged`, lazy compute, persistence de l'état dynamique, distinction
court/long terme) sur un modèle mathématiquement simple, avant d'empiler la complexité scientifique au
sprint 5.

## Résultat

- **Atteint, de bout en bout.** `BanisterModel` pur (TDD), `AthleteCondition` aggregate + persistence,
  event-driven `PersonalTraining → Athletics` prouvé, lazy compute, snapshots, endpoint REST, fiche
  athlète « Forme » (composant `ConditionGauge`).
- **313 tests verts** ; JaCoCo `athletics.domain` à **95,1 %** (gate 80 % enforced) ; isolation Spring
  Modulith verte, **sans cycle**.
- **2 ADRs** (027 Athletics module + clé `AthleteId` + `RosterQueryPort` ; 028 Banister formulation +
  constantes + calibration), `sport-science.md` créé.
- **GATE 1 du premier coup** : la courbe 12 semaines a montré la supercompensation après deload sans
  refactor. **GATE 2 validé** (navigateur, accumulation confirmée à l'œil).

## Ce qui s'est bien passé

- **GATE 1 crédible immédiatement.** La simulation 12 semaines a produit la supercompensation (+28 % de
  performance après le deload) au premier run. C'était le risque scientifique n°1 — la mécanique de base
  était juste, et la périodisation est **émergente** (non codée). Forte confiance pour la suite.
- **La distinction court/long terme, prouvée.** Le test event-driven asserte qu'une séance fait évoluer la
  forme **mais que les CurrentStats (1RM) ne bougent pas**. C'est le concept-clé du sprint, transformé en
  comportement testé, pas en affirmation.
- **L'event-driven Athletics a réutilisé le pattern sprint 3 sans friction.** Snapshot DTO, `@Application
  ModuleListener`, idempotence par garde monotone, `@NamedInterface` — tout était déjà éprouvé. Seule
  nouveauté : un **query port** côté Roster (`RosterQueryPort`, le premier de Roster) pour résoudre le
  miroir, et la promotion de `AthleteId` vers `shared` (ADR-017). L'investissement architectural du sprint 3
  a payé.
- **L'honnêteté épistémique de la calibration.** Reconnaître qu'aucune littérature ne convertit une séance
  en impulsion de Banister, et le tracer comme « calibration Atlas par défaut », est ce qui rend le modèle
  défendable. Posture à garder (matière du devblog #5).

## Ce qui a coincé (et qu'on a tranché à temps)

- **Le prompt initial sur-scopait (sprint 4 + 5 fusionnés).** La v1 décrivait « par groupe musculaire +
  mapping pondéré + génétique + progression CurrentStats » dès le sprint 4 — en contradiction frontale avec
  **ADR-004 (Accepté)** qui actait « stat globale au sprint 4, raffinement au sprint 5 ». La **lecture
  critique contre l'ADR** l'a attrapé **avant tout code**, et on a re-scopé (prompt v2). Belle illustration
  du rôle des ADR comme garde-fous de décision : sans ADR-004, on aurait dérivé sur deux sprints de travail
  en un, et perdu le bénéfice du phasing (dérisquer l'archi avant la science).
- **Le faux bug d'accumulation.** En test manuel, une petite séance a semblé « écraser » l'historique
  (forme à 0, `fitness == fatigue`). Démarche **repro-avant-fix** : un test multi-séances a montré que
  l'accumulation **fonctionne** (vert, y compris en répliquant le scénario exact). Vraie cause, confirmée
  par 3 requêtes SQL sur la DB de dev : les séances antérieures avaient été loggées **avant** la création du
  module Athletics — **Modulith ne redélivre pas les events passés à un listener ajouté après coup** → seule
  la première séance *post-Athletics* avait créé une condition, de zéro → `(S, S)` → « cuit ». Comportement
  correct. Leçon : **suivre l'évidence contre l'hypothèse de départ**, écrire le test de repro d'abord, et
  **localiser** la cause au lieu de balayer l'observation.
- **Collision de bean Spring** (mineure, attrapée au premier `verify`) : deux modules consommant
  `WorkoutLogged` avec chacun un `@Component WorkoutLoggedHandler` → même nom de bean → conflit. Fix :
  nommer par l'**intention** (`WorkoutStimulusHandler`). Piège classique du modular monolith multi-consumer.

## Notes tracées pour plus tard (pas une correction immédiate)

- **Backfill de l'historique antérieur au module.** Un athlète miroir avec des séances loggées *avant*
  l'existence d'Athletics démarre sa forme à zéro (les events passés ne sont pas redélivrés). Neutre tant
  qu'il n'y a pas d'utilisateurs réels. À la beta, décider : **rejouer l'historique** pour amorcer la
  condition, ou **assumer** « la forme démarre maintenant ». À arbitrer sprint 5+ / pré-beta.
- **Poids de corps / leste / charge externe.** L'UI et le calcul ne distinguent pas une traction au poids
  de corps, lestée, ou une charge externe — limite **assumée** au sprint 4 (la charge absolue est exclue du
  stimulus, le RPE tient lieu d'intensité relative). À traiter au **sprint 5**, quand la charge absolue / le
  %1RM entrent en jeu.
- **Purge `event_publication`** (hérité sprint 3) : toujours à prévoir si le volume le justifie. Non MVP.

## Ce qui part au sprint 5 (pour reprendre proprement)

- **Fitness/Fatigue par `MuscleGroup`** : enrichir `FitnessFatigueState` (de la paire globale vers une map
  par muscle) sans changer la frontière de l'aggregate `AthleteCondition`.
- **Mapping stimulus pondéré** : `MovementPattern → Set<MuscleGroup>` et `BodyRegion → MuscleGroup`
  (sourcé EMG / Stronger By Science), + ADR dédié. Le mapping `BodyRegion → MuscleGroup` était déjà tracé
  « à arbitrer » dans ADR-026.
- **Individualisation génétique** : moduler les constantes Banister par la `Genetics` (`baseRecoveryRate`,
  `trainingResponseSensitivity` — ce dernier portait déjà « à revérifier au sprint 4 », reporté).
- **Progression structurelle des CurrentStats** : le mécanisme distinct (accumulateur de stimulus chronique
  → montée lente quasi-irréversible du 1RM), avec l'arbitrage d'ownership (Athletics fait progresser des
  CurrentStats qui vivent dans Roster → event ? port ? déplacement ?).
- **Charge absolue / %1RM** dans le stimulus (et la distinction poids de corps/leste/externe).
- **Convexité de `effort(rpe)`** (« stimulating reps ») : considérée et écartée au sprint 4 faute de besoin
  observé ; à réévaluer si on simule des programmes à difficulté variable.

## Sur la collaboration Claude ↔ Ryan

- Le rythme **gate par gate** (GATE 1 modélisation → GATE 2 intégration) a de nouveau attrapé des décisions
  réelles avant de coder : formulation récursive discrète, asymétrie en sortie, indice de Forme
  adimensionnel, mapping 0–100 avec point neutre, couleur ambre (pas rouge) pour « cuit ».
- **La formule de stimulus montrée et discutée AVANT d'être câblée** (le moment de modélisation du sprint),
  puis ajustée ensemble au vu de la courbe au GATE 1 — exactement le mode prof-élève visé.
- **Repro-avant-fix** confirmé comme rituel sur les bugs : écrire le test rouge (ou vert) d'abord, suivre
  l'évidence. A évité de « corriger » du code sain.
- À garder : Ryan **pilote le navigateur** pour la validation visuelle ; Claude dérisque le backend par des
  tests d'intégration (ici, le test event-driven + le test d'accumulation, plus rigoureux qu'un smoke manuel).

## Micro-amélioration pour le sprint 5

- **Écrire l'ADR à son gate, à chaud.** ADR-027 (où vit le `FitnessFatigueState` + clé `AthleteId`) a été
  rédigé **à la clôture**, alors que la décision a été tranchée **au début** du sprint, juste après la
  lecture critique. La discipline posée au sprint 3 était « ADR à son gate, au moment de la décision » : un
  ADR écrit à chaud capture mieux le *raisonnement* et les alternatives réellement pesées que reconstruit
  a posteriori. Petit relâchement à corriger au sprint 5 — rédiger chaque ADR dès que la décision tombe.
