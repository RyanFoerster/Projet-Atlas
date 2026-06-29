# ADR-035 : Saisie de charge typée — poids de corps / lesté / externe

**Statut** : Accepté (GATE de la Couche 1 du sprint 6 validé)
**Date** : Sprint 6
**Décideur** : Ryan Foerster

## Contexte

Jusqu'au sprint 5, une série encodait la charge par un `Weight` **nullable** : `null` = poids de corps,
non-null = charge. Trois conséquences gênantes pour le sprint 6 (qui fait entrer la charge dans le stimulus
puis dans la progression structurelle) :

1. **Aucun discriminant de type de charge.** Impossible de distinguer une traction au poids de corps (~75 kg
   déplacés), une traction lestée +20 (~95 kg), et un tirage vertical 60 kg externe. Le seul signal était
   `weightKg == null`.
2. **La contribution du poids de corps était perdue** : une traction au poids de corps logguait `null` → 0
   charge → 0 volume. Or c'est une charge réelle.
3. La charge **totale déplacée** (avec poids de corps) ne pouvait pas être calculée sans connaître le poids
   de corps de l'athlète, qui vit dans Roster.

La Couche 1 résout **la saisie** (PersonalTraining). Le calcul de la charge totale et du %1RM est la Couche 2
(Athletics). Cet ADR ne décide que de la saisie et de ce qui traverse l'event.

## Décision

### 1. Un sealed `Load` dans le domaine PersonalTraining

`ExerciseSet` passe de `(reps, Weight weight, RPE rpe)` à `(reps, Load load, RPE rpe)`, où `Load` est un
**sealed interface** auto-validant à trois variantes :

- `Bodyweight` — poids de corps pur, **aucune** valeur de charge (singleton sans état) ;
- `Weighted(Weight added)` — lesté, la valeur est la charge **ajoutée** ;
- `External(Weight weight)` — charge externe seule.

**Pourquoi un sealed et pas un `enum LoadType` + `Weight` nullable ?** Même raison qu'`ExerciseCategory` : un
discriminant + un champ nullable peuvent se désynchroniser (un `BODYWEIGHT` qui porte une valeur, un
`EXTERNAL` sans). Le type fermé rend ces états **irreprésentables à la compilation** et permet un `switch`
exhaustif aux frontières. Le poids de corps devient **explicite** (`Load.bodyweight()`), plus un `null`.

### 2. PersonalTraining logge la saisie, n'interprète pas la charge totale

`ExerciseSet.volumeKg()` ne compte que la charge **externe portée** (ajoutée pour un lesté, externe pour un
externe) ; le poids de corps **n'y est pas** modélisé comme charge. C'est volontaire : PersonalTraining **ne
connaît pas le poids de corps de l'athlète** (il vit dans Roster). La résolution « charge totale = poids de
corps (+ leste) » et le %1RM sont faits **côté Athletics** (Couche 2), seul module qui lit le bodyweight et
le 1RM. Séparation des responsabilités : *logger fidèlement ≠ interpréter*.

### 3. L'event porte la saisie BRUTE (primitifs, ADR-024)

`ExerciseSetSnapshot` passe de `(reps, weightKg, rpe)` à `(reps, String loadType, Double weightKg, Double
rpe)` avec `loadType ∈ {BODYWEIGHT, WEIGHTED, EXTERNAL}` et `weightKg` = charge ajoutée (lesté) / externe
(externe) / `null` (poids de corps). Le sealed `Load` est aplati au passage de l'event (comme
`ExerciseCategory`). Athletics résout bodyweight + 1RM pour calculer charge totale et %1RM — pas
PersonalTraining. Cohérent avec « queries via ports synchrones, side effects via events » : l'event reste
décorrélé du domaine consumer.

### 4. Saisie API tolérante, validée à la frontière

Le DTO `SetInputDto` gagne `loadType`. Un `loadType` absent (client simple) est **inféré** : `weightKg` null
→ poids de corps, sinon externe (compat ascendante, aucune rupture des clients existants). `WEIGHTED` et
`EXTERNAL` **exigent** une `weightKg` (sinon 400) ; un `loadType` inconnu est une saisie invalide (400).

### 5. Migration : backfill (pas reset), lecteur tolérant, garde-fou de complétude

`workout_sessions` est une donnée **IRL source** (les vraies séances du joueur — le hook de l'app), **non
recalculable** — contrairement aux `athlete_conditions` dérivées qu'on a pu reset au sprint 5. On **backfill**
donc le JSONB plutôt que de détruire les séances. Règle historique (la seule défendable automatiquement) :
`weightKg` null → `BODYWEIGHT`, non-null → `EXTERNAL` (le type lesté est une capacité nouvelle, aucune donnée
historique ne peut en être qualifiée).

Trois pièces, apprises **à la dure sur la donnée réelle** :

1. **`V013` était inopérante** : elle navigait la colonne `exercises` comme un **tableau** direct
   (`jsonb_typeof(exercises) = 'array'`), alors qu'elle contient un **objet** `{"exercises": [...]}` (le record
   `ExercisesJson` enveloppe la liste). Elle a matché zéro ligne → Flyway `success=true` **sans rien
   backfiller**. Le test d'intégration de la couche partait d'une base vierge (TRUNCATE) : toutes les séries
   avaient un `loadType`, donc le no-op était invisible. L'historique réel l'a révélé (crash navigateur).

2. **`V014` corrige** : navigation de la bonne structure (`exercises -> 'exercises' -> sets`), préservation des
   séries déjà typées (idempotent, ne dégrade pas un `WEIGHTED` écrit entre V013 et V014), et surtout un
   **garde-fou de complétude** : `RAISE EXCEPTION` si une série reste sans `loadType` après le backfill. *Une
   migration peut « réussir » en ne faisant rien — l'assert de complétude est ce qui transforme un no-op
   silencieux en échec bruyant.* `V013` reste telle quelle (on ne modifie jamais une migration appliquée).

3. **Lecteur tolérant** (expand/contract) : le convertisseur traite un `loadType` **null/absent** comme une
   donnée legacy à inférer (même règle), au lieu de planter sur le `switch` (un string-switch sur `null` lève
   `NullPointerException` *avant* tout `case`). Une seule ligne legacy ne doit jamais faire tomber tout
   l'historique. Le `default -> throw` reste réservé à une **vraie** valeur inconnue (corruption). C'est la
   moitié « lecteur » de l'expand/contract, que la migration seule n'assurait pas.

### 6. UI logger : type de charge par série

> **Révisé (sprint 6, Couche 2)** — la simplification « par exercice » initiale est **levée**. Le type de
> charge est désormais **par série** en UI comme en backend (qui l'était déjà). Motivation : cas d'usage réel
> du lifter — des tractions/dips lestés sur les premières séries, puis au poids de corps quand la fatigue
> arrive. Le par-exercice empêchait de logger ça fidèlement.

Chaque ligne de série porte un **mini-`atlas-select` compact** (taille `compact`, design system §4.11/§4.13 —
réutilisation d'une primitive, pas de nouveau composant) : `Externe / Lesté / Poids de corps`. La cellule
poids s'adapte **par série** : `kg` (externe), `+kg` (lesté), désactivée + « Poids de corps » (poids de
corps). **Héritage** : une nouvelle série reprend le type (et le poids) de la précédente — on choisit « Lesté »
une fois, ça se propage, et on passe la dernière série en « Poids de corps » d'un tap. Cas simple quasi sans
effort, cas mixte intra-exercice possible. Aucune migration (le backend était déjà par-série).

*Choix du `select` plutôt qu'un `SegmentedControl` (initial) ou un bouton cycle-icône : le segment 3 options
est trop large (~230px) pour la ligne dense ; le cycle-icône force à décoder une icône et cliquer en boucle.
Le mini-select montre l'état en permanence (label lisible) et change d'un tap — meilleure ergonomie de saisie
réelle.*

## Conséquences

**Positives** : états illégaux irreprésentables ; le poids de corps cesse d'être perdu ; PersonalTraining
reste découplé de Roster ; les trois types traversent fidèlement l'event (prouvé par test d'intégration :
traction +40 lestée, squat externe 140, dips poids de corps) ; séances réelles préservées.

**Coûts assumés** : le sealed `Load` impose un `switch` à chaque frontière (event, JSON, DTO in/out) — comme
`ExerciseCategory`, c'est le prix de la sûreté. L'UI par-exercice ne couvre pas le mix intra-exercice (rare,
réouvrable). La charge totale et le %1RM ne sont **pas** calculés ici — c'est la Couche 2 (ADR-034).

## Alternatives écartées

- **`enum LoadType` + `Weight` nullable** : plus léger mais autorise les états désynchronisés ; écarté pour la
  même raison qu'on a un sealed `ExerciseCategory`.
- **Résoudre le poids de corps côté saisie** (PersonalTraining lit Roster) : créerait une dépendance
  PersonalTraining → Roster pour un module aujourd'hui purement amont. Écarté : Athletics lit déjà Roster et
  centralise la physiologie.
- **Reset de `workout_sessions`** : simple mais détruit des données IRL réelles non recalculables. Écarté.
