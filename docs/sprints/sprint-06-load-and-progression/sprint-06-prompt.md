# Sprint 6 — Athletics : charge absolue + progression structurelle

> Prompt complet à coller dans Claude Code. **Boucle le moteur de simulation** : la charge entre enfin dans le calcul du stimulus (%1RM, poids de corps/leste), et les CurrentStats (1RM) **progressent** dans le temps — lentement, structurellement, sans casser la distinction court/long terme. Deux blocs liés, exécution en couches.

---

## ⚠️ Cadrage — lire en premier

Ce sprint réalise les sujets **long terme + charge** reportés des sprints 4 et 5. Deux blocs :

**Bloc A — Charge absolue / %1RM** : la charge entre dans le calcul du stimulus (résout le point d'ampleur composé/isolation tracé au sprint 5), avec la distinction **poids de corps / leste / charge externe** (la remarque traction du sprint 4). Saisie (PersonalTraining) ET calcul (Athletics) ce sprint.

**Bloc B — Progression structurelle des CurrentStats** : les 1RM montent lentement et quasi-irréversiblement quand le stimulus chronique le justifie. Inclut l'arbitrage d'**ownership** (les CurrentStats vivent dans Roster, Athletics les fait progresser) et le rôle des **axes génétiques structurels** (hypertrophyPotential, strengthAffinity, réservés depuis le sprint 5).

**L'ordre des deux blocs est à trancher en lecture critique** selon les dépendances réelles (la charge alimente-t-elle la progression, ou la progression peut-elle se faire sur le stimulus actuel d'abord ?).

---

## ⚠️ Le concept central — NE PAS casser la distinction court/long terme

C'est le pilier établi au sprint 4 et préservé au sprint 5. Ce sprint le pousse à sa conclusion en ajoutant une **troisième échelle de temps** :

| Composante | Échelle | Comportement | Sprint |
|------------|---------|--------------|--------|
| Fatigue | jours (τ≈7) | monte vite, redescend vite | 4 |
| Fitness | semaines (τ≈42) | monte modéré, redescend lent | 4 |
| **CurrentStats (1RM)** | **mois** | **monte très lentement, ne redescend quasi PAS à court terme** | **6** |

**Le piège mortel** : faire progresser les CurrentStats trop vite, ou les faire redescendre au repos. Si un athlète qui prend 2 semaines de repos "perd du 1RM structurel", on a re-mélangé les temporalités. Le 1RM structurel est quasi-irréversible à l'échelle de semaines (le détraining existe mais sur des mois). 

**Ce qu'on prouve ce sprint (gate conceptuel clé)** : sur un programme long, le 1RM monte progressivement ; sur un repos de quelques semaines, la Fitness baisse (forme) mais le 1RM structurel reste. Les trois échelles coexistent sans se confondre.

**Performance exprimée** = f(CurrentStats, Fitness, Fatigue) : ton potentiel structurel (CurrentStats) × ton état de forme du moment (Fitness/Fatigue). Au sprint 4 on avait un indice de Forme adimensionnel ; ce sprint, avec les CurrentStats qui progressent, on peut commencer à exprimer une performance en kg (1RM exprimable un jour donné = CurrentStats modulé par la forme) — à voir si on l'affiche ce sprint ou si on le garde pour Insights.

---

## Contexte projet (lecture préalable obligatoire)

1. `CLAUDE.md` — §4 (la distinction Fitness/Fatigue vs CurrentStats est LE sujet), §8 (phasing)
2. Les ADRs : ADR-004 (phasing), ADR-026 (ExerciseCategory, charge), ADR-027 (Athletics, AthleteCondition, clé AthleteId), ADR-028 (Banister), ADR-029 (granularité muscle), ADR-030 (mapping sourcé + le point ampleur tracé "résolu par la charge sprint 6"), ADR-031 (génétique court terme, axes structurels réservés sprint 6, fiber en réserve)
3. `docs/domain/sport-science.md` — le modèle complet à enrichir (charge, progression)
4. `docs/learning/sprint-04` et `sprint-05` — les patterns posés
5. Les RETROSPECTIVE 4 et 5 — les notes reportées (ampleur, poids de corps/leste, fiber, ownership CurrentStats)
6. Le code de **Roster** : `CurrentStats` (structure, où il vit dans Athlete), `Genetics` (`hypertrophyPotentialByMuscleGroup`, `strengthAffinityByPattern`, `fiberTypeProfile`), le `bodyweight` de l'athlète, le `RosterQueryPort` (findGeneticProfile, findMirrorAthleteId)
7. Le code de **PersonalTraining** : `ExerciseSet` (reps, weightKg nullable, rpe), `LoggedExercise`, le snapshot `WorkoutLogged` — c'est ici que la saisie poids de corps/leste va changer
8. Le code d'**Athletics** : `StimulusCalculator` (où la charge va entrer), `BanisterModel`, `MuscleStimulusMapping`, `GeneticModifiers`, `AthleteCondition`

Si contradiction avec un ADR Accepté, **arrête et signale** (réflexe sprint 4/5).

---

## Objectif du Sprint 6

À la fin :

**Bloc A**
1. La saisie d'une série distingue **poids de corps / lesté de X / charge externe de X** (PersonalTraining)
2. Le `StimulusCalculator` intègre la **charge réelle déplacée** et le **%1RM** (lu depuis les CurrentStats du pattern concerné)
3. Le point d'ampleur composé/isolation est **résolu** : un squat lourd génère plus de stimulus qu'un curl léger, naturellement via la charge

**Bloc B**
4. Les **CurrentStats (1RM) progressent** dans le temps selon le stimulus chronique accumulé, par pattern
5. La progression est **modulée par les axes génétiques structurels** (hypertrophyPotential, strengthAffinity) + éventuellement fiber
6. La progression suit des **rendements décroissants** (débutant progresse vite, avancé lentement, plafond vers le potentiel génétique)
7. L'**ownership des CurrentStats** est tranché et implémenté proprement (event / port / déplacement)
8. La distinction court/long terme est **préservée et prouvée** (repos → forme baisse, 1RM structurel reste)

Techniquement : changement de modèle de saisie (PersonalTraining), enrichissement du StimulusCalculator (charge), nouveau mécanisme de progression (accumulateur chronique), arbitrage d'ownership cross-module, mini-cours + devblog #7.

---

## Décisions à TRANCHER en lecture critique (AVANT le plan)

1. **L'ordre des deux blocs.** Dépendance à analyser : la progression structurelle (B) a-t-elle besoin de la charge (A) pour être crédible, ou peut-elle se faire sur le stimulus actuel d'abord ? Intuition : la charge alimente la qualité du stimulus, donc A avant B semble logique (la progression se nourrit d'un stimulus plus juste). Mais à confirmer sur le code et les dépendances réelles.

2. **L'ownership des CurrentStats — LA grosse décision d'archi.** Les CurrentStats vivent dans Roster (ADR-019, dans l'entité Athlete). Athletics veut les faire progresser. Analyser les 3 options :
   - **(a) Event** : Athletics émet `CurrentStatsProgressed(athleteId, pattern, newValue)`, Roster l'applique à son Athlete. Respecte l'ownership (Roster reste maître de ses données), asynchrone, cohérence éventuelle. Mais : double sens d'events (PersonalTraining→Athletics ET Athletics→Roster), et Roster devient consommateur d'un event Athletics.
   - **(b) Port de commande** : Athletics appelle un `RosterCommandPort.progressCurrentStat(...)`. Synchrone, explicite. Mais : Roster expose une commande mutable (jusqu'ici RosterQueryPort n'était que lecture), et couplage plus fort.
   - **(c) Déplacer CurrentStats vers Athletics** : les CurrentStats deviennent partie de l'état dynamique d'Athletics. Cohérent (Athletics gère tout ce qui évolue), mais : gros refactor, et CurrentStats est aussi l'identité "force" affichée sur la fiche Roster (les 4 1RM des sprints 2-5).
   
   Analyser les implications de chaque option sur la séparation des bounded contexts, la cohérence, le couplage, et le refactor nécessaire. Recommander, justifier. C'est ADR-032. (Note : c'est exactement le type de décision qu'on a tranchée pour le workoutCount au sprint 3 — penser "source de vérité" et "qui possède quoi".)

3. **Le mécanisme de progression des CurrentStats.** Options à analyser :
   - **(a) Accumulateur + seuil** : un accumulateur de stimulus chronique par pattern ; quand il dépasse un seuil, le 1RM monte d'un pas, l'accumulateur se vide partiellement. Progression par "paliers".
   - **(b) Cible convergente** : le stimulus chronique définit un 1RM "mérité" vers lequel le 1RM réel converge lentement. Progression continue.
   - Recommander selon le réalisme et la sensation de jeu. Tracer dans ADR-033.

4. **Comment la charge entre dans le stimulus (formule %1RM).** Le stimulus passe de `reps × effort(rpe)` à intégrer la charge. Le %1RM (charge / 1RM du pattern) est la mesure standard d'intensité. Question : `f(%1RM)` linéaire, ou courbe (les zones 70-90% du 1RM sont les plus stimulantes pour la force) ? Et comment ça compose avec effort(rpe) qui capture déjà une intensité relative ? Attention au double comptage : RPE et %1RM sont corrélés (un RPE élevé ET un %1RM élevé disent tous deux "c'est intense"). Bien réfléchir à comment les deux coexistent sans se doubler. Tracer dans ADR-034.

5. **Distinction poids de corps / leste / charge externe — modèle de saisie.** Comment PersonalTraining représente ça ? Un champ "type de charge" (bodyweight / bodyweight+added / external) + la valeur ? Et comment la charge totale déplacée est calculée (poids de corps de l'athlète + leste, ou charge externe seule) ? Le poids de corps est dans le profil Roster — Athletics/PersonalTraining doit-il le lire ? Tracer.

---

## Réservation des numéros d'ADR

Réservés : ADR-032, ADR-033, ADR-034 (+ 035 si besoin).

| Numéro | Sujet |
|--------|-------|
| ADR-032 | Ownership des CurrentStats : comment Athletics fait progresser des données Roster (event/port/déplacement) |
| ADR-033 | Mécanisme de progression structurelle (accumulateur/cible) + rendements décroissants + plafond génétique |
| ADR-034 | Intégration de la charge / %1RM dans le stimulus + coexistence avec effort(rpe) sans double comptage |
| ADR-035 (si besoin) | Modèle de saisie poids de corps / leste / charge externe |

ADR à chaud, à son gate (discipline reprise au sprint 5, maintenir).

---

## Exécution en couches (méthode S4/S5)

L'ordre exact des couches dépend de l'arbitrage "ordre des blocs" en lecture critique. Structure probable (à confirmer) :

### Si Bloc A avant Bloc B (intuition du prompt) :

**Couche 1 — Saisie charge (PersonalTraining)** : modèle de saisie poids de corps/leste/externe, migration, UI logger. Gate : on peut logger une traction lestée de +40kg, un squat à 140kg externe, un dips au poids de corps, et la charge totale déplacée est correcte dans l'event.

**Couche 2 — Charge dans le stimulus (Athletics)** : le StimulusCalculator lit le %1RM (charge / CurrentStats du pattern) et l'intègre, sans double-compter avec effort(rpe). Le point d'ampleur est résolu. Gate : un squat lourd génère plus de stimulus qu'un curl léger ; la courbe de simulation reste crédible.

**Couche 3 — Progression structurelle** : accumulateur chronique par pattern, progression du 1RM, rendements décroissants, plafond génétique, modulation par axes structurels. Gate : sur 12-16 semaines le 1RM monte progressivement, ralentit en approchant du plafond ; un athlète à haute affinité progresse plus vite.

**Couche 4 — Ownership + intégration** : l'arbitrage CurrentStats implémenté (event/port/déplacement), event-driven complet, distinction court/long terme prouvée. Gate : flow complet (logger → forme évolue ET 1RM progresse sur la durée ; repos → forme baisse, 1RM reste).

Chaque couche son gate. Le **gate distinction court/long terme** (couche 4) est le gate conceptuel critique.

---

## Portée technique

- **PersonalTraining** : modèle de saisie de charge enrichi (type + valeur), migration, snapshot WorkoutLogged enrichi (la charge totale déplacée ou les éléments pour la calculer), UI logger
- **Athletics domain** : StimulusCalculator intègre %1RM ; nouveau `StructuralProgression` (accumulateur chronique par pattern) ; `GeneticModifiers` étendu aux axes structurels (hypertrophyPotential, strengthAffinity, fiber si tranché) ; la progression applique rendements décroissants + plafond
- **CurrentStats** : selon l'arbitrage ownership — soit un event consommé par Roster, soit un command port, soit un déplacement
- **Lecture du 1RM pour le %1RM** : Athletics doit lire les CurrentStats (charge/1RM). Via RosterQueryPort enrichi ? Dénormalisé comme la Genetics ? Attention : contrairement à la Genetics (immutable), les CurrentStats PROGRESSENT ce sprint — donc la dénormalisation immutable du sprint 5 ne s'applique PAS ici. C'est une donnée mutable, à lire fraîche. Réfléchir au pattern (cf. la leçon option D sprint 3 sur le mutable).
- **Persistence** : tables/colonnes pour l'accumulateur chronique, migration
- **Web + frontend** : afficher la progression du 1RM (les cartes 1RM existantes des sprints 2-5 deviennent dynamiques) ; éventuellement la performance exprimable en kg (forme × CurrentStats) — à décider
- **Migrations Flyway** : saisie charge, accumulateur, selon ownership

### Tests requis

- Saisie charge : poids de corps/leste/externe → charge totale correcte
- %1RM dans le stimulus : intégration correcte, pas de double comptage avec RPE, point d'ampleur résolu (squat lourd > curl léger)
- Progression : 1RM monte sur la durée, rendements décroissants, plafond génétique respecté, modulation par axes structurels prouvée
- **Distinction court/long terme (gate conceptuel)** : repos → Fitness baisse, CurrentStats inchangés ; programme long → CurrentStats montent
- Ownership : selon l'option, event consommé / port appelé / déplacement cohérent
- Lecture CurrentStats mutable fraîche (pas de staleness sur une donnée qui progresse)
- Event-driven complet (Scenario), isolation Modulith, régression (tous les tests S4/S5 verts), Testcontainers via AbstractIntegrationTest

### Coverage

80%+ sur athletics.domain (JaCoCo enforced).

---

## Règles d'architecture (rappel)

- DDD strict, domaine pur, VO auto-validants, immutabilité
- **Attention donnée mutable** : les CurrentStats progressent → la leçon "ne pas dénormaliser ce qui change" (option D sprint 3) s'applique, contrairement à la Genetics immutable du sprint 5
- Mappers manuels, JSONB si besoin
- Lazy compute (ADR-006), event-driven (ADR-023), @NamedInterface
- Constantes scientifiques sourcées (progression : rendements décroissants en force = littérature, ex. modèles de plateau ; %1RM zones = SBS/Helms)
- Honnêteté épistémique (le mapping stimulus→progression structurelle est une calibration Atlas, comme le reste)
- Testcontainers via AbstractIntegrationTest, ADR à chaud

---

## Definition of Done

- [ ] `./mvnw clean verify` (deux runOrder)
- [ ] Modulith verify + isolation
- [ ] Coverage athletics.domain ≥ 80%
- [ ] Saisie poids de corps/leste/externe fonctionne (PersonalTraining)
- [ ] %1RM intégré au stimulus sans double comptage RPE ; point d'ampleur résolu
- [ ] CurrentStats progressent sur la durée, rendements décroissants, plafond génétique
- [ ] Modulation par axes génétiques structurels prouvée
- [ ] Ownership CurrentStats tranché et implémenté (ADR-032)
- [ ] **Distinction court/long terme prouvée** (repos → forme baisse, 1RM reste) — gate conceptuel
- [ ] Lecture CurrentStats fraîche (pas de staleness sur donnée mutable)
- [ ] Frontend : progression du 1RM visible
- [ ] Flow navigateur complet
- [ ] Régression S4/S5 verte
- [ ] CI verte
- [ ] ADR-032, 033, 034 (+035 si besoin) à chaud
- [ ] sport-science.md enrichi (charge, %1RM, progression structurelle, plafond)
- [ ] `docs/learning/sprint-06-load-and-progression.md`
- [ ] Devblog #7
- [ ] RETROSPECTIVE sprint 6
- [ ] Glossaire à jour
- [ ] Récap format A
- [ ] Note sprint 7+ : ce qui reste (Insights/courbes, programmes pour athlètes virtuels, compétition...)

---

## Contexte métier (le pourquoi)

Ce sprint **boucle le moteur de simulation**. Jusqu'ici, les athlètes fluctuaient en forme (court terme) mais ne devenaient pas vraiment plus forts. Après ce sprint, ils **progressent structurellement** : un programme bien suivi sur des mois fait monter les 1RM, modulé par la génétique, avec des rendements décroissants réalistes. C'est la promesse complète d'un simulateur d'entraînement : non seulement "es-tu en forme aujourd'hui ?" mais "es-tu devenu plus fort cette saison ?".

Et la charge qui entre enfin résout la dernière approximation majeure : un squat lourd compte plus qu'un curl léger, naturellement. Le modèle devient pleinement crédible pour un lifter.

La distinction court/long terme, maintenue de bout en bout, est ce qui fait qu'Atlas est défendable scientifiquement : la forme yo-yote, la force structurelle se construit. Deux choses différentes, modélisées différemment. C'est l'aboutissement de la vision.

---

## Format de récap attendu

**Récap format A**.

**Mini-cours `sprint-06-load-and-progression.md`** (format C), concepts :
1. Trois échelles de temps (Fatigue/Fitness/CurrentStats) — comment les faire coexister sans les confondre
2. La charge dans le stimulus : %1RM, et éviter le double comptage avec le RPE (deux mesures d'intensité corrélées)
3. Progression structurelle : accumulateur chronique, rendements décroissants, plafond génétique (la loi du débutant vs avancé)
4. Ownership cross-module d'une donnée mutable (l'arbitrage CurrentStats — event/port/déplacement, et pourquoi la dénormalisation immutable du sprint 5 ne s'applique pas)
5. Boucler un modèle de simulation : de la forme à la progression, la vision complète

**Devblog #7** — sujet possible : "Trois échelles de temps dans un simulateur d'entraînement" (l'angle conceptuel fort : modéliser fatigue/forme/force structurelle sans les confondre) ou "Faire progresser un athlète sans casser la physiologie". Le bouclage du moteur est une belle histoire.

---

## Première instruction concrète

1. Lis les sources (CLAUDE.md §4, ADR-027/030/031, le code CurrentStats/Genetics/StimulusCalculator).
2. Lecture critique : tranche l'ordre des blocs (dépendances réelles), analyse les 3 options d'ownership CurrentStats (recommande + justifie), le mécanisme de progression, la formule %1RM (anti double-comptage RPE), le modèle de saisie charge. Signale les tensions.
3. **Point d'attention spécifique** : les CurrentStats sont MUTABLES (progressent) — la lecture pour le %1RM ne peut pas être dénormalisée comme la Genetics immutable du sprint 5. Réfléchir au bon pattern.
4. Propose le plan en couches (l'ordre dépend de l'arbitrage bloc A/B), avec les gates, dont le gate conceptuel "distinction court/long terme".
5. Attends validation du plan avant de coder.
6. Exécute couche par couche, ADR à chaque gate.

---

*Sprint 6 — Athletics charge absolue + progression structurelle. Boucle le moteur de simulation. Estimé 2-2.5 semaines (dense : 2 blocs liés, un arbitrage d'archi majeur, le changement de saisie). Le sprint 4 a posé Banister, le 5 l'a densifié par muscle + génétique, le 6 ajoute le long terme structurel + la charge.*
