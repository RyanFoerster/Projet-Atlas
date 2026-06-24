# Rétrospective — Sprint 3 (PersonalTraining & event-driven Modulith)

*Date : 2026-06-25*

## Objectif du sprint (rappel)

Construire le module **PersonalTraining** : logger ses vraies séances IRL, et — surtout — faire que ça mette à
jour l'athlète **miroir** dans Roster via un **event Modulith**. En sous-texte, le vrai enjeu : **valider
l'architecture event-driven du modular monolith** sur un cas concret (isolation, durabilité, idempotence).

## Résultat

- **Atteint, de bout en bout.** Domaine riche (TDD), persistance JSONB, 3 endpoints REST, communication
  event-driven `PersonalTraining → Roster` prouvée, 4 pages frontend (logger dynamique compris).
- **268 tests verts** ; JaCoCo `*.domain.*` ≥ 80 % forcé (PersonalTraining domaine à 97 %) ; isolation Spring
  Modulith verte, **sans cycle**.
- **4 ADRs** (023 registry/outbox, 024 snapshot DTO, 025 option D, 026 ExerciseCategory) — tous bâtis sur des
  décisions existantes, l'event-driven étant la seule vraie nouveauté architecturale.
- **4 paliers tenus** : GATE A (modélisation), GATE B (contrat REST au curl/MockMvc), GATE C (event-driven,
  le gate critique), GATE D (visuel).
- **Smoke-test sur le stack live** : POST séance → event → miroir mis à jour via query port (option D),
  confirmé en conditions réelles.

## Ce qui s'est bien passé

- **L'event-driven validé du premier coup au GATE C.** Le flow end-to-end (`Scenario` API) est passé sans
  refactor. C'était le risque structurel n°1 du sprint, attaqué en premier — et l'isolation a tenu. Signal
  fort pour tout le reste du projet (Athletics, Insights consommeront le même pattern).
- **Le test négatif a été fait.** Consumer forcé en échec → event reste incomplet + séance loggée. Très peu de
  bases de code testent le *chemin d'échec* d'un event. C'est ce qui transforme la durabilité d'une affirmation
  en comportement prouvé (ADR-023).
- **Le désassemblage du registry.** Plutôt que de supposer le comportement du republish, on a lu les requêtes
  JPQL réelles (`javap -c -p` sur `JpaEventPublicationRepository` → `where completion_date is null`). Niveau de
  rigueur à garder pour les décisions structurantes.
- **Le logger UX.** SegmentedControl Composé/Accessoire + liseré gauche + Select qui swap : dense, clavier
  (Tab/Enter), lisible — du Football Manager pour saisir une séance. Validé visuellement, cross-browser inclus.

## Ce qui a coincé (et qu'on a tranché à temps)

- **`MovementPattern` à double emploi, repéré en lecture critique.** Le prompt suggérait d'ajouter `ACCESSORY`
  à l'enum partagé. En relisant *contre* le plan, on a vu que ça polluait Roster (un accessoire n'est pas un
  axe de force génétique). Refactor vers un sealed `ExerciseCategory` distinct (ADR-026), tranché **avant** de
  coder. La relecture adverse (leçon rétro sprint 2) a payé une deuxième fois.
- **Surprise `@NamedInterface` au premier `verify`.** « roster depends on non-exposed type WorkoutLogged ».
  Diagnostiqué correctement : **pas un cycle**, un défaut d'exposition. Première consommation cross-module via
  `api/` du projet → la convention n'avait jamais été outillée. Corrigé en déclarant la named interface.
- **Débat idempotence option A vs D.** La garde par `lastWorkoutSessionId` (option A) *mitigeait* mais ne
  couvrait pas le rejeu multi-gap. Co-décision : **option D** (ne pas dupliquer le compteur → problème
  supprimé). Décision d'archi prise ensemble, après vérification du comportement Modulith. Belle illustration
  du mode prof-élève.
- **Accroc `deleteAll()` sur état partagé.** Un test passait en isolation mais échouait dans la suite : mon
  `cleanUp` supprimait la table `users` partagée, cassant les FK des rosters d'autres tests. Corrigé en
  scopant par owner unique (pas de `@DirtiesContext` partout).

## À surveiller (réévaluation future, pas une correction immédiate)

- **Pattern de test à état partagé (Testcontainers singleton).** La base est unique entre classes de test.
  **Règle tenue** : scoper les données par owner/id unique → les résidus d'autres tests n'affectent pas les
  assertions. **Vigilance** : le jour où un test fait une assertion **globale** (`countAll`, `findAll`), elle
  sera polluée. Il faudra alors isoler (scope explicite, ou nettoyage ordonné respectant les FK).
- **Mapping `BodyRegion → MuscleGroup` (sprint 4).** Tracé dans ADR-026 : ~9/11 en correspondance directe, mais
  `BACK` est plus grossier que `BACK_UPPER`/`BACK_LOWER`, et `FOREARMS` n'a pas d'équivalent. À arbitrer quand
  Athletics calculera le stimulus d'hypertrophie d'un accessoire.
- **Variance perçue des Generic (hérité sprint 2).** Toujours en observation : élargir légèrement la base au
  sprint 4+ si les Generic se ressemblent trop après quelques scouts.
- **Purge de `event_publication`.** La table des publications complétées grossit ; prévoir une purge/archivage
  (`completion-mode`) si le volume le justifie. Non nécessaire au MVP.

## Ce qu'on change / confirme pour le Sprint 4

- **Athletics consommera `WorkoutLogged` exactement comme Roster.** Le pattern event-driven est prouvé
  (snapshot, `@ApplicationModuleListener`, idempotence, named interface). Il n'y a plus qu'à brancher la
  sport-science par-dessus.
- **Définir le mapping `BodyRegion → MuscleGroup`** (ADR-026) au moment du calcul de stimulus.
- **Promouvoir `RPE` (et peut-être `BodyRegion`) vers `shared`** si Athletics en a besoin (critère ADR-017).
- **Relecture adverse systématique du plan** : confirmée comme rituel (a évité la tension `MovementPattern`).

## Sur la collaboration Claude ↔ Ryan

- Le rythme **paliers + validation** (GATE A/B/C/D) a de nouveau attrapé des points réels : la modélisation
  `ExerciseCategory`, le mapping `BodyRegion`, l'option D, le RPE en demi-points, les spinners natifs.
- **Décisions d'archi co-construites avant de coder** (idempotence, `@NamedInterface`, option D) plutôt que
  tranchées en solo — exactement le mode prof-élève visé. Ryan a poussé sur le « quasi nulle » de l'option A,
  ce qui a mené à l'option D, supérieure.
- À garder : Ryan **pilote le navigateur** pour toute validation visuelle frontend (acté ce sprint) ; Claude
  fournit la checklist et dérisque le backend en curl sur le stack live.
