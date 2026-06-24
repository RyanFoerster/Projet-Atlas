# ADR-023 : Event Publication Registry Modulith — durabilité et cohérence éventuelle

**Statut** : Proposé — finalisation au GATE C du sprint 3 (intégration event-driven)
**Date** : Sprint 3
**Décideur** : Ryan Foerster

## Contexte (esquisse — à compléter au gate)

PersonalTraining publie `WorkoutLogged`, Roster le consomme pour mettre à jour le `TrainingHistory` du
miroir. Sans garantie de livraison, un échec du handler perdrait l'event (la séance resterait loggée mais
le miroir ne serait jamais à jour). Spring Modulith offre un **event publication registry** (table
`event_publication`) pour rendre cette communication durable.

## Décision (déjà tranchée — tensions #2 et #3)

- **Activer le registry JPA** (`spring-modulith-starter-jpa`), table `event_publication` créée par Flyway
  (V007), schéma v2 officiel Modulith 2.1.0.
- **Sémantique exacte** : durabilité transactionnelle au commit + **republication au démarrage**
  (`spring.modulith.events.republish-outstanding-events-on-restart=true`). **PAS de retry runtime
  automatique** — une publication échouée reste incomplète et durable, re-livrée au restart.
- **Cohérence éventuelle assumée** : `@ApplicationModuleListener` = async + `REQUIRES_NEW` + `AFTER_COMMIT`.
  La séance commit immédiatement ; le miroir est mis à jour juste après, de façon asynchrone.
- Options futures (non retenues au sprint 3) : scheduler de resoumission des incomplets ;
  `ExternalizedEvents` + broker (Kafka/RabbitMQ) si multi-instance.

> Corps complet (contexte détaillé, conséquences, preuve par test — dont le test négatif éventuel) rédigé
> au GATE C.
