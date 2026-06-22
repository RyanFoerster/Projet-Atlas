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

## Concepts athlète

### Athlete
Aggregate root du module **athletics**. Représente un athlète virtuel ou l'athlète miroir du Player. Possède une `Genetics`, un `FitnessFatigueState`, des `CurrentStats`, une `NutritionPhase`, un `TrainingHistory`.

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
Synonyme de Workout dans le contexte du module **personaltraining** (séances IRL loggées). Aggregate root de personaltraining.

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
Échelle 1 à 10 de l'effort perçu. Value object validant la plage. Convertible vers/depuis une intensité relative (% 1RM approximatif).

### OneRepMax
Value object représentant un 1RM. Précise s'il est `MEASURED` (testé en vrai) ou `ESTIMATED` (calculé par formule Epley/Brzycki). La confiance attribuée varie selon le type.

---

## Modèle Fitness-Fatigue

### TrainingStimulus
Value object qui représente ce qu'un Workout "inflige" à un Athlete. Contient la distribution du volume effectif sur les `MuscleGroup`, l'intensité moyenne par `MovementPattern`, la charge sur les `EnergySystem`, et le RPE global. C'est l'input principal du `FitnessFatigueModel`.

### FitnessFatigueState
Value object porté par l'Athlete, représentant son état dynamique à un instant donné. Contient les valeurs de **fitness** et de **fatigue** par `MuscleGroup`, plus les valeurs aérobie/anaérobie globales, plus le `lastUpdated: Instant`.

### Fitness
Adaptation positive à long terme suite aux stimuli. Monte lentement (impulse à chaque stimulus), redescend lentement au repos (décroissance exponentielle, constante de temps ~30-50 jours). Stocké par MuscleGroup et globalement pour aérobie/anaérobie.

### Fatigue
Effet aigu négatif suite aux stimuli. Monte rapidement, redescend rapidement (constante de temps ~7-15 jours). La performance immédiate dépend de Fitness − k × Fatigue.

### Form / Readiness
Pas un attribut stocké, mais une mesure dérivée : `form(muscleGroup) = fitness - k × fatigue`. Positif = sur-compensation (peak), négatif = sur-fatigue. Calculée à la demande.

### FitnessFatigueModel
Domain service stateless qui implémente les équations de Banister. Prend un FitnessFatigueState, un TrainingStimulus, un Duration, et retourne un nouveau FitnessFatigueState. Aucune dépendance Spring/JPA.

### CurrentStats
Value object porté par l'Athlete, représentant ses capacités **structurelles** long terme : 1RM par MovementPattern (en `OneRepMax`), masse musculaire estimée par MuscleGroup, VO2max, capacité anaérobie, poids de corps, % de masse grasse. Distinct du FitnessFatigueState : un deload baisse la fitness mais pas la masse musculaire.

### AdaptationCalculator
Domain service stateless qui calcule comment les CurrentStats long terme évoluent en réponse à des stimuli cumulés sur des semaines/mois. Tient compte de la Genetics, du TrainingAge, de la NutritionPhase.

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
Aggregate root du module **roster**. Représente l'écurie d'athlètes du Player. Contient les Athletes actifs, l'historique des recrutements, des limitations (taille max selon le niveau du coach).

### Recruitment
Action de recruter un nouvel Athlete dans le Roster. En MVP, recrutement simple via liste de candidats générés. Post-MVP, mécanique riche type scouting.

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
