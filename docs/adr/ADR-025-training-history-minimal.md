# ADR-025 : TrainingHistory sans compteur (option D) — source de vérité unique et idempotence par construction

**Statut** : Accepté (GATE C du sprint 3 validé)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte

Quand Roster consomme `WorkoutLogged`, il doit refléter sur l'athlète miroir : (1) le **nombre** de séances
loggées, (2) la **dernière** séance (date + patterns de force couverts), pour l'affichage et, au sprint 4,
le calcul du stimulus.

La livraison Modulith est **at-least-once** (ADR-023) : un event peut être rejoué (republish au restart, ou
fenêtre commit/complétion). Un consumer qui ferait naïvement `workoutCount += 1` **fausserait le compteur**
sur rejeu — et c'est un compteur **visible par l'utilisateur** (« j'ai loggé 3 séances, mon miroir en
affiche 4 »). L'idempotence doit donc être réellement fiable, pas approximative.

## Décision — Option D : ne pas dupliquer le compteur

On a comparé trois familles de solutions :

| Option | Idée | Verdict |
|---|---|---|
| A — garde `lastWorkoutSessionId` | skip si l'event == dernier appliqué | **mitige** seulement : ne couvre pas le rejeu d'un event *antérieur* après qu'un autre soit passé (scénario multi-gap réel). Rejeté. |
| B — table inbox `(listener, sessionId)` | ledger des events traités | exactly-once strict, mais **duplique** le registry pour un simple compteur. Surdimensionné. Rejeté. |
| **D — compteur non dupliqué** | le count vit dans PersonalTraining ; Roster ne stocke que la dernière séance | **supprime** le problème au lieu de le mitiger. **Retenu.** |

### Pourquoi D (le raisonnement de fond)
> **A *mitige* le problème d'idempotence du compteur ; D le *supprime*.** Un compteur qu'on ne duplique pas
> ne peut pas diverger.

- **Source de vérité unique** : le nombre de séances EST le nombre de lignes dans `workout_sessions`
  (PersonalTraining). On l'expose via `PersonalTrainingQueryPort.countSessionsFor(owner)` et on l'interroge
  à l'affichage de la fiche du miroir (**composition backend**). `countByOwner` est toujours exact, par
  construction. Zéro dérive possible, zéro idempotence à gérer sur le count.
- **L'état résiduel côté Roster est idempotent par construction.** `TrainingHistory` ne garde que
  `lastWorkoutAt` + `lastPatternsCovered`, mis à jour par **écrasement monotone** : on n'écrit que si
  `event.performedAt` est **strictement postérieur** au `lastWorkoutAt` connu. Conséquence :
  - rejeu du dernier event (date égale) → no-op ;
  - rejeu / réception d'un event **antérieur** (désordre de livraison) → no-op (pas de régression de date) ;
  - nouvel event réellement plus récent → mise à jour.
  
  C'est idempotent **ET** robuste au désordre, sans mémoriser le moindre identifiant d'event.
- **Les deux moitiés de Modulith, chacune à sa place** : les **events** portent les side-effects (mettre à
  jour la dernière séance) ; les **ports synchrones** répondent aux queries (le count). C'est ce qui donne
  enfin une vraie raison d'être au `PersonalTrainingQueryPort`.

### Composition backend (pas frontend)
La fiche du miroir est servie par Roster, qui interroge `PersonalTrainingQueryPort` pour le count et compose
une réponse complète. Le client ne voit pas que le count vient d'un autre module : **l'API est un contrat
orienté consommateur, pas un reflet de la structure interne**. (Alternative — composition côté frontend en 2
appels — rejetée : elle ferait fuiter l'architecture des modules vers le client.)

### Forme retenue
```java
// Roster — pas de compteur ici
record TrainingHistory(Instant lastWorkoutAt /*nullable*/, Set<MovementPattern> lastPatternsCovered) {
    TrainingHistory recordWorkout(Instant performedAt, Set<MovementPattern> patterns) {
        if (lastWorkoutAt != null && !performedAt.isAfter(lastWorkoutAt)) return this; // no-op monotone
        return new TrainingHistory(performedAt, patterns);
    }
}
// PersonalTraining — source de vérité du compteur
interface PersonalTrainingQueryPort { long countSessionsFor(UserId owner); }
```
`lastPatternsCovered` ne contient que les patterns des exercices **composés** du snapshot (cohérent avec
`WorkoutSession.patternsCovered()`, ADR-026). Stockage JSONB sur `athletes` (V009), cohérent avec
genetics/current_stats (ADR-019). **Périmètre sprint 3** : compteur passif, aucun calcul fitness/fatigue —
ce sera Athletics au sprint 4 (Banister, ADR-004), qui consommera le même event.

## Vérification — pas de cycle Modulith
D crée une dépendance **Roster → PersonalTraining** (via `personaltraining.api`). PersonalTraining ne dépend
de rien de Roster → **direction unique, pas de cycle** (confirmé par `AtlasApplicationModulesTest.verify()`
et une règle ArchUnit dédiée). Conceptuellement sain : le miroir *reflète* l'entraînement, donc en aval.

## Conséquences

**Positives** — pas de problème d'idempotence sur le count (supprimé, pas mitigé) ; état Roster idempotent
et robuste au désordre ; source de vérité unique ; le port inter-module prend tout son sens.

**Négatives / à surveiller**
- Une query cross-module (`count(*)` indexé par owner) à l'affichage de la fiche miroir. Négligeable,
  cacheable plus tard si besoin.
- `lastWorkoutAt`/`lastPatternsCovered` restent (légitimement) dupliqués dans Roster — ce sont les données
  dont le miroir a besoin, pas un cache du compteur.
- Edge cosmétique : deux séances au `performedAt` identique → la garde stricte `>` ne rafraîchit pas
  `lastPatternsCovered` pour la seconde. Négligeable (même instant exact pour 2 séances distinctes).
