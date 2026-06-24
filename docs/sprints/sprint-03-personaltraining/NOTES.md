# Sprint 3 — Notes en cours (matière pour rétro + mini-cours)

> Accumulateur de points saillants à chaud, à recycler dans `RETROSPECTIVE.md` et le mini-cours en S6.

## Patterns / pièges rencontrés

- **JSONB d'une liste : enveloppe-la dans un record.** Un champ `List<ExerciseJson>` nu annoté
  `@JdbcTypeCode(SqlTypes.JSON)` risque de se désérialiser en `List<LinkedHashMap>` (résolution de
  générique). L'envelopper dans `ExercisesJson(List<…>)` donne un type concret à la racine → bon type
  d'élément, et jsonb de type `object` (cohérent avec Roster). → mini-cours sprint 3.

- **Sealed type en JSONB : aplatir en discriminant.** `ExerciseCategory` (sealed) est aplati en
  `categoryType` + champs nullables, reconstruit via `switch` exhaustif. Même pattern que le snapshot
  d'event (ADR-024). → mini-cours.

- **État partagé entre classes de test (Testcontainers singleton) — pattern à surveiller.** La base est
  unique et partagée ; un `deleteAll()` sur une table partagée (`users`) peut violer des FK de lignes
  laissées par d'autres classes de test (rosters). **Règle tenue** : chaque test scope ses données par
  owner/id unique → les résidus d'autres tests n'affectent pas les assertions. **Vigilance** : si un jour
  un test fait une assertion globale (`countAll`, `findAll`…), elle sera polluée par les résidus — il
  faudra alors isoler (scope explicite, ou nettoyage ordonné en respectant les FK). À acter en rétro.

- **Named interface Modulith — `api/` n'est public que si déclaré.** Découvert au GATE C : déclarer un
  event dans `api/events/` ne suffit pas à le rendre consommable cross-module. Par défaut Modulith n'expose
  que le package racine du module ; il faut `@NamedInterface("api")` sur les package-info d'`api` et
  `api.events` (même nom → fusionnés). C'est la première fois qu'on consomme un `api/` cross-module dans le
  projet → la convention CLAUDE.md « modules externes n'importent que api/ » est maintenant *outillée*. →
  mini-cours + devblog #4 (point que beaucoup ratent avec Modulith).

- **Idempotence : supprimer le problème > le mitiger (option D).** Plutôt que de rendre un compteur dupliqué
  idempotent (garde par id, ou table inbox), on ne duplique pas le compteur : sa source de vérité est
  PersonalTraining, interrogée via un query port. L'état résiduel côté Roster (dernière séance) est
  idempotent par écrasement monotone. Belle illustration « events pour side-effects, ports pour queries ».
  → devblog #4 + mini-cours.

- **Désassembler une lib pour lever un doute.** Au lieu de supposer le comportement du republish Modulith,
  on a désassemblé `JpaEventPublicationRepository` (`javap -c -p`) pour lire les vraies requêtes JPQL
  (`where completion_date is null`). Niveau de rigueur à garder pour les décisions structurantes.

## Décisions de modélisation

- `ExerciseCategory` sealed (ADR-026) : séparation force génétique (`MovementPattern`) vs catégorie
  d'exercice loggé. Mapping `BodyRegion` → `MuscleGroup` repoussé au sprint 4 (frictions `BACK`,
  `FOREARMS` tracées dans ADR-026).
