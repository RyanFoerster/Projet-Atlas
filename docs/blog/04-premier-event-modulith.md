---
title: "Mon premier event Spring Modulith en production (et tout ce que les tutos ne disent pas)"
date: 2026-06-25
author: Ryan Foerster
tags: [atlas, devblog, spring-modulith, event-driven, ddd, java, architecture]
status: draft
---

# Mon premier event Spring Modulith en production

La plupart des contenus sur Spring Modulith s'arrêtent au même endroit : *« regardez, je publie un event, un
autre module le reçoit, magique. »* Démo faite, applaudissements, fin.

Le problème, c'est que cet endroit est exactement là où les vraies questions commencent. Un event en
production, ce n'est pas `publishEvent()` et basta. C'est : **est-ce que je perds l'event si le consumer
plante ? Est-ce que je double-compte si l'event est rejoué ? Est-ce que j'ai vraiment isolé mes modules, ou
juste l'illusion ?** Ce sont ces questions-là qui m'ont occupé au sprint 3 d'**Atlas** — mon « Football
Manager du lifting ». Et la réponse à l'une d'elles m'a fait désassembler une classe de Spring Modulith pour
en avoir le cœur net.

## Le décor

Atlas a un concept central : l'**athlète miroir**. Tu logges tes vraies séances de muscu, et un athlète de ton
écurie virtuelle progresse en fonction. Techniquement, ça veut dire que deux modules doivent se parler :
`PersonalTraining` (où tu logges la séance) doit déclencher une mise à jour dans `Roster` (où vit le miroir).

Le réflexe junior : `Roster` appelle une méthode de `PersonalTraining`, ou l'inverse. Le réflexe DDD/Modulith :
**un event**. `PersonalTraining` publie `WorkoutLogged`, `Roster` le consomme, et les deux modules restent
ignorants l'un de l'autre. Jusqu'au sprint 3, mes modules ne s'étaient jamais parlé. C'était le premier vrai
test de l'architecture *modular monolith*.

## Étape 1 — publier l'event au bon endroit

```java
@Transactional
public WorkoutSession logWorkout(UserId owner, LogWorkoutCommand cmd) {
    WorkoutSession saved = repository.save(WorkoutSession.log(...));
    eventPublisher.publishEvent(WorkoutSessionToEventMapper.toEvent(saved)); // DANS la transaction
    return saved;
}
```

Le détail qui compte : `publishEvent` est appelé **à l'intérieur** de la méthode `@Transactional`. Avec
l'*event publication registry* de Modulith (la dépendance `spring-modulith-starter-jpa`), ça insère une ligne
dans une table `event_publication` **dans la même transaction** que la séance. Soit les deux sont commités
ensemble, soit aucun.

C'est le **pattern transactional outbox**, et Modulith le fait pour toi. Publier *après* le commit (hors
`@Transactional`) ouvrirait une fenêtre : la séance existe, le serveur crashe, l'event n'est jamais parti. Le
miroir ne saurait jamais que tu as soulevé. Pour un compteur visible par l'utilisateur, c'est inacceptable.

Le consumer, côté `Roster` :

```java
@ApplicationModuleListener // = @TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)
public void on(WorkoutLogged event) {
    rosterRepository.findByOwnerId(new UserId(event.ownerId()))
        .ifPresent(roster -> rosterRepository.save(roster.recordMirrorWorkout(event.performedAt(), compoundPatterns(event))));
}
```

Asynchrone, après le commit. **Cohérence éventuelle** assumée : la séance est enregistrée tout de suite, le
miroir suit quelques millisecondes plus tard. Pour Atlas, c'est exactement ce qu'on veut — logger une séance
ne doit pas échouer parce que `Roster` a un souci.

## Étape 2 — ne pas faire fuiter le domaine dans l'event

Première tentation : mettre mes objets riches dans l'event. `WorkoutLogged` qui transporte une
`List<LoggedExercise>`, avec mes value objects, mon sealed `ExerciseCategory`, mon enum `BodyRegion`…

C'est un piège. Le jour où je refactore `LoggedExercise`, je casse `Roster`, qui en dépendrait via l'event. Un
event public est un **contrat**, pas une fenêtre sur mon modèle interne. La solution : un **snapshot aplati**,
composé uniquement de types primitifs ou partagés.

```java
record LoggedExerciseSnapshot(String name, String categoryType, MovementPattern pattern,
                              String accessoryRegion, List<ExerciseSetSnapshot> sets) {}
```

Mon `ExerciseCategory` (un sealed interface : un exercice est soit `CompoundForce(MovementPattern)`, soit
`Accessory(BodyRegion)`) est **aplati** en un discriminant + des champs nullables. `MovementPattern` vit dans
mon kernel partagé, donc je peux l'exposer ; mais `BodyRegion` est interne à `PersonalTraining`, alors je le
passe en `String`, jamais en type. La frontière est nette.

## Étape 3 — la surprise au premier `verify`

Tout compile. Je lance `./mvnw verify`, confiant. Rouge :

```
Module 'roster' depends on non-exposed type
dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged within module 'personaltraining'!
```

Mon premier réflexe : *un cycle ?* Non. La dépendance est à sens unique (`Roster → PersonalTraining`). Le mot
clé est **non-exposed**. Et là j'apprends quelque chose que je ne savais pas : par défaut, Spring Modulith
n'expose que le **package racine** d'un module. Mon event est dans `personaltraining.api.events` — un
sous-package — donc *interne*, même s'il s'appelle `api`.

La règle que je m'étais fixée (« les modules externes n'importent que de `api/` ») n'était qu'une intention
dans ma tête. Modulith ne la connaissait pas. Il faut la **déclarer** :

```java
@org.springframework.modulith.NamedInterface("api")
package dev.ryanfoerster.atlas.personaltraining.api; // + api.events, même nom → fusionnés
```

C'est la première fois du projet qu'un module en consomme un autre via `api/`. Sprints 1 et 2, mes modules
vivaient chacun dans leur coin. La convention n'avait jamais été *exercée*, donc jamais *outillée*. Maintenant
elle l'est — et un test ArchUnit dédié vérifie en plus que `Roster` ne dépend de `PersonalTraining` que via
`api`, jamais de son `domain`/`application`/`infrastructure`. L'isolation n'est plus une bonne volonté, c'est
une contrainte qui casse le build.

## Étape 4 — le problème d'idempotence, et comment je l'ai supprimé

Mon consumer met à jour le miroir : `workoutCount += 1`, et la date de la dernière séance. Sauf que la
livraison Modulith est **at-least-once**, pas exactly-once. Un event peut être rejoué — au redémarrage de
l'app, ou dans une fenêtre étroite. Et `+= 1` sur rejeu, ça donne « j'ai loggé 3 séances, mon miroir en
affiche 4 ». Bug visible, bug qui érode la confiance.

J'ai d'abord pensé *mitiger* : mémoriser l'id du dernier event traité et skipper les doublons. Mais mon tech
lead (enfin, moi-même en relecture critique) a trouvé le contre-exemple : si l'event #2 échoue, que #3 passe,
puis que #2 est rejoué au restart, ma garde par « dernier id » ne le rattrape pas. Pour un compteur visible,
*à peu près idempotent* ne suffit pas.

La meilleure solution n'était pas de mieux mitiger. C'était de **supprimer le problème** : ne pas dupliquer le
compteur du tout. Le nombre de séances a déjà une source de vérité — le nombre de lignes dans
`workout_sessions`, côté `PersonalTraining`. Je l'expose via un **port de query synchrone**
(`PersonalTrainingQueryPort.countSessionsFor`) et je le compose côté backend dans la fiche du miroir. Côté
`Roster`, je ne garde plus que la *dernière* séance, mise à jour par **écrasement monotone** : je n'écris que
si la séance est strictement plus récente que la dernière connue. Rejeu d'un event ancien ? Plus récent que
rien ? Non → no-op. **Idempotent par construction, et robuste au désordre de livraison, sans mémoriser le
moindre identifiant.**

La leçon dépasse Atlas : Modulith a **deux** mécanismes de communication, et il faut les utiliser chacun à sa
place. Les **events** portent les *side-effects* (mettre à jour la dernière séance). Les **ports synchrones**
répondent aux *queries* (combien de séances ?). Le piège, c'est de tout faire en events — y compris répliquer
une donnée qui a déjà un propriétaire. Un compteur qu'on ne duplique pas ne peut pas diverger.

## Étape 5 — le moment où j'ai désassemblé Spring Modulith

Restait une affirmation que je répétais sans l'avoir vérifiée : « le republish au démarrage ne rejoue que les
events incomplets ». Toute ma stratégie d'idempotence en dépendait. Si Modulith rejouait *tout* dans une
fenêtre, j'avais un problème plus large.

Plutôt que de croire la doc sur parole, j'ai fait ce qu'on fait trop rarement : je suis allé voir le code réel.
`javap -c -p` sur `JpaEventPublicationRepository`, et les constantes de requêtes apparaissent en clair :

```sql
-- findIncompletePublications() — appelée par republish-outstanding-events-on-restart
select p from DefaultJpaEventPublication p where p.completionDate is null order by p.publicationDate asc

-- marquage après succès du listener
update DefaultJpaEventPublication p set p.status = COMPLETED, p.completionDate = ?3
 where p.serializedEvent = ?1 and p.listenerId = ?2 and p.completionDate is null
```

Sans ambiguïté : le republish filtre sur `completion_date IS NULL`. Une publication complétée n'est **jamais**
rejouée. Le seul rejeu possible concerne un event resté incomplet — un consumer qui a échoué. Affirmation
devenue fait, vérifiée à la source.

## Étape 6 — prouver le chemin d'échec

C'est là que je voulais aller plus loin que les tutos. N'importe qui peut tester qu'un event passe quand tout
va bien. Moi je voulais prouver ce qui se passe **quand le consumer plante**.

Trois tests, donc. Le **bout-en-bout** d'abord, avec l'API `Scenario` de Modulith qui sait attendre la
consommation asynchrone (pas de `Thread.sleep` flaky) :

```java
scenario.stimulate(() -> logWorkout.logWorkout(owner, command()))
        .andWaitForStateChange(() -> mirrorLastWorkoutAt(owner))
        .andVerify(last -> assertThat(last).isEqualTo(PERFORMED_AT));
```

La **complétion** ensuite : après succès, `completion_date IS NOT NULL` sur la ligne de l'event — la preuve
empirique, alignée sur ce que j'avais lu au désassemblage.

Et surtout le **négatif** : je mocke le `save` du repository de `Roster` pour qu'il lève une exception. Le
handler échoue. Et je vérifie que **la publication reste incomplète** (`completion_date IS NULL`) **et que la
séance reste loggée**. Autrement dit : la donnée métier survit, et l'event sera re-livré au prochain
redémarrage. La durabilité outbox n'est plus un slogan, c'est un test vert.

## Ce que ça m'a appris

Le modular monolith tient. Mieux : il m'a forcé à voir les vraies questions tôt — l'isolation au premier
`verify`, l'idempotence au moment de la modélisation, la durabilité dans un test. Un event en production, ce
n'est pas une ligne de code. C'est un contrat (snapshot), une frontière outillée (`@NamedInterface` + ArchUnit),
une garantie de livraison (outbox, at-least-once), et un consumer idempotent.

Et quand un doute subsiste sur le comportement réel d'un framework, on n'argumente pas dans le vide : on
désassemble, on lit les requêtes, on sait. C'est, je crois, ce qui sépare « j'ai fait un event » de « j'ai mis
un event en production ».

Prochain sprint : le module Athletics va consommer ce même `WorkoutLogged` pour faire *vraiment* progresser le
miroir — modèle de fatigue, adaptation, sport science. Le pattern est prouvé. Il n'y a plus qu'à brancher.
