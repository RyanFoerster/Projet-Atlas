# Sprint 4 — Athletics : Fitness-Fatigue basique (le câblage scientifique)

> Prompt complet à coller dans Claude Code. Version respectant le phasing ADR-004 : **stat globale uniquement** au sprint 4 (le raffinement par groupe musculaire est le sprint 5). Objectif : dérisquer l'architecture (event-driven Athletics + lazy compute + distinction Fitness/Fatigue vs CurrentStats) sur un modèle mathématique simple, avant d'empiler la complexité scientifique au sprint 5.

---

## ⚠️ Cadrage — lire en premier

Ce sprint respecte **ADR-004 (Accepté)** et **CLAUDE.md §8** : sprint 4 = **une stat de forme globale par athlète** (pas par groupe musculaire). Le raffinement par `MuscleGroup` + mapping pondéré + individualisation génétique + progression structurelle des CurrentStats est le **sprint 5**.

Pourquoi ce phasing : on valide d'abord le plus dur **architecturalement** (Athletics consomme l'event, lazy compute, persistence de l'état dynamique, distinction court/long terme) sur un modèle **mathématiquement simple**. Le plus dur **scientifiquement** (mapping muscle sourcé, calibration génétique) vient au sprint 5, sur une architecture déjà éprouvée.

Dossier du sprint : `docs/sprints/sprint-04-fitness-fatigue-basic/`.

---

## Contexte projet (lecture préalable obligatoire)

1. `CLAUDE.md` — notamment §4 (modèle métier) et §8 (phasing des sprints : confirme stat globale au sprint 4)
2. `docs/adr/ADR-004` — le phasing Fitness-Fatigue (stat globale sprint 4 → par muscle sprint 5). **Décision cadre de ce sprint.**
3. `docs/adr/ADR-006` (lazy compute idle loop), ADR-023 (event-driven), ADR-026 (ExerciseCategory + mapping tracé pour sprint 5)
4. `docs/vision.md` — le réalisme scientifique comme pilier
5. `docs/domain/glossary.md`
6. `docs/learning/sprint-01` à `sprint-03`
7. RETROSPECTIVE sprint 1-3 — notamment l'infra de test (TRUNCATE CASCADE) à réutiliser, pas de cleanup par classe
8. Le code de Roster (Athlete, Genetics, CurrentStats) et PersonalTraining (WorkoutLogged event) — Athletics consomme les deux

Si contradiction avec ce prompt ou un ADR, **arrête et signale** (comme tu l'as fait pour le re-scope — c'était exactement le bon réflexe).

---

## Le concept scientifique central (à comprendre AVANT de coder)

Atlas modélise deux temporalités distinctes. **Les confondre casserait la crédibilité scientifique.** Même sur une stat globale, cette distinction doit être posée proprement dès le sprint 4.

### Fitness / Fatigue — court terme (jours/semaines)

Modèle de Banister (impulse-response). Chaque séance est une impulsion générant deux réponses antagonistes :
- **Fitness** : monte modérément, décroît LENTEMENT (constante de temps longue, ~42j littérature classique)
- **Fatigue** : monte fortement, décroît VITE (constante de temps courte, ~7j)

**Performance disponible** = `k1 · Fitness(t) − k2 · Fatigue(t)`.

Conséquence : juste après une grosse séance, Fatigue domine → forme basse. Après repos, Fatigue décroît plus vite que Fitness → supercompensation (forme qui dépasse le niveau initial). Fondement de la périodisation.

### CurrentStats — long terme (mois/années)

Le 1RM structurel et la masse musculaire se construisent lentement et ne se perdent PAS vite. Un athlète qui se repose une semaine ne perd pas son 1RM structurel.

### Au sprint 4 précisément

- **Fitness/Fatigue** : modélisés (globalement), évoluent à chaque séance, yo-yotent.
- **CurrentStats** : existent (déjà dans Roster depuis sprint 2), restent **STABLES** ce sprint. Ils ne baissent pas au repos, ne montent pas encore. La *progression* structurelle des CurrentStats est le **sprint 5**.
- **Ce qu'on prouve au sprint 4** : la distinction est nette. Une semaine de repos fait baisser la Fitness (donc la forme exprimable) mais NE TOUCHE PAS les CurrentStats. C'est le gate conceptuel clé.

---

## Objectif du Sprint 4

À la fin :

1. Chaque athlète a un **FitnessFatigueState global** (une paire Fitness/Fatigue) qui évolue selon Banister
2. Quand une séance est loggée (`WorkoutLogged`), Athletics calcule un **TrainingStimulus global** (magnitude agrégée depuis le volume de la séance) et l'applique
3. L'état est **lazy-computed** : décroissance calculée à la volée depuis `lastUpdated`, pas de scheduler
4. Un **snapshot** est créé à chaque `WorkoutLogged` (pour les courbes futures, sprint 7)
5. La distinction **Fitness/Fatigue (dynamique) vs CurrentStats (stable ce sprint)** est posée et prouvée
6. L'**athlète miroir progresse en forme depuis les vraies séances IRL** (bouclage du hook sprint 3)
7. La **fiche athlète affiche l'état de forme global** (Fitness, Fatigue, performance disponible)

Techniquement :
- Module **Athletics** complet (décision de modélisation à trancher en lecture critique)
- **BanisterModel** en domain service pur (forme récursive discrète, calibré littérature, testable)
- **Lazy compute** (ADR-006)
- **Calibration validée par simulation** (scénario 12 semaines, supercompensation visible)
- Mini-cours sprint-04 + devblog #5

---

## Décision de modélisation à trancher en lecture critique (AVANT le plan)

**Où vit le FitnessFatigueState ?**

- **Option 1** — Athletics nouveau module, aggregate dédié (ex. `AthleteCondition`) référençant `AthleteId`. État dynamique séparé de l'identité.
- **Option 2** — Enrichir l'Athlete de Roster. Gonfle Roster avec de la simulation.
- **Option 3** — Athletics module séparé possédant l'état dynamique ; Roster garde l'identité statique. Athlète complet = composition à l'affichage.

**Ce prompt penche Option 1/3** (séparation des bounded contexts : Roster = identité+génétique statique, Athletics = état dynamique). À challenger.

**Sur CurrentStats** : comme leur progression part au sprint 5, au sprint 4 ils restent dans Roster (statiques), Athletics les **lit** seulement (pour calculer la performance disponible = f(CurrentStats, Fitness, Fatigue) si pertinent à l'affichage). L'ownership de la *progression* CurrentStats sera tranché au sprint 5. Donc ADR-027 ce sprint = surtout "où vit FitnessFatigueState", pas l'ownership CurrentStats.

---

## Formulation Banister cible (validée)

**Forme récursive discrète** (colle au lazy compute + persistence d'un état) :

- On stocke `fitness` et `fatigue` courants + `lastUpdated`.
- Au moment d'appliquer un stimulus ou de query : on décroît exponentiellement depuis `lastUpdated` :
  - `fitness ← fitness · exp(−Δt / τ_fitness)`
  - `fatigue ← fatigue · exp(−Δt / τ_fatigue)`
- Puis on ajoute le stimulus (si nouvelle séance) :
  - `fitness ← fitness + stimulus`
  - `fatigue ← fatigue + stimulus` (avec magnitudes/poids potentiellement différents)
- Performance disponible = `k1 · fitness − k2 · fatigue`.

Pas de convolution continue (ré-intégrer tout l'historique) : coûteux et contradictoire avec le lazy compute. La forme récursive ne garde que l'état courant + le timestamp.

**Toutes les constantes (τ_fitness, τ_fatigue, k1, k2, magnitude du stimulus) sourcées en JavaDoc** (Banister 1975, Calvert 1976, valeurs classiques τ_fitness≈42j, τ_fatigue≈7j) et validées par la simulation. Là où la littérature ne tranche pas → "calibration par défaut, à raffiner" explicite.

---

## Décisions prises en amont

| Décision | Choix | Note |
|----------|-------|------|
| Granularité | **Stat globale** (pas par muscle) | ADR-004, raffinement = sprint 5 |
| Gestion du temps | Lazy compute + snapshot à chaque WorkoutLogged | ADR-006 |
| Calibration | Littérature + validée par simulation | Pas de génétique ce sprint (sprint 5) |
| CurrentStats | Stables (distinction posée, progression = sprint 5) | Restent dans Roster, Athletics lit |
| Formulation | Récursive discrète | Décroissance depuis lastUpdated + stimulus |
| Visualisation | État actuel (forme globale) | Courbes = sprint 7 |

---

## Exécution en couches (2 couches ce sprint, vs 5 dans la version fusionnée)

### Couche 1 — BanisterModel nu (domaine pur, TDD) → 🟢 GATE 1 (critique)
Le modèle Fitness/Fatigue mathématique pur, stat globale, constantes littérature fixes. Forme récursive discrète. Domain service testable en isolation.
**GATE 1** : une simulation de 12 semaines (stimulus régulier type 4 séances/semaine, avec un deload semaine 7) produit une courbe Fitness/Fatigue/Performance crédible — supercompensation visible après le deload. C'est de la pure validation mathématique, sans infra. **Ne pas avancer tant que la courbe n'est pas crédible.**

### Couche 2 — Intégration (event-driven + lazy compute + persistence + snapshots + web + frontend) → 🟢 GATE 2
Athletics consomme `WorkoutLogged` (comme Roster au sprint 3), calcule le TrainingStimulus global, applique au FitnessFatigueState. Lazy compute pour l'affichage. Snapshot à chaque event. Persistence. Endpoints. Fiche athlète "Forme". Athlète miroir progresse depuis les vraies séances.
**GATE 2** : flow complet navigateur (logger une séance → l'athlète miroir voit sa Fitness/Fatigue évoluer ; une semaine sans séance simulée → Fitness baisse un peu, Fatigue beaucoup, CurrentStats inchangés).

---

## Réservation des numéros d'ADR

Réservés : ADR-027, ADR-028. (Moins que la version fusionnée — le mapping muscle et la progression CurrentStats étant au sprint 5, leurs ADR aussi.)

| Numéro | Sujet |
|--------|-------|
| ADR-027 | Athletics module + où vit FitnessFatigueState (état dynamique vs identité statique) |
| ADR-028 | Modèle Banister : formulation récursive discrète, constantes, calibration par simulation |

Si un sujet émerge (ex. forme du stimulus global), prendre 029+. ADR-029/030 (mapping muscle, progression CurrentStats) sont **réservés pour le sprint 5**, ne pas les consommer ici.

---

## Portée technique

- Module `athletics/` : api / domain / application / infrastructure
- **Domain** : `BanisterModel` (domain service pur), `FitnessFatigueState` (VO : fitness, fatigue, lastUpdated), `TrainingStimulus` (VO : magnitude globale), `StimulusCalculator` (depuis le WorkoutLogged snapshot → magnitude ; au sprint 4, agrégation simple du volume, pas de distribution par muscle)
- **Application** : `WorkoutLoggedHandler` (@ApplicationModuleListener, comme Roster sprint 3), use case `GetAthleteConditionUseCase` (lazy compute à la query)
- **Infrastructure** : persistence de l'état + table snapshots, web (endpoint condition), config, `@NamedInterface` sur api/
- **Migration Flyway** : table `athlete_conditions` (état dynamique) + `condition_snapshots`
- Pas de table de mapping muscle ce sprint (sprint 5)

### Tests requis

- **BanisterModel pur** (TDD, le cœur) : décroissance exponentielle, application stimulus, supercompensation, performance disponible, monotonies attendues
- **Simulation de calibration** : scénario 12 semaines, assertions sur la forme de la courbe (Fitness monte lentement, Fatigue yo-yote, supercompensation après deload). Dans `athletics/test/calibration/`
- **Distinction court/long terme** : prouver qu'un "repos" (décroissance sans stimulus) baisse Fitness/Fatigue mais que CurrentStats (lu depuis Roster) est inchangé
- **Event-driven** : WorkoutLogged consommé par Athletics, miroir mis à jour (Scenario API)
- **Lazy compute** : l'état à la query reflète la décroissance depuis lastUpdated sans tick
- **Isolation Modulith** : Athletics consomme Roster/PersonalTraining via api/ uniquement
- **Intégration** persistence + web via AbstractIntegrationTest (TRUNCATE CASCADE déjà en place)

### Coverage

80%+ sur `athletics.domain.*` (JaCoCo enforced).

---

## Règles d'architecture (rappel)

- DDD strict, domaine pur, VO auto-validants, immutabilité
- Mappers manuels (ADR-015), JSONB si structure complexe
- Lazy compute (ADR-006), pas de scheduler
- Event-driven via api/events (ADR-023), @NamedInterface
- Constantes scientifiques sourcées, jamais inventées
- Testcontainers via AbstractIntegrationTest (TRUNCATE CASCADE, runOrder épinglé)
- Conventional Commits

---

## Definition of Done

- [ ] `./mvnw clean verify` passe (les deux runOrder)
- [ ] Modulith verify + isolation (Athletics → Roster/PT via api/ uniquement)
- [ ] Coverage athletics.domain ≥ 80%
- [ ] **GATE 1** : BanisterModel nu, simulation 12 semaines crédible (supercompensation après deload)
- [ ] **GATE 2** : flow event-driven complet (WorkoutLogged → miroir progresse en forme globale)
- [ ] Distinction prouvée : repos → Fitness/Fatigue baissent, CurrentStats inchangés
- [ ] Lazy compute : état calculé à la query depuis lastUpdated
- [ ] Snapshot créé à chaque WorkoutLogged
- [ ] Migrations Flyway athletics
- [ ] Fiche athlète frontend affiche la forme globale (Fitness, Fatigue, perf dispo)
- [ ] Flow navigateur complet
- [ ] CI verte
- [ ] ADR-027, 028 rédigés
- [ ] `docs/domain/sport-science.md` créé (modèle, constantes, sources)
- [ ] `docs/learning/sprint-04-athletics-and-banister.md` (mini-cours)
- [ ] Devblog `docs/blog/05-modele-banister.md`
- [ ] `docs/sprints/sprint-04-fitness-fatigue-basic/RETROSPECTIVE.md`
- [ ] Glossaire à jour
- [ ] Récap format A
- [ ] **Note sprint 5** : tracer explicitement ce qui est reporté (par muscle, mapping pondéré, génétique, progression CurrentStats) pour que le sprint 5 reprenne proprement

---

## Contexte métier (le pourquoi)

Athletics est le cœur scientifique d'Atlas — ce qui le transforme de "gestionnaire avec chiffres" en "vraie simulation". Au sprint 4 on pose les **fondations** : le moteur Banister tourne, l'athlète miroir progresse en forme depuis tes vraies séances, la distinction court/long terme est nette. Au sprint 5 on densifie (par muscle, génétique, progression structurelle).

Le bouclage du hook : au sprint 3 tes séances étaient comptées (passif). Au sprint 4 elles font **évoluer la forme** du miroir (actif). Même sur une stat globale, c'est le moment où Atlas devient une simulation vivante.

---

## Format de récap attendu

**Récap format A**.

**Mini-cours `sprint-04-athletics-and-banister.md`** (format C), concepts :
1. Le modèle Fitness-Fatigue de Banister (impulse-response, supercompensation)
2. Court terme vs long terme (Fitness/Fatigue vs CurrentStats) — la distinction qui fait la crédibilité
3. Forme récursive discrète + lazy compute (pourquoi on ne ré-intègre pas l'historique)
4. Calibration par simulation (valider un modèle scientifique par scénario long, pas juste assertions unitaires)
5. Phasing d'un modèle complexe (pourquoi stat globale d'abord, par muscle ensuite — dérisquer architecture avant science)

**Devblog #5** "Modéliser l'adaptation à l'entraînement : le modèle de Banister en Java" — sport-science + code, sujet rare. Angle fort : la calibration par simulation. Publiable r/java + r/weightroom/r/strength_training.

---

## Première instruction concrète

1. Lis les sources (CLAUDE.md §4/§8, ADR-004, littérature Banister).
2. Confirme la lecture + lecture critique : tranche la décision de modélisation (Option 1/2/3 pour FitnessFatigueState), confirme la formulation récursive discrète, signale toute tension résiduelle.
3. Propose le plan séquencé en 2 couches (Gate 1 BanisterModel nu → Gate 2 intégration), avec sous-étapes.
4. Pose les questions de clarification scientifiques (valeurs de constantes envisagées, forme exacte du stimulus global depuis le volume de séance).
5. Attends validation du plan avant de coder.
6. Exécute. **Gate 1 est critique** : la mécanique mathématique doit être validée (courbe crédible) avant toute intégration.

---

*Sprint 4 — Athletics Fitness-Fatigue basique (stat globale). Respecte ADR-004. Estimé 1.5-2 semaines (le câblage architectural + la calibration du modèle simple). Le sprint 5 densifiera par muscle + génétique + progression CurrentStats.*
