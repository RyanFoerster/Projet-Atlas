# Rétrospective — Sprint 6 (Charge %1RM + progression structurelle du 1RM)

*Date : 2026-06-30*

## Objectif du sprint (rappel)

Fermer le moteur de simulation. Deux blocs complémentaires :

- **Bloc A — Charge** : faire entrer la **3ᵉ variable de dose**, l'intensité de charge (%1RM), dans le
  stimulus (jusqu'ici exclue, sprints 4–5). Implique une **saisie de charge typée** (poids de corps / lesté /
  externe). Résout le déséquilibre composé/isolation laissé ouvert au sprint 5.
- **Bloc B — Progression** : faire **progresser les `CurrentStats`** (le 1RM réel) dans le temps — montée
  lente, quasi-irréversible, vers un plafond génétique. Distincte de la fitness aiguë (qui dippe en deload).
  Avec arbitrage d'**ownership** (les CurrentStats vivent dans Roster, ADR-019, mais Athletics porte le modèle).

Exécution en couches isolées (1 = saisie charge, 2 = %1RM dans le stimulus, 3 = progression structurelle), la
Couche 3 elle-même sous-découpée (3a domaine, 3b ownership, 3c persistence + émission + frontend + gate
conceptuel).

## Résultat

- **Atteint, de bout en bout.** La charge entre dans la dose (`loadFactor(%1RM)` orthogonal à
  `effortFactor(rpe)`), le 1RM progresse (cible convergente + cliquet), et la **boucle d'auto-régulation se
  ferme pour de vrai** (1RM↑ → %1RM↓ → loadFactor↓ → stimulus↓), stable en calibration.
- **389 tests verts** (les **deux** runOrder — parité CI confirmée), JaCoCo `athletics.domain` **≥ 80 %**
  (garde `check-domain-coverage` au vert), isolation Spring Modulith verte **sans cycle**.
- **4 ADRs écrits à leur gate, à chaud** : 032 (ownership 1RM par event + résolution de cycle), 033
  (progression cible convergente + cliquet), 034 (charge %1RM dans le stimulus), 035 (saisie de charge typée).
- **Gate conceptuel prouvé** (Clock contrôlée) : squat 140 → plafond 195,5 ; 12 sem à 120 kg → **144,72 kg**
  (borné) ; 6 sem de repos → fitness **1,264 → 0,465** (e⁻¹ sur 1τ) **mais 1RM = 144,72 exactement**. Les
  trois échelles de temps coexistent sans se contaminer.
- **Calibration** : débutant **+19 kg/12 sem** (newbie gains), avancé **+7 kg**, écart génétique **×2,3**,
  tous sous plafond, trajectoire stable. Rendements décroissants et plateau **émergents** (non codés).

## Ce qui s'est bien passé

- **L'émergence comme objectif de design, atteinte.** Rendements décroissants, plateau à volume constant, et
  auto-régulation **tombent** d'un seul modèle continu (convergence exponentielle vers plafond + lecture
  fraîche du 1RM) — aucun `if trainingAge`, aucun « détecteur de plateau ». C'est la réussite conceptuelle du
  sprint : un modèle qui dit quelque chose de vrai produit gratuitement les bons comportements.
- **L'orthogonalité charge/effort, propre.** `loadFactor(%1RM)` et `effortFactor(rpe)` restent deux fonctions
  distinctes multipliées (tension mécanique vs proximité de l'échec). Les nullables encodent l'orthogonalité :
  un accessoire sans 1RM garde son crédit d'effort, perd son crédit de tension (loadFactor au plancher).
- **Le cycle Modulith résolu sans compromis.** Ownership par event (Athletics émet, Roster consomme), cycle
  `athletics ↔ roster` cassé en descendant **seulement le contrat** `CurrentStatsProgressed` dans
  `shared/events` — la **logique** (StructuralProgressionModel, cliquet) reste dans athletics. Ryan a nommé et
  écarté l'alternative (port de commande synchrone) comme **piège** violant CLAUDE.md §3.6.
- **Les deux régimes de lecture, côte à côte et explicites.** Plafond génétique (immuable → dénormalisé une
  fois) vs 1RM courant (mutable → relu frais chaque séance). La distinction n'est pas théorique : cacher le
  1RM aurait cassé l'auto-régulation. La mutabilité décide du régime, pas la perf.
- **Le cliquet à la bonne frontière.** Monotonie du 1RM garantie en **n'émettant que les hausses**
  (`mérité > courant`), pas en corrigeant après coup côté consommateur.

## Ce qui a coincé (et qu'on a tranché à temps)

- **`V013` : la migration silencieusement inopérante.** Le backfill du `loadType` naviguait `exercises` comme
  un tableau direct, alors que la colonne contient `{"exercises": [...]}` (objet). `WHERE` a matché **zéro
  ligne** → Flyway `success=true`, **rien backfillé**. Corrigé par `V014` (vrai backfill) **+ garde-fou de
  complétude** (`RAISE EXCEPTION` si une série reste sans `loadType`). Leçon actée : une migration de données
  doit **vérifier qu'elle a agi**, pas seulement s'exécuter sans erreur. (V013 laissée intacte — on ne modifie
  jamais une migration appliquée.)
- **Calcul ≠ affichage (le `103,0122747238`).** Le 1RM s'affichait à 10 décimales. Le réflexe « arrondir en
  base » était **le piège** : stocker arrondi à 103 rend tout gain `< 0,5 kg` invisible **à jamais** (le 1RM
  ne monterait jamais visiblement). Tranché : **pleine précision en stockage**, arrondi **seulement à
  l'affichage** — jalon entier (« 103 kg ») + **delta cumulé à 1 décimale** (« +3,0 kg ») pour rendre la
  progression lente perceptible dès la 1ʳᵉ séance.
- **La progression imperceptible après 2 séances.** Symptôme du même problème : sans le delta cumulé exposé,
  un gain de quelques centaines de grammes ne se voyait pas. Résolu en exposant la **baseline** (1RM de départ
  figé) sur le read Athletics → le front calcule `courant − baseline`. Pas de nouveau cycle (lecture pure).
- **Le déséquilibre composé/isolation du sprint 5 : résolu, comme prévu.** La charge absolue (140 kg de squat
  vs 20 kg de curl) redonne son ascendant au composé via `loadFactor` — exactement le mécanisme planifié au
  sprint 5, pas un facteur d'ampleur arbitraire. La note « à vérifier sprint 6 » de la rétro précédente est
  close.

## Notes tracées pour plus tard (pas une correction immédiate)

- **Scope structurel = patterns avec 1RM de référence.** Seuls les patterns ayant un 1RM dans `CurrentStats`
  (les gros lifts) progressent structurellement ; ROW/CHIN_UP sans 1RM de référence ne progressent pas. Tracé
  comme **choix conscient** dans ADR-033 §5, pas un oubli. À rouvrir si on matérialise un 1RM pour ces patterns.
- **`fiberTypeProfile` toujours réservé.** Aucun levier **distinct** n'a émergé côté structurel ce sprint (il
  resterait redondant). Transporté, prêt à réactiver si un mécanisme propre se présente.
- **Calibration `SCALE`/τ_chronic à re-éprouver en playtest.** `SCALE=20` et τ_chronic=90 j sont validés à
  l'œil de lifter sur simulation 12–16 sem ; un playtest long (plusieurs cycles enchaînés) pourra affiner.

## Ce qui part au sprint 7 (Insights)

Le moteur de simulation est **complet** (forme court terme sprints 4–5, charge + progression structurelle
sprint 6). Le sprint 7 ouvre la **lecture** : dashboards, courbes de progression (les `ConditionSnapshot`
append-only attendent depuis le sprint 4), détail par muscle (l'agrégation maillon-faible rouvrable y trouve
sa place), visualisation de la trajectoire 1RM vers le plafond.

## Sur la collaboration Claude ↔ Ryan

- **Le découpage en couches + sous-étapes a payé** sur le gate le plus lourd du sprint (Couche 3 : 3a domaine
  → 3b ownership → 3c persistence/émission/frontend/gate). Chaque effet validé isolément avant le suivant.
- **Le mode prof-élève sur une décision structurante** : l'ownership (event vs port synchrone) a été posé en
  question avant de coder ; Ryan a tranché Option A **et nommé** le piège de l'Option B — exactement
  l'objectif (savoir défendre le choix, pas juste l'appliquer).
- **Décision sur observation, pas anticipation** : la calibration (sim Python + test Java) a tranché `SCALE`,
  prouvé la borne sous plafond et la convergence de la boucle **avant** de committer.
- **Les défauts d'affichage attrapés au navigateur par Ryan** ont mené à la bonne règle (calcul ≠ affichage)
  plutôt qu'au mauvais réflexe (arrondir en base). Le pilotage navigateur par Ryan reste un filet précieux.

## Micro-améliorations des rétros précédentes : **tenues**

- « ADR à son gate, à chaud » (acquis sprint 5) : **respecté** — 032, 033, 034, 035 chacun écrit au moment de
  la décision.
- « Vérifier la parité des deux runOrder » : faite explicitement (alphabetical + reversealphabetical), les
  deux vertes — le TRUNCATE CASCADE (sprint 5) rend bien le nettoyage indépendant de l'ordre.

## Micro-amélioration pour le sprint 7

Écrire le **garde-fou de complétude dès la première version** d'une migration de données (pas seulement dans
le correctif). La leçon V013/V014 : tout backfill doit s'auto-vérifier d'emblée, pour échouer bruyamment plutôt
que réussir silencieusement.
</content>
