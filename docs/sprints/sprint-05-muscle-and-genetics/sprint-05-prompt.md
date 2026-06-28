# Sprint 5 — Athletics : granularité musculaire + génétique

> Prompt complet à coller dans Claude Code. Densifie le modèle de forme posé au sprint 4 : on passe d'une stat globale à une forme **par groupe musculaire**, distribuée par un **mapping stimulus sourcé** (EMG/SBS), et **individualisée par la génétique** de chaque athlète. Structuré en 3 couches de validation isolées (la méthode qui a sauvé le sprint 4).

---

## ⚠️ Cadrage — lire en premier

Ce sprint réalise le raffinement annoncé par **ADR-004** : Fitness-Fatigue **par groupe musculaire** (le sprint 4 avait posé la stat globale). Il reprend 3 des sujets reportés du sprint 4, regroupés parce qu'ils enrichissent tous le **modèle de forme court terme** (Fitness/Fatigue) :

1. **Granularité musculaire** — `FitnessFatigueState` passe d'une paire globale à une `Map<MuscleGroup, MuscleCondition>`
2. **Mapping stimulus pondéré sourcé** — `MovementPattern → Set<MuscleGroup>` et `BodyRegion → MuscleGroup`, sourcé EMG/Stronger By Science
3. **Individualisation génétique** — moduler les constantes Banister par la `Genetics` de l'athlète

**Restent explicitement HORS scope (sprint 6)** : la progression structurelle des CurrentStats (1RM qui montent) + son arbitrage d'ownership, et la charge absolue / %1RM dans le stimulus + la distinction poids de corps/leste/externe. Ne pas les traiter ici. Si une tentation de les inclure apparaît, **signaler et s'arrêter** (comme au re-scope du sprint 4).

L'aggregate `AthleteCondition` garde sa frontière (on enrichit son contenu interne, on ne change pas qui possède quoi). CurrentStats restent dans Roster, stables, lus si besoin — leur progression est le sprint 6.

---

## Contexte projet (lecture préalable obligatoire)

1. `CLAUDE.md` — §4 (modèle métier), §8 (phasing : confirme le raffinement par muscle ici)
2. `docs/adr/ADR-004` (phasing global→muscle), ADR-026 (ExerciseCategory + mapping BodyRegion/MuscleGroup tracé "à arbitrer" — on l'arbitre ce sprint), ADR-027 (Athletics module, AthleteCondition, clé AthleteId), ADR-028 (Banister récursif discret, constantes, calibration)
3. `docs/domain/sport-science.md` — le modèle Banister, les constantes, les sources (créé sprint 4, à enrichir)
4. `docs/learning/sprint-04-athletics-and-banister.md` — le modèle posé, à étendre
5. `docs/sprints/sprint-04-fitness-fatigue-basic/RETROSPECTIVE.md` — les notes reportées, dont le micro-point "ADR à écrire à chaud, à son gate"
6. Le code de Roster — **notamment `Genetics`** (`hypertrophyPotentialByMuscleGroup`, `baseRecoveryRate`, `trainingResponseSensitivity` qui portait "à revérifier sprint 4", les axes de force) et l'enum `MuscleGroup` existant
7. Le code d'Athletics (sprint 4) — `BanisterModel`, `FitnessFatigueState`, `TrainingStimulus`, `StimulusCalculator`, `WorkoutStimulusHandler`, `AthleteCondition`
8. Le code de PersonalTraining — `ExerciseCategory` (CompoundForce(MovementPattern) | Accessory(BodyRegion)), le snapshot `WorkoutLogged`

Si contradiction avec un ADR Accepté ou ce prompt, **arrête et signale** — la lecture critique du sprint 4 contre ADR-004 a été exemplaire, même réflexe.

---

## Objectif du Sprint 5

À la fin :

1. Chaque athlète a un `FitnessFatigueState` **par groupe musculaire** (Map<MuscleGroup, MuscleCondition>) qui évolue selon Banister
2. Le `StimulusCalculator` **distribue** le stimulus d'une séance sur les bons MuscleGroup via un **mapping sourcé pondéré** (composés via MovementPattern, accessoires via BodyRegion)
3. Les constantes Banister sont **modulées par la Genetics** de chaque athlète (axes et ampleur déterminés en lecture critique + calibration)
4. L'**indice de Forme global** (la jauge 0-100 du sprint 4) est désormais **agrégé depuis les muscles** (UX inchangée, calcul par muscle dessous)
5. La **convexité de effort(rpe)** est réévaluée (linéaire vs convexe) à la lumière de programmes à difficulté variable
6. Tout reste **event-driven** (WorkoutLogged → Athletics) et **lazy compute** comme au sprint 4

Techniquement : enrichir les VOs et le BanisterModel sans changer la frontière de l'aggregate AthleteCondition, le mapping sourcé documenté, l'individualisation paramétrique, mini-cours + devblog #6.

---

## Exécution en COUCHES (la méthode qui a sauvé le sprint 4)

Trois sujets s'empilent. On les valide en isolation pour savoir, en cas d'anomalie, à quelle couche est le problème.

### Couche 1 — Granularité musculaire (mapping trivial temporaire) → 🟢 GATE 1
`FitnessFatigueState` devient `Map<MuscleGroup, MuscleCondition>`. Le BanisterModel s'applique par muscle. L'indice global est agrégé depuis les muscles. **Mapping temporaire trivial** (ex. chaque séance → un muscle générique ou une distribution uniforme) — le but de cette couche est de valider la STRUCTURE par muscle et l'AGRÉGATION, pas encore la distribution réaliste.
**GATE 1** : la forme par muscle s'accumule correctement (chaque MuscleCondition suit Banister), et l'agrégation vers l'indice global 0-100 est cohérente (à définir : moyenne ? pondérée ? muscle le plus fatigué ?). Simulation : un stimulus réparti produit des MuscleCondition crédibles + un indice global sensé.

### Couche 2 — Mapping stimulus sourcé → 🟢 GATE 2
Remplace le mapping trivial par le **vrai mapping pondéré**, sourcé EMG/Stronger By Science :
- `MovementPattern → Set<MuscleGroup>` pondéré (composés) : SQUAT → {QUADS, GLUTES, LOWER_BACK, ...} avec pondérations
- `BodyRegion → MuscleGroup` (accessoires) : mapping plus direct
**GATE 2** : un squat loggé distribue le stimulus sur quads/glutes/lower back dans des proportions défendables (pas sur les biceps), un curl biceps sur les biceps. La distribution est prouvée par test ET défendable par sources.

### Couche 3 — Individualisation génétique → 🟢 GATE 3
Les constantes Banister sont modulées par la `Genetics`. Les axes (quelle propriété génétique module quel paramètre) sont proposés en lecture critique selon les axes Genetics réellement existants. L'ampleur est calibrée par simulation.
**GATE 3** : deux athlètes de génétiques différentes, même programme → progressions différentes, cohérentes avec leur profil (ex. haut recovery → fatigue qui décroît plus vite → supercompense plus vite).

---

## Décisions prises en amont

| Décision | Choix | Note |
|----------|-------|------|
| Granularité | Par MuscleGroup | ADR-004 réalisé |
| Enum MuscleGroup | Réutiliser celui de Genetics, enrichir si besoin | Vérifier la liste, tracer les ajouts |
| Affichage forme | Indice global agrégé (jauge actuelle, calculée depuis les muscles) | UX inchangée, détail par muscle = sprint 7 Insights |
| Mapping | Sourcé et défendable (EMG/SBS), composés ET accessoires | Honnêteté épistémique : sourcé où la littérature existe, interprétation Atlas assumée et tracée où elle ne tranche pas |
| Génétique : axes | À proposer en lecture critique selon Genetics existant | Mapping génétique → paramètres Banister |
| Génétique : ampleur | À calibrer par simulation au GATE 3 | Fourchette qui rend la génétique sensible sans casser la comparabilité |
| Convexité effort(rpe) | Réévaluée ce sprint | Linéaire actuel vs convexe, décidé sur simulation de programmes variés |
| CurrentStats | Stables, lus si besoin | Progression = sprint 6 |

---

## Décisions à trancher en lecture critique (AVANT le plan)

1. **L'enum MuscleGroup existant couvre-t-il les besoins du stimulus ?** Il a été conçu pour l'hypertrophie (Genetics). Les groupes pertinents pour distribuer un stimulus de force/fatigue sont-ils les mêmes ? Manque-t-il des groupes (ischio-jambiers, bas du dos, deltoïdes...) ? Proposer les ajouts nécessaires, tracer pourquoi. **Attention** : enrichir l'enum MuscleGroup peut rippler dans Genetics (sprint 2) qui l'utilise pour `hypertrophyPotentialByMuscleGroup` — vérifier l'impact (un nouveau muscle doit-il avoir un potentiel génétique ? migration JSONB ?). C'est exactement le type de ripple cross-module qu'on a appris à anticiper.

2. **La règle d'agrégation muscles → indice global.** Comment passer de N MuscleCondition à un seul indice 0-100 ? Moyenne simple ? Pondérée par l'importance/taille du muscle ? Le muscle le plus fatigué tire vers le bas (logique "ta forme globale est limitée par ton maillon le plus fatigué") ? Proposer avec justification. C'est une vraie décision de modélisation.

3. **Quels axes Genetics modulent quels paramètres Banister.** Proposer le mapping selon ce qui existe vraiment dans Genetics. Exemples à challenger : `baseRecoveryRate` → τ_fatigue (récupère vite = fatigue décroît vite) ? `trainingResponseSensitivity` → magnitude du stimulus (répond plus fort) ? Les potentiels par muscle → un plafond/gain d'adaptation par muscle ? Et `trainingResponseSensitivity` portait un commentaire "à revérifier sprint 4" — c'est le moment de le revérifier.

4. **Convexité de effort(rpe).** Le sprint 4 a gardé `effort(rpe) = rpe/10` linéaire, en notant que la convexité (stimulating reps, Nuckols/SBS) serait réévaluée si on simule des programmes à difficulté variable. Maintenant qu'on peut distribuer par muscle, simuler un programme varié et regarder si le linéaire discrimine assez les séances dures des faciles. Décider : garder linéaire, ou passer convexe (ex. (rpe/10)² ou (rpe−4)/6 clampé). Décision sur observation au GATE concerné.

---

## Réservation des numéros d'ADR

Réservés : ADR-029, ADR-030, ADR-031.

| Numéro | Sujet |
|--------|-------|
| ADR-029 | Granularité musculaire : FitnessFatigueState par MuscleGroup + règle d'agrégation vers l'indice global + enrichissement de l'enum MuscleGroup (+ ripple Genetics) |
| ADR-030 | Mapping stimulus pondéré sourcé (MovementPattern → MuscleGroup, BodyRegion → MuscleGroup) + les sources + l'interprétation Atlas assumée |
| ADR-031 | Individualisation génétique : axes Genetics → paramètres Banister + ampleur calibrée + (réévaluation convexité effort si tranchée ici) |

Écrire chaque ADR **à son gate, à chaud** (micro-point relevé en rétro sprint 4 : ADR-027 avait glissé à la clôture — ne pas reproduire).

---

## Portée technique

- **Domain Athletics** : `FitnessFatigueState` → Map<MuscleGroup, MuscleCondition> ; `BanisterModel` applique par muscle + agrège ; `StimulusCalculator` distribue via le mapping ; nouveau(x) VO/service de mapping (`MuscleStimulusMapping` ou équivalent) ; `GeneticModifiers` (les paramètres Banister individualisés dérivés de Genetics)
- **Mapping** : tables de pondération (constantes en code, documentées + sourcées). MovementPattern → Set<(MuscleGroup, poids)>, BodyRegion → MuscleGroup
- **Lecture de Genetics** : Athletics lit la Genetics de l'athlète (via le RosterQueryPort ? nouvelle méthode ? ou la Genetics voyage-t-elle autrement ?) — à trancher en lecture critique (rappel : au sprint 4 Athletics ne lisait PAS Roster sauf pour résoudre le miroir ; lire Genetics est un nouveau besoin cross-module)
- **Persistence** : `athlete_conditions` doit stocker une Map par muscle (JSONB, pattern sprint 2/3/4) — migration. Les snapshots aussi (par muscle ou agrégé ? à décider — agrégé suffit probablement pour les courbes sprint 7, à tracer)
- **Web + frontend** : l'endpoint condition retourne désormais l'indice global agrégé (UX inchangée) ; vérifier que le composant Forme du sprint 4 fonctionne toujours. Pas de nouveau composant ce sprint (le détail par muscle = sprint 7)
- **Migration Flyway** : structure par muscle de athlete_conditions + snapshots

### Tests requis

- **BanisterModel par muscle** : décroissance/stimulus/perf par MuscleCondition, indépendance des muscles
- **Agrégation** : N MuscleCondition → indice global cohérent (la règle tranchée)
- **Mapping distribution** : chaque MovementPattern/BodyRegion distribue sur les bons muscles dans les bonnes proportions (cœur du GATE 2)
- **Individualisation** : génétiques différentes → GeneticModifiers différents → progressions différentes (cœur du GATE 3)
- **Simulation calibration** : programme 12-16 semaines, vérifier que la forme par muscle + l'agrégat restent crédibles ; tester un programme à difficulté variable pour la décision convexité
- **Event-driven** : WorkoutLogged → distribution par muscle → forme évolue (Scenario, comme sprint 4)
- **Régression** : les 2 tests d'accumulation du sprint 4 doivent toujours passer (adaptés à la structure par muscle)
- **Isolation Modulith** : Athletics lit Genetics via api/ uniquement
- **Intégration** via AbstractIntegrationTest (TRUNCATE CASCADE, runOrder épinglé)

### Coverage

80%+ sur `athletics.domain.*` (JaCoCo enforced).

---

## Règles d'architecture (rappel)

- DDD strict, domaine pur, VO auto-validants, immutabilité
- Mappers manuels (ADR-015), JSONB pour la Map par muscle
- Lazy compute (ADR-006), pas de scheduler
- Event-driven via api/events (ADR-023), @NamedInterface
- **Constantes scientifiques sourcées** — le mapping musculaire est le gros morceau de sourcing de ce sprint
- Honnêteté épistémique : sourcer ce qui existe, assumer et tracer ce qui n'existe pas
- Testcontainers via AbstractIntegrationTest
- Conventional Commits
- **ADR à chaud, à son gate** (pas à la clôture)

---

## Definition of Done

- [ ] `./mvnw clean verify` passe (les deux runOrder)
- [ ] Modulith verify + isolation (Athletics lit Genetics via api/ uniquement)
- [ ] Coverage athletics.domain ≥ 80%
- [ ] **GATE 1** : forme par muscle s'accumule + agrégation vers indice global cohérente
- [ ] **GATE 2** : mapping sourcé prouvé (squat → quads/glutes/lower back, curl → biceps), distribution défendable
- [ ] **GATE 3** : individualisation génétique prouvée (2 génétiques → 2 progressions cohérentes)
- [ ] Convexité effort(rpe) réévaluée et tranchée (linéaire conservé ou convexe adopté, justifié)
- [ ] Enum MuscleGroup vérifié/enrichi, ripple Genetics géré (migration si besoin)
- [ ] Indice global agrégé, jauge Forme du sprint 4 fonctionne toujours (UX inchangée)
- [ ] Snapshots adaptés (agrégé ou par muscle, tracé)
- [ ] Migrations Flyway
- [ ] Flow navigateur : logger une séance → la forme (agrégée) évolue selon les muscles travaillés
- [ ] Les 2 tests d'accumulation sprint 4 passent toujours (adaptés)
- [ ] CI verte
- [ ] ADR-029, 030, 031 rédigés **à leurs gates**
- [ ] `sport-science.md` enrichi (mapping musculaire + sources + individualisation génétique)
- [ ] `docs/learning/sprint-05-muscle-and-genetics.md` (mini-cours)
- [ ] Devblog #6
- [ ] `docs/sprints/sprint-05-*/RETROSPECTIVE.md`
- [ ] Glossaire à jour (MuscleCondition, GeneticModifiers, mapping...)
- [ ] Récap format A
- [ ] **Note sprint 6** : tracer ce qui reste (progression CurrentStats + ownership, charge absolue + poids de corps/leste)

---

## Contexte métier (le pourquoi)

Le sprint 4 a fait tourner le moteur Banister sur une stat globale. Le sprint 5 le rend **réaliste et individuel** : ta forme n'est plus un nombre unique, c'est un état par groupe musculaire (tu peux avoir les jambes cuites et le haut du corps frais), distribué par les muscles que tes exercices travaillent vraiment, et modulé par la génétique de l'athlète (deux athlètes ne répondent pas pareil au même programme).

C'est ce qui fait qu'un lifter sérieux regarde Atlas et dit "ok, ils ont compris" : la distinction quads/pecs/dos, le fait qu'un squat ne fatigue pas les biceps, le fait qu'un athlète "récupérateur" supercompense plus vite. Le mapping sourcé est ce qui rend le modèle défendable face à quelqu'un qui connaît la physiologie.

C'est aussi le sprint qui **rentabilise le système génétique du sprint 2** : jusqu'ici la génétique n'était qu'un profil affiché. Maintenant elle pilote vraiment la simulation. Le lien entre les deux systèmes (génétique procédurale + Banister) est l'aboutissement de la vision sport-science d'Atlas.

---

## Format de récap attendu

**Récap format A**.

**Mini-cours `sprint-05-muscle-and-genetics.md`** (format C), concepts :
1. De la stat globale à la granularité par muscle (enrichir un VO sans casser l'aggregate)
2. Mapping sourcé : traduire la science (EMG/activation) en pondérations défendables + honnêteté sur l'interprétation
3. Agrégation N→1 (des états par muscle à un indice lisible — quelle règle et pourquoi)
4. Individualisation paramétrique : comment la génétique module un modèle (le lien génétique procédurale ↔ Banister)
5. Réévaluer une décision sur observation (la convexité effort(rpe) : pourquoi on l'avait reportée, comment on tranche maintenant)

**Devblog #6** — sujet possible : "Distribuer un stimulus d'entraînement sur les muscles : du mapping EMG au code" OU "Quand la génétique pilote la simulation : individualiser un modèle physiologique". Le sourcing musculaire est un angle riche et original.

---

## Première instruction concrète

1. Lis les sources (notamment Genetics, l'enum MuscleGroup existant, le BanisterModel sprint 4).
2. Confirme la lecture + lecture critique : tranche les 4 décisions (enum MuscleGroup à enrichir + ripple Genetics, règle d'agrégation, axes génétique → Banister, comment Athletics lit Genetics), confirme l'ordre des couches, signale les tensions.
3. Propose le plan séquencé en 3 couches (Gate 1 par muscle → Gate 2 mapping sourcé → Gate 3 génétique), avec sous-étapes.
4. Pour la Couche 2, **présente le mapping sourcé AVANT de le câbler** (les pondérations + leurs sources), qu'on en discute — c'est la décision scientifique du sprint, et l'expertise lifter de Ryan est un atout pour le valider/challenger.
5. Attends validation du plan avant de coder.
6. Exécute couche par couche. ADR à chaque gate.

---

*Sprint 5 — Athletics granularité musculaire + génétique. Densifie le modèle de forme du sprint 4. Estimé 2 semaines. Le sprint 6 ajoutera la progression structurelle des CurrentStats + la charge absolue.*
