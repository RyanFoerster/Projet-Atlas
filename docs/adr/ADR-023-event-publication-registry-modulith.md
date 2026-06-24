# ADR-023 : Event Publication Registry Modulith — durabilité, at-least-once et cohérence éventuelle

**Statut** : Accepté (GATE C du sprint 3 validé)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte

PersonalTraining publie l'event `WorkoutLogged` ; Roster le consomme pour mettre à jour le
`TrainingHistory` de l'athlète miroir. Sans garantie de livraison, un échec du consumer perdrait l'event :
la séance resterait loggée mais le miroir ne refléterait jamais l'entraînement. Spring Modulith fournit un
**event publication registry** (table `event_publication`) pour rendre cette communication durable.

## Décision

### 1. Activer le registry JPA
Dépendance `spring-modulith-starter-jpa` (tire `events-jpa` + le serializer `events-jackson`). La table
`event_publication` est créée par **Flyway** (migration V007, schéma officiel Modulith 2.1.0 v2 pour
PostgreSQL), pas par Modulith — on désactive son auto-initialisation pour rester cohérent avec
`ddl-auto: validate` (ADR-008). L'entité `JpaEventPublication` est validée contre la table au boot.

### 2. Transactional outbox : publier DANS la transaction
`LogWorkoutUseCase` appelle `eventPublisher.publishEvent(...)` **à l'intérieur** de sa méthode
`@Transactional`. Le registry insère alors la ligne `event_publication` (incomplète) **dans la même
transaction** que la séance : soit la séance ET la trace de l'event sont commitées ensemble, soit rien.
C'est le **pattern transactional outbox**, géré par Modulith — pas de fenêtre où la séance existe sans
event tracé.

### 3. Sémantique de livraison : at-least-once, comportement (a) — *prouvé par désassemblage*
On a désassemblé `JpaEventPublicationRepository` (Modulith 2.1.0) pour lever toute ambiguïté. La requête
de republication au démarrage est :

```sql
-- findIncompletePublications() — utilisée par republish-outstanding-events-on-restart
select p from DefaultJpaEventPublication p where p.completionDate is null order by p.publicationDate asc
```

et la complétion après succès du listener :

```sql
update DefaultJpaEventPublication p set p.status = COMPLETED, p.completionDate = ?3
 where p.serializedEvent = ?1 and p.listenerId = ?2 and p.completionDate is null
```

**Conclusion** : Modulith ne rejoue QUE les publications dont `completion_date IS NULL`, par (event,
listener). Une publication complétée n'est **jamais** rejouée. Le seul rejeu possible est celui d'un event
resté **incomplet** (consumer échoué, ou crash dans la fenêtre commit/complétion). Pas de rejeu en masse,
pas de fenêtre aveugle (comportement « (a) »).

Config Atlas par défaut (`application.yml`) :
```yaml
spring.modulith.events.republish-outstanding-events-on-restart: true   # gratuit ; rejoue les incomplets au boot
spring.modulith.events.jdbc.schema-initialization.enabled: false       # Flyway possède le schéma
```
**Pas de retry runtime automatique** : un incomplet est re-livré au **redémarrage**, pas en boucle à
l'exécution.

### 4. Cohérence éventuelle assumée (async)
Le consumer est annoté `@ApplicationModuleListener` = `@TransactionalEventListener(AFTER_COMMIT)` +
`@Async` + `@Transactional(REQUIRES_NEW)`. La séance commit immédiatement côté PersonalTraining ; le
miroir est mis à jour juste après, dans une transaction séparée. Un client pourrait observer un état
transitoire (séance loggée, miroir pas encore à jour) pendant quelques ms. **Acceptable pour Atlas** : le
logging de séance ne doit pas dépendre de la disponibilité de Roster (découplage), et il n'y a aucune
criticité temporelle. Si un jour critique : basculer en synchrone.

## Preuves (tests)

- **End-to-end (`Scenario` API)** : log → publish → consommation async → `TrainingHistory` du miroir à jour.
- **Complétion** : après succès du consumer, `completion_date IS NOT NULL` sur la ligne `WorkoutLogged`
  (donc exclue du republish — confirme empiriquement le comportement (a)).
- **Négatif** : consumer forcé en échec (save mocké qui lève) → la publication reste **incomplète**
  (`completion_date IS NULL`) ET la séance reste loggée. La durabilité outbox est prouvée, pas supposée.

## Idempotence du consumer
La garantie étant at-least-once, le consumer **doit** être idempotent. Choix retenu : aucun compteur
dupliqué côté Roster (option D, ADR-025) + écrasement monotone du `TrainingHistory`. Rejouer un event est
donc un no-op. Voir ADR-025.

## Conséquences

**Positives** — communication inter-module durable et découplée ; séance jamais perdue ; comportement de
livraison maîtrisé (et prouvé, pas supposé) ; base saine pour Athletics au sprint 4 (même event).

**Négatives / à surveiller**
- Cohérence éventuelle : à garder en tête pour l'UX (un rafraîchissement peut être nécessaire).
- Purge des publications complétées : la table `event_publication` grossit ; prévoir une purge
  (`spring.modulith.events.completion-mode` / archivage) si le volume le justifie. Non nécessaire au MVP.
- Multi-instance : le republish-au-restart et le traitement async sont pensés mono-instance pour le MVP.
  En multi-instance, prévoir une coordination (ou externalisation via broker). Tracé, repoussé.

## Options futures (non retenues au sprint 3)
- Scheduler de resoumission périodique des incomplets (au lieu du seul restart).
- `ExternalizedEvents` Modulith + broker fiable (Kafka/RabbitMQ) si multi-instance.
