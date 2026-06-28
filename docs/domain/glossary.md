# Glossaire — Ubiquitous Language d'Atlas

> Ce document définit le **vocabulaire métier officiel** du projet. Tout code, toute doc, toute conversation utilise ces termes avec ces définitions précises. Lu par Claude Code et Cowork pour parler le bon langage. Lu par Ryan pour rester rigoureux.
>
> Règle DDD fondamentale : **un concept, un terme, partout**. Si on parle de "joueur" dans le code, on parle aussi de "joueur" dans la doc, dans les events, dans les commits. Pas de "user" dans le code et "joueur" dans la doc pour la même chose.

---

## Concepts joueur

### Player
L'humain qui joue à Atlas. Inscrit, authentifié, possède un compte. Dans le code, l'aggregate s'appelle `User` dans le module **identity** (terme technique standard pour l'auth), mais dans le langage métier on dit **Player**. Les events business diront `PlayerXxx`, les routes API `/players/...`.

### Coach
Rôle métier du Player dans le jeu. Le Player est le coach de son écurie. Dans certains modules (Programming, Roster), le terme **Coach** est utilisé pour insister sur cette identité métier. C'est une vue conceptuelle du Player, pas un aggregate séparé.

### CoachLevel
Niveau de progression du coach (XP cumulé, prestige). Géré par le module **progression**. Influence quelques mécaniques de gameplay long terme (recrutement, etc.) mais reste léger en MVP.

---

## Authentification (module identity)

### MagicLink
Entity du module **identity** : un lien à **usage unique** envoyé par email, valable un court instant,
qui authentifie son porteur quand il le consomme. Pas un aggregate root — il vit dans le contexte de
l'authentification d'un Player. On y stocke l'**email** (`userEmail`), pas un `UserId` : au moment de
l'émission, le Player peut ne pas exister encore (premier login = signup implicite).

### MagicLinkToken
Value object : le **secret** d'un `MagicLink`, un `UUID v7` (RFC 9562) transmis dans l'URL du lien. Type
distinct de `UserId` (un identifiant ≠ un secret d'authentification).

### MagicLinkExpirationPolicy
Domain policy stateless qui décide de la durée de vie d'un lien magique (**TTL 15 minutes** par défaut).
Isole la règle « +15 min » dans un objet nommé et testable plutôt que de la coder en dur.

### DomainException
Classe de base abstraite (kernel `shared`) de toutes les **exceptions métier**. Réservée aux violations
de règles métier (→ HTTP 400 + message) ; jamais aux erreurs techniques (→ 500, qui utilisent
`IllegalArgumentException`/`IllegalStateException`). Voir mini-cours Sprint 1, concept 3.

### tempSessionToken — *terme écarté*
Le prompt Sprint 1 envisageait un « tempSessionToken » pour porter l'email vérifié entre la consommation
du lien et l'onboarding (flow A). **Non retenu** : l'état « email vérifié, compte pas encore créé » est
porté par la **session HTTP** (cookie `JSESSIONID`), pas par un jeton applicatif. Le frontend ne stocke
rien — le cookie suffit. À ne pas réintroduire sans raison.

---

## Concepts athlète

### Athlete
Représente un athlète virtuel ou l'athlète miroir du Player. Possède une `Genetics`, des `CurrentStats`, et (à terme) un `FitnessFatigueState`, une `NutritionPhase`, un `TrainingHistory`.

> **Précision modélisation (sprint 2, ADR-019)** : dans le module **roster**, `Athlete` est une **entity interne** de l'aggregate `Roster` (pas de repository propre, créé via `Roster.addMirror`/`recruit`). Le futur module **athletics** portera le comportement dynamique (Fitness-Fatigue) ; le roster ne porte que l'identité, la génétique et les stats actuelles.

### MirrorAthlete
Conceptuellement, l'Athlete unique d'un Player qui est lié à ses séances IRL. Techniquement, c'est un Athlete avec `isMirror: true`. Pas un type séparé. Au plus un MirrorAthlete par Player.

### VirtualAthlete
Tout Athlete d'un Player qui n'est pas le MirrorAthlete. Sa progression se fait via simulation pure (programmes appliqués, calcul lazy). Termes "VirtualAthlete" et "Athlete" sont interchangeables hors contexte de comparaison explicite avec le MirrorAthlete.

### Genetics
Value object immutable porté par chaque Athlete. Définit le profil génétique unique : potentiels d'hypertrophie par groupe musculaire, affinités de force par pattern, taux de récupération, profil de fibres (Type I vs II), sensibilité de réponse à l'entraînement. Fixée à la création de l'Athlete, jamais modifiée.

### TrainingAge
Énumération du niveau d'expérience de l'athlète : `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `ELITE`. Influence les vitesses de progression (newbie gains, plateaux). Peut évoluer avec le temps.

---

## Concepts entraînement

### Workout
Une séance d'entraînement concrète. Pour le MirrorAthlete, c'est une séance IRL loggée par le Player. Pour un VirtualAthlete, c'est une séance simulée par le système. Composée d'`Exercises` et de leurs `Sets`.

### WorkoutSession
**Aggregate root autonome** du module **personaltraining** (sprint 3) : une séance IRL loggée par le Player = un aggregate à part entière (≠ Roster qui contient une collection — cf. mini-cours sprint 3). Immutable, identité `WorkoutSessionId` (UUID v7). Invariants : ≥ 1 exercice (`EmptyWorkoutSessionException`), `performedAt` pas dans le futur (vérifié à la création via `log()`, pas dans `reconstitute()`), durée plausible, notes ≤ 500. Méthodes calculées : `totalSets`, `totalReps`, `estimatedVolume`, `patternsCovered()` (uniquement les patterns des exercices composés).

### LoggedExercise
Value object riche d'un exercice loggé dans une `WorkoutSession` (personaltraining) : un `ExerciseName`, une `ExerciseCategory` (composé ou accessoire), et ≥ 1 `ExerciseSet` (copie défensive). Pas d'identité propre — n'a de sens que dans sa séance.

### ExerciseCategory (CompoundForce / Accessory)
**Sealed interface** catégorisant un exercice loggé (ADR-026) : soit `CompoundForce(MovementPattern)` (squat, bench, deadlift… — un axe de force), soit `Accessory(BodyRegion)` (curl, gainage… — une région). Volontairement **distinct** de `MovementPattern` réutilisé tel quel : un accessoire n'est pas un axe de force génétique. Permet un `switch` exhaustif (pivot pour le stimulus Athletics au sprint 4 : composé → pattern, accessoire → région).

### BodyRegion
Énumération des régions musculaires d'un exercice **accessoire** (`BICEPS`, `TRICEPS`, `SHOULDERS`, `CHEST`, `BACK`, `FOREARMS`, `CORE`, `GLUTES`, `QUADS`, `HAMSTRINGS`, `CALVES`). Interne au module personaltraining au sprint 3 ; promotion possible vers `shared` au sprint 4 si Athletics en a besoin. Distinct de `MuscleGroup` (mapping à définir au sprint 4, ADR-026 : `BACK` plus grossier, `FOREARMS` sans équivalent).

### Exercise
Un exercice spécifique réalisé dans un Workout. Ex : "Squat barre dos", "Bench press incliné", "Tirage horizontal". Chaque Exercise est mappé à un ou plusieurs `MovementPattern` et à un set de `MuscleGroup` impactés, avec des coefficients de pondération.

### Set
Une série dans un Exercise. Composée d'un nombre de répétitions, d'un poids (`Weight`), et optionnellement d'un `RPE`.

### MovementPattern
Énumération des patterns moteurs principaux : `SQUAT`, `BENCH_PRESS`, `DEADLIFT`, `OVERHEAD_PRESS`, `ROW`, `CHIN_UP`. Chaque exercice est mappé à un MovementPattern principal (et parfois secondaires).

### MuscleGroup
Énumération des groupes musculaires modélisés : `CHEST`, `BACK_UPPER`, `BACK_LOWER`, `QUADS`, `HAMSTRINGS`, `GLUTES`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `CALVES`, `CORE`. ~10-12 groupes en MVP.

### EnergySystem
Énumération des filières énergétiques : `STRENGTH` (ATP-PC, 1-5 reps), `HYPERTROPHY` (glycolyse, 6-15 reps), `ENDURANCE_ANAEROBIC` (HIIT), `ENDURANCE_AEROBIC` (zone 2 lipolyse), `POWER` (explosif).

### Weight
Value object représentant un poids. Encapsule une valeur `BigDecimal` (précision) et une unité (`KG` ou `LB`). Empêche les confusions d'unités et les valeurs négatives.

### RPE (Rate of Perceived Exertion)
Échelle de l'effort perçu, **1.0 à 10.0 par incréments de 0.5** (ex. 7.5, 8.5 — standard powerlifting). Value object auto-validant. Introduit dans `personaltraining.domain` au sprint 3 (optionnel sur une `ExerciseSet`) ; promotion possible vers `shared` au sprint 4 si Athletics en a besoin (critère ADR-017).

### OneRepMax
Value object représentant un 1RM. Précise s'il est `MEASURED` (testé en vrai) ou `ESTIMATED` (calculé par formule Epley/Brzycki). La confiance attribuée varie selon le type.

---

## Modèle Fitness-Fatigue

> **État sprint 5** : modèle de Banister **par groupe musculaire**, distribué par un **mapping pondéré sourcé** et **individualisé par la génétique**. Le sprint 4 avait posé la stat globale. La progression structurelle des CurrentStats part au **sprint 6** (ADR-004).

### TrainingStimulus
Value object : l'impulsion qu'une séance inflige à un muscle. Magnitude scalaire (`Σ reps × effort(rpe)` × `NORM`, charge absolue exclue — ADR-028), **distribuée par `MuscleGroup`** via le `MuscleStimulusMapping` (sprint 5). Input du `BanisterModel`.

### StimulusCalculator
Domain service stateless : `from(sets)` calcule la magnitude d'un bloc de séries ; `distribute(exercises, mapping)` la répartit sur les muscles (`Map<MuscleGroup, TrainingStimulus>`). `S = NORM × Σ reps × effort(rpe)`, `effort(rpe) = clamp((rpe−4)/6)` (seuil convexe doux, sprint 5, ADR-031), RPE absent → RPE 7 (0.5, neutre), `NORM = 0.013`. Le mapping séance→impulsion **n'a pas de littérature** → calibration Atlas assumée (ADR-028).

### ExerciseStimulus
Value object d'entrée du calcul : un exercice loggé réduit à sa **cible** (`MovementPattern` composé **ou** `BodyRegion` accessoire) et ses séries. Forme domaine de `LoggedExerciseSnapshot` (le handler traduit le nom de région de l'event en `BodyRegion` à sa frontière).

### MuscleStimulusMapping
Domain service : distribue le stimulus d'un exercice sur les `MuscleGroup` via des **tables de pondération sourcées** (somme = 1 par exercice — ADR-030). Composés (`MovementPattern → {muscle: poids}`, ex. squat → quads 0.42, glutes 0.30…) et accessoires (`BodyRegion → muscle`, cible directe sauf BACK → upper/lower 80/20, FOREARMS → biceps). Classement sourcé EMG/SBS, nombres = interprétation Atlas assumée.

### FitnessFatigueState
Value object immutable de l'état dynamique d'un Athlete : une `Map<MuscleGroup, MuscleCondition>` (sparse — muscle jamais travaillé = absent) + **un seul** `lastUpdated: Instant` partagé. Agrégé par **somme** (`totalFitness`/`totalFatigue`) vers l'indice global (sprint 5, ADR-029).

### MuscleCondition
Value object : la paire `(fitness, fatigue)` d'**un** groupe musculaire (sans horloge — le timestamp vit une fois au niveau du `FitnessFatigueState`). Brique élémentaire de la forme par muscle. Toujours ≥ 0.

### AthleteCondition
**Aggregate root du module athletics** : l'état dynamique d'un athlète, clé par `AthleteId`. Porte le `FitnessFatigueState` (par muscle) **et ses `GeneticModifiers`** (dénormalisés, résolus une fois à la création). Le fait évoluer via le `BanisterModel`. Distinct de l'identité statique (Roster) — composition à l'affichage (ADR-027).

### Fitness
Adaptation positive. Monte modérément à chaque stimulus, redescend **lentement** (décroissance exponentielle, `τ_fitness ≈ 42 j`). Court terme (semaines) — distincte de la force structurelle (`CurrentStats`).

### Fatigue
Effet aigu négatif. Monte fortement, redescend **vite** (`τ_fatigue ≈ 7 j`). Performance disponible = `k1·fitness − k2·fatigue` (k1=1, k2=2).

### Supercompensation
Phénomène **émergent** du modèle : après une séance, la fatigue (τ court) s'efface plus vite que la fitness (τ long) → la performance dépasse le niveau initial. Fondement de la périodisation et du **deload**. Validée par la simulation 12 semaines (GATE 1, sprint 4) — non codée, émergente de la dynamique.

### Indice de Forme
Synthèse de présentation 0–100 (50 = neutre) : `50 + 50·(Σperf/Σfitness)`, clampé, **agrégé par somme** sur les muscles (sprint 5, ADR-029). **Indépendant de l'échelle interne NORM** (le ratio l'annule). États : `Cuit` (<40), `Frais` (40–60), `Affûté` (>60). Affiché par `ConditionGauge` (design system §4.16).

### Agrégation (somme vs maillon-faible)
Règle de passage des N `MuscleCondition` à l'indice global unique. Atlas retient la **somme** (robuste, indépendante de NORM, pondérée par le volume). L'alternative « maillon-faible » (indice du muscle le plus cuit) est réservée au **détail par muscle (sprint 7)** : elle ne diverge de la somme que sous **asymétrie temporelle** (muscles entraînés à des moments différents) et sur-pénalise un seul groupe cuit pour la jauge globale (ADR-029).

### BanisterModel
Domain service stateless : le modèle de Banister en **forme récursive discrète**, appliqué **par muscle** — `decayedTo(state, modifiers, at)`, `applyStimulus(state, distributed, modifiers, at)` (decay + impulsion par muscle), `availablePerformance` (agrégé `k1·Σfitness − k2·Σfatigue`). La décroissance de la fatigue et la magnitude sont **modulées par les `GeneticModifiers`**. Zéro Spring/JPA, constantes sourcées/assumées (ADR-028).

### GeneticProfile
Snapshot **api-level** (record `roster.api`, types primitifs) du profil génétique d'un athlète, exposé par Roster à Athletics via `RosterQueryPort.findGeneticProfile(AthleteId)`. Ne fait pas fuiter le VO `Genetics` interne. Porte `baseRecoveryRate`, `trainingResponseSensitivity`, `fiberTypeProfile` (sprint 5, ADR-031).

### GeneticModifiers
Value object Athletics dérivé du `GeneticProfile`, **dénormalisé dans `AthleteCondition`** (résolu une fois — `Genetics` immutable). `recoveryRate` → module τ_fatigue (`τ_eff = 7/recovery`, **fraîcheur**) ; `stimulusMultiplier` → module la magnitude (**construction**). Le mapping `Genetics → GeneticModifiers` vit dans le handler (application), pas dans le domaine (ADR-031).

### Individualisation paramétrique
Principe : la `Genetics` (procédurale, immutable) **module les paramètres** du modèle de Banister par athlète, plutôt que de changer le modèle. Deux leviers distincts au sprint 5 (recovery → fatigue, sensitivity → impulsion) ; `fiberTypeProfile` réservé (redondance avec recovery) ; axes structurels (hypertrophie, affinité) réservés au sprint 6. C'est le lien entre génétique procédurale (sprint 2) et simulation Banister.

### Decay / Lazy compute
On ne tick jamais les athlètes (pas de scheduler, ADR-006) : l'état stocké `(fitness, fatigue, lastUpdated)` est **décru à la volée** à la lecture/application (`exp(−Δt/τ)`). La forme récursive ne garde que l'état courant + le timestamp — pas de ré-intégration de tout l'historique (convolution continue), trop coûteuse.

### ConditionSnapshot
Point daté `(fitness, fatigue, performance)` capturé **à chaque séance appliquée** (append-only). Alimentera les **courbes du sprint 7** (Insights). `performance` peut être négative (athlète « cuit »).

### CurrentStats
Value object porté par l'Athlete (module **roster**), capacités **structurelles** long terme : 1RM par MovementPattern (`OneRepMax`). **Sprints 4–5** : stables — Athletics ne les touche pas, un deload baisse la fitness mais PAS le 1RM (la distinction court/long terme, cœur des sprints 4–5). **Sprint 6** : leur progression (montée lente quasi-irréversible) sera pilotée par Athletics, avec arbitrage d'ownership (elles vivent dans Roster).

### AdaptationCalculator
Domain service **prévu sprint 6** : progression long terme des CurrentStats sous stimulus chronique, pilotée par les axes génétiques **structurels** (hypertrophie, affinité de force). Pas encore implémenté.

### InterferencePolicy
Domain policy qui modélise l'interférence aérobie-force (concurrent training effect). Si un cardio long suit un entraînement de force dans une fenêtre courte, les gains de force sont réduits de 20-30%.

### RecoveryPolicy
Domain policy qui détermine si un Athlete est prêt pour la prochaine séance, en fonction de son état Fitness/Fatigue, de son TrainingAge, de sa Genetics.

### NutritionPhase
Énumération : `BULK` (surplus calorique, favorise hypertrophie et force), `CUT` (déficit, favorise perte de gras, peut affecter récup et force), `MAINTENANCE`, `RECOMP` (limite, hypertrophie possible en déficit léger pour débutants). Influence l'AdaptationCalculator.

---

## Modèle de programmation

### Program
Aggregate root du module **programming**. Un programme d'entraînement structuré : 5/3/1, nSuns, PPL, Sheiko, etc. Possède une `ProgramStructure` (cycles, blocs, séances types), une `Duration` totale, des contraintes (équipement, niveau requis).

### ProgramTemplate
Un Program "modèle" qui peut être appliqué à un ou plusieurs Athletes. Les templates sont **débloqués** soit par défaut (basiques), soit en complétant un cycle IRL (déblocage par MirrorAthlete).

### ProgramInstance
Application d'un ProgramTemplate à un Athlete spécifique, sur une période donnée. Aggregate distinct du Template. Contient l'historique des séances effectuées dans le cadre de cette instance, l'adaptation des charges au fil du programme, le calendrier prévisionnel.

### ProgramCycle
Une unité du programme (typiquement 3-6 semaines). Un Program complet est composé de plusieurs ProgramCycles. La complétion d'un ProgramCycle peut déclencher un événement (déblocage, achievement).

### Mesocycle / Microcycle
Termes techniques de la périodisation. Microcycle = une semaine d'entraînement. Mesocycle = un bloc de 3-6 semaines avec un objectif spécifique (volume, intensité, peaking, deload). Présents dans le code pour les programmes avancés.

### Deload
Phase de récupération volontaire dans un programme : volume et/ou intensité réduits pour permettre la surcompensation. Capturée comme un type spécial de microcycle.

---

## Modèle de roster

### Roster
Aggregate root du module **roster** (le **seul** du module, ADR-019). Représente l'écurie d'athlètes du Player. Contient ses `Athlete` (entities internes), avec l'invariant **« au plus un athlète miroir »** protégé au niveau de l'aggregate. `owner_id` unique : un Player a au plus un Roster.

### Recruitment
Action de recruter un nouvel Athlete dans le Roster. En MVP : `/scout` propose un candidat, `/recruit` l'ajoute par son id.

### Rarity
Énumération de la **rareté** d'un athlète, modélisée comme de la **spécialisation**, pas un niveau global (ADR-020) : `GENERIC` (65 %, équilibré), `PROMISING` (25 %, un axe modeste), `SPECIALIST` (8 %, deux axes francs), `PRODIGY` (2 %, un axe exceptionnel). Personne n'est élite partout. Anti-gatcha : la rareté n'est pas « plus de puissance brute », c'est une spécificité.

### Phénomène
**Libellé UI français** du palier `PRODIGY` (le code garde l'enum `PRODIGY`). Choisi plutôt que « Prodige » (jugé enfantin) : registre sport-outlier, plus mature. Pour le miroir, la rareté n'est pas significative (toujours `GENERIC` par défaut technique, masqué à l'UI).

### AthleteCandidate
Value object décrivant un athlète **généré mais pas encore recruté** : nom, âge, profil, `Genetics`, `Rarity`, 1RM de base. Produit par l'`AthleteGenerator` au scouting.

### ScoutedCandidate
Entity du module roster : un `AthleteCandidate` **persisté temporairement** (id propre + TTL court, consommé une seule fois — même pattern que `MagicLink`, ADR-022). Garantit que `/recruit` part d'un candidat serveur (anti-forge), pas d'un objet renvoyé par le client.

### MirrorCreationRequest
Value object groupant les saisies du Player pour créer son miroir (nom, âge, poids, taille, sexe, 1RM mesurés). Le backend en dérive une `Genetics` hybride (les ratios force/poids orientent les affinités de force, ADR-021).

### TrainingHistory
Value object sur l'`Athlete` **miroir** (roster) : trace passive de la **dernière** séance IRL reçue — `lastWorkoutAt` + `lastPatternsCovered` (patterns de force uniquement). **Pas de compteur** : le nombre de séances a sa source de vérité dans personaltraining (option D, ADR-025), interrogé via `PersonalTrainingQueryPort` à l'affichage. Mis à jour par **écrasement monotone** (cf. *idempotence par écrasement monotone*) à la consommation de `WorkoutLogged`. Au sprint 4, Athletics enrichira ces données pour piloter le `FitnessFatigueState`.

### AthleteProfile
Sous-objet de l'Athlete affichant les informations publiques : nom, âge, poids, classe (powerlifter, strongman, bodybuilder, etc.), photo générée, historique des performances notables.

---

## Modèle de compétition

### Competition
Aggregate root du module **competition**. Représente un événement compétitif : un meet de powerlifting, un strongman event, un championnat de bodybuilding. Possède une date, une fédération, des catégories, un lieu, des règles spécifiques.

### CompetitionEntry
Inscription d'un Athlete à une Competition. Aggregate distinct. Contient les choix tactiques (premières barres annoncées, par exemple), le résultat final post-event.

### MeetPerformance
Résultat d'un Athlete dans une Competition. Score total, classement, podium, records battus. Génère un event PRBeaten si applicable.

### Federation
Énumération des fédérations modélisées : `IPF`, `USAPL`, `USPA`, `IFBB`, `NASS`, etc. Chacune a ses règles propres.

---

## Modèle d'analytics (Insights)

### Projection
Vue matérialisée dénormalisée dans le module **insights**. Maintenue à jour par event handlers qui écoutent les events des autres modules.

### TimeSeriesProjection
Sous-type de Projection qui stocke des données temporelles : progression de 1RM par semaine, tonnage par jour, etc.

### LeaderboardEntry
Entrée dans un classement (global, par cohort, par programme).

### YearlyWrapped / MonthlyRecap
Projection pré-calculée en fin de période, exposée en dashboard "fun" partageable.

---

## Events inter-modules (extrait, non exhaustif)

| Event | Module émetteur | Modules consommateurs typiques |
|-------|-----------------|-------------------------------|
| `PlayerRegistered` | identity | insights, roster |
| `PlayerLoggedIn` | identity | insights, progression |
| `WorkoutLogged` | personaltraining | athletics, insights |
| `ProgramCycleCompleted` | personaltraining | programming, progression, insights |
| `StimulusApplied` | athletics | insights |
| `AthleteStatsUpdated` | athletics | insights, progression |
| `PRBeaten` | athletics | insights, progression |
| `AthletePeaked` | athletics | competition |
| `AthleteOverreached` | athletics | competition (alerte) |
| `PhaseChanged` | athletics | insights |
| `ProgramInstanceCreated` | programming | athletics, insights |
| `RecruitmentCompleted` | roster | insights, progression |
| `CompetitionFinished` | competition | insights, progression |

### ApplicationEvent vs ModulithEvent (mécanique de publication)
Deux niveaux, souvent confondus :
- **Spring `ApplicationEvent`** : le mécanisme de publication standard de Spring (`ApplicationEventPublisher.publishEvent(...)`). Synchrone par défaut, en mémoire. C'est ce qu'on utilise aujourd'hui pour publier `PlayerRegistered`/`PlayerLoggedIn` depuis les use cases.
- **Spring Modulith event** : par-dessus, Modulith ajoute un **event publication registry** qui *persiste* les events consommés par un `@ApplicationModuleListener` (un `@TransactionalEventListener` durable). Cela permet la livraison fiable et le passage progressif à l'asynchrone entre modules (un consommateur qui plante ne perd pas l'event).

En clair : on **publie** des `ApplicationEvent` ; Modulith les **fiabilise** dès qu'un module les consomme via un listener dédié. **Devenu réel au sprint 3** : `personaltraining` publie `WorkoutLogged`, `roster` le consomme — premier event inter-module en production (ADR-023).

### WorkoutLogged (event)
Event public publié par **personaltraining** quand une séance est loggée, consommé par **roster** (sprint 3) puis **athletics** (sprint 4). Autosuffisant : transporte tout ce dont un consumer a besoin (owner, séance, exercices **aplatis en snapshots**), pour qu'il ne re-query jamais l'émetteur. Types primitifs / `shared` uniquement (UUID, `MovementPattern`), jamais le domaine interne (isolation, ADR-024). Publié **dans** la transaction du use case (*transactional outbox*).

### Transactional outbox
Pattern de fiabilisation des events : l'event est **persisté** (table `event_publication`) dans la **même transaction** que la donnée métier, puis marqué complété quand le consumer réussit. Géré par l'*event publication registry* de Spring Modulith (ADR-023). Garantit qu'une séance commitée a toujours son event tracé (re-livré au restart si le consumer échoue). Livraison **at-least-once**, pas exactly-once → le consumer doit être idempotent.

### Idempotence par écrasement monotone
Stratégie d'idempotence d'un consumer (ADR-025) : plutôt que d'incrémenter un état (fragile au rejeu), on **n'écrit que si l'event est strictement plus récent** que le dernier connu. Rejouer un event (ou en recevoir un dans le désordre) = no-op. Utilisé par `WorkoutLoggedHandler` pour le `lastWorkoutAt` du miroir. Idempotent **par construction**, sans mémoriser d'identifiant d'event.

### Named interface (Modulith)
Déclaration `@NamedInterface("api")` sur un package, qui le rend **exposé** aux autres modules. Par défaut Spring Modulith n'expose que le **package racine** d'un module ; un sous-package (`api`, `api.events`) reste interne sauf déclaration. Concrétise la règle « les modules externes n'importent que de `api/` » (CLAUDE.md), vérifiée par `AtlasApplicationModulesTest` + ArchUnit. Première utilisation : sprint 3 (`personaltraining.api`).

### Snapshot DTO (event)
Forme **aplatie** d'un objet du domaine, transportée par un event public (ADR-024). Évite d'exposer le domaine interne (un sealed `ExerciseCategory`, un VO `LoggedExercise`) dans le contrat public. Ex. `LoggedExerciseSnapshot` : discriminant `categoryType` + champs nullables, `BodyRegion` en `String`.

---

## Termes à ne PAS utiliser (faux amis)

Ces termes sont à éviter parce qu'ils créent de l'ambiguïté avec le vocabulaire défini :

- **User** : ne pas utiliser dans le langage métier (réservé à l'aggregate technique du module identity). Dire `Player`.
- **Account** : ne pas utiliser. Dire `Player`.
- **Fighter / Combatant** : ne pas utiliser. Dire `Athlete`.
- **Routine / Workout plan** : ne pas utiliser dans le code. Dire `Program` ou `ProgramTemplate`.
- **Workout history** : préférer `TrainingHistory`.
- **Game** : ne pas utiliser pour parler d'Atlas — Atlas est un produit, pas un "game". Utiliser "Atlas" ou "l'application".
- **Health / HP / Stamina** : ce sont des termes RPG génériques. Utiliser les vrais termes : `Fitness`, `Fatigue`, `Form`, `Readiness`.
- **Power Level** : pas utilisé. Préférer des stats explicites.

---

*Document vivant — étendu à chaque ajout de concept métier important. Maintenu par Ryan Foerster.*
