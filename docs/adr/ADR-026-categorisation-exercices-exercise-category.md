# ADR-026 : Catégorisation des exercices loggés via sealed interface `ExerciseCategory`

**Statut** : Accepté (GATE A du sprint 3 validé)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte

Le module PersonalTraining doit catégoriser chaque exercice loggé (un squat, mais aussi un curl, du
gainage, des mollets…). Au sprint 4, Athletics consommera ces catégories pour distribuer un
`TrainingStimulus` : un mouvement composé doit alimenter un **axe de force** (squat → pattern SQUAT), un
accessoire doit alimenter une **région musculaire** (curl → biceps).

La tentation initiale (cf. prompt du sprint) était de réutiliser directement
`MovementPattern` (kernel `shared`) et d'y ajouter une valeur `ACCESSORY`. Or `MovementPattern` a déjà un
rôle précis dans le module Roster : ce sont les **axes de force génétique** d'un athlète
(`Genetics.strengthAffinityByPattern`, `Map<MovementPattern, Double>`), parcourus par le générateur
procédural, stockés en JSONB et affichés par le panel génétique. Un accessoire (curl) **n'est pas un axe
de force génétique**. Ajouter `ACCESSORY` à l'enum partagé polluerait Roster :

- le générateur devrait produire une valeur de génétique pour un « pattern » qui n'en est pas un ;
- le JSONB des génétiques existantes gagnerait une clé incohérente ;
- le panel génétique afficherait un axe qui n'a pas de sens.

On est au point précis du projet où corriger ce choix de modélisation ne coûte rien (~2 h). Au sprint 4,
avec Athletics branché dessus, ce serait un refactor transverse. Tension tranchée **avant** de coder.

## Décision

`MovementPattern` (shared) reste **inchangé** (6 axes de force, zéro pollution Roster). La catégorisation
d'un exercice loggé est un **concept distinct**, propre à `personaltraining.domain`, modélisé par un
**sealed interface** :

```java
public sealed interface ExerciseCategory {
    record CompoundForce(MovementPattern pattern) implements ExerciseCategory {}  // squat, bench, DL, OHP, row, chin-up
    record Accessory(BodyRegion region)         implements ExerciseCategory {}  // curl, extension, gainage, mollets…

    default Optional<MovementPattern> movementPattern() {
        return this instanceof CompoundForce cf ? Optional.of(cf.pattern()) : Optional.empty();
    }
}
```

- Les records étant **imbriqués** dans l'interface scellée, la clause `permits` est **inférée** (inutile
  de l'écrire).
- `BodyRegion` est un **enum interne au module** au sprint 3 (anti-dette ADR-017 : on le promeut à `shared`
  seulement si Athletics en a besoin au sprint 4 — transverse à 2+ modules ET fondamental).
- `WorkoutSession.patternsCovered()` ne retourne que les `MovementPattern` des `CompoundForce` ; les
  accessoires n'ont pas de pattern de force (prouvé par un test du cas mixte : 2 composés + 3 accessoires
  → 2 patterns, pas 5).
- Le **pattern matching exhaustif** (`switch` sans `default`) est garanti par le compilateur : ajouter une
  variante (`Cardio`, `Mobility`…) fera échouer la compilation de tout traitement non mis à jour — la
  dette devient une erreur de compile, pas un oubli silencieux.

### Traçabilité anticipatrice — mapping `BodyRegion` ↔ `MuscleGroup` (à finaliser au sprint 4)

Au sprint 4, Athletics devra mapper la `BodyRegion` d'un accessoire vers un (ou des) `MuscleGroup`
(`shared`) pour calculer le stimulus d'hypertrophie (curl biceps → stimulus BICEPS). Les deux enums sont
**volontairement distincts** (un concerne la saisie d'une séance, l'autre la modélisation physiologique).
Le mapping est ~9/11 en correspondance directe, avec **deux frictions à arbitrer au sprint 4** :

| `BodyRegion` | `MuscleGroup` | Note |
|---|---|---|
| BICEPS, TRICEPS, SHOULDERS, CHEST, CORE, GLUTES, HAMSTRINGS, QUADS, CALVES | identiques | correspondance directe 1:1 |
| `BACK` | `BACK_UPPER` + `BACK_LOWER` | `BodyRegion` est plus **grossier** : un accessoire « dos » devra cibler `BACK_UPPER` par défaut (tirages/rowing), le bas du dos restant surtout l'affaire des composés (deadlift) |
| `FOREARMS` | *(aucun)* | pas d'équivalent dans `MuscleGroup` — décider au sprint 4 : ajouter `FOREARMS` à `MuscleGroup`, le replier sur un voisin, ou ne pas modéliser de stimulus hypertrophie pour les avant-bras |

Cette table n'est **pas** une décision de ce sprint : elle documente où la jointure se fera, pour qu'au
sprint 4 on ne (re)découvre pas la friction en plein calcul de stimulus. Même esprit que la stratégie
d'évolution du format JSONB (ADR-019) : on trace l'angle mort, on le résout au premier besoin réel.

## Alternatives rejetées

- **(A) Ajouter `ACCESSORY` au `MovementPattern` partagé** : pragmatique (~0 h), mais pollue Roster et
  mélange deux concepts (force génétique vs catégorie de saisie). C'est exactement le « tech debt qu'on
  regrettera » au sprint 4. Rejeté.
- **(C) Ne rien catégoriser au sprint 3, mapper « accessoire » sur un pattern existant** : sous-modélise,
  reporte le problème, et oblige un rétro-fit du modèle de séance au sprint 4. Rejeté.

## Conséquences

**Positives**
- `MovementPattern` reste sémantiquement pur ; aucune régression ni migration sur Roster.
- Modèle expressif et extensible (variantes futures sans toucher l'existant).
- Le sealed donne un calcul de stimulus **type-safe** au sprint 4 (switch exhaustif).

**Négatives / à surveiller**
- Léger surcoût de sérialisation : le sealed devra être **aplati** en discriminant pour le JSONB et pour
  l'event public (snapshot, ADR-024). C'est précisément le test le plus important de S2.
- Le mapping `BodyRegion` → `MuscleGroup` reste à définir (cf. table ci-dessus), avec deux cas non triviaux
  (`BACK`, `FOREARMS`).
