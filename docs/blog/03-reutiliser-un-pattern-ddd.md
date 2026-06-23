---
title: "Réutiliser un pattern DDD : combien ça coûte vraiment ?"
date: 2026-06-23
author: Ryan Foerster
tags: [atlas, devblog, ddd, java, spring, architecture, modulith]
status: draft
---

# Réutiliser un pattern DDD : combien ça coûte vraiment ?

Le Sprint 1 d'**Atlas** m'a appris le DDD. Le Sprint 2 m'a appris quelque chose de différent, et de plus
inquiétant : **est-ce que le DDD tient sous pression ?**

Petit rappel pour ceux qui débarquent. Atlas, c'est un « Football Manager du lifting » : tu diriges une
écurie d'athlètes de force, tu les programmes, tu les amènes en compétition, avec une simulation qui
prend la sport science au sérieux. Au Sprint 1, j'ai construit le module **Identity** (connexion, compte)
en découvrant le DDD pour la première fois. Au Sprint 2, j'ai construit le module **Roster** : ton écurie,
ton athlète miroir, le recrutement d'athlètes générés procéduralement.

La grande question du sprint n'était donc pas « est-ce que je sais faire du DDD » — c'était : **mon
premier module était-il un coup de chance, ou un vrai template réutilisable ?** Un pattern qu'on
n'utilise qu'une fois n'est pas un pattern. C'est une anecdote.

J'ai décidé de mesurer. Pas au feeling — avec des chiffres.

## Le test : trois métriques

À la fin du sprint, j'ai compté trois choses.

**ADRs (les décisions d'architecture).** Le Sprint 2 a produit **4 nouveaux ADRs** (019 à 022). Mais
chacun s'appuie sur des décisions déjà prises : domaine pur (ADR-003), mappers manuels (ADR-015, que j'ai
*étendu* plutôt que remplacé), kernel partagé (ADR-017), UUID v7 (ADR-014)… Environ **8 décisions
réutilisées telles quelles**. Le chiffre qui compte : **zéro architecture nouvelle**. Les 4 ADRs ne
décrivent pas de nouveaux principes, ils *appliquent* les anciens à des cas plus riches.

**Tests.** J'ai écrit **48 tests** pour le module roster. Mais l'infrastructure de test — la classe de
base Testcontainers avec un vrai PostgreSQL, AssertJ, la discipline TDD sur le domaine, la config JaCoCo
qui force 80 % de couverture sur `domain/` — a été **réutilisée à 100 %**. Je n'ai pas réinventé un seul
outil de test. Mes deux tests d'intégration roster héritent de la même `AbstractIntegrationTest` qu'au
Sprint 1.

**Patterns.** Là, le partage est net :

- *Copiés* : value objects auto-validants, `reconstitute()` pour la réhydratation, ports/adapters,
  hiérarchie `DomainException`, events en records, domain services purs câblés en beans dans
  l'infrastructure.
- *Inventés* : l'aggregate avec entité interne, la persistance JSONB en trois couches, la génération
  procédurale seedée.

Le ratio est clair : **on copie la structure, on invente le modèle**. Le squelette se réutilise tel quel ;
ce qui change, c'est ce qu'on met dedans.

## Le moment qui valide tout : MagicLink → ScoutedCandidate

Il y a un endroit où la réutilisation est devenue palpable.

Au Sprint 1, j'avais un `MagicLink` : un objet temporaire, à durée de vie courte, consommé une seule
fois (le lien de connexion par email). Au Sprint 2, le scouting m'a posé un problème de sécurité : quand
le joueur « scoute » un athlète, le backend lui renvoie un candidat. S'il pouvait ensuite recruter en
renvoyant ce candidat, n'importe quel client malin pourrait **forger** un athlète parfait — un Phénomène
à génétique maxée. Le système de rareté deviendrait trivialement contournable.

La solution : persister le candidat côté serveur avec un identifiant, et ne recruter que par cet id. Un
objet temporaire… à durée de vie courte… consommé une seule fois.

Je n'ai pas inventé un nouveau mécanisme. J'ai **reconnu** que c'était exactement `MagicLink`. Même
forme : `isExpired()`, `canBeConsumed()`, `consume()`. Même cycle de vie. Mon `ScoutedCandidate` est le
jumeau de `MagicLink`, dans un autre contexte métier. C'est *ça*, capitaliser un template : pas
copier-coller du code, mais reconnaître un pattern qu'on possède déjà.

Et pour ne pas me mentir à moi-même, j'ai écrit un **test ArchUnit** : il vérifie par l'architecture que
le module roster a bien les 4 couches, que son domaine n'importe ni Spring ni JPA ni Jackson, que la
persistance utilise des mappers manuels (pas de MapStruct), et que les events publics sont des records.
Un test unitaire vérifie un comportement ; un test d'architecture vérifie une *forme*. Le template n'est
plus une intention dans ma tête — c'est une contrainte exécutable.

## Où le DDD a craqué (un peu)

Sous pression, deux fissures sont apparues. Aucune fatale, mais instructives.

**Le kernel partagé était sous-peuplé — et je ne le voyais pas.** Au Sprint 1, mon `UserId` vivait dans
`identity.domain.model`, à l'intérieur du module Identity. Ça marchait. Au Sprint 2, mon `Roster` a un
propriétaire : un `UserId`. Sauf que Spring Modulith *interdit* à roster d'importer l'intérieur
d'identity. Le mono-module masquait l'incohérence ; il a fallu un **deuxième consommateur** pour la
révéler. J'ai promu `UserId`, `Weight`, `OneRepMax` et deux enums vers le kernel `shared`. Leçon
contre-intuitive : **un kernel partagé ne se peuple correctement que sous la pression d'un deuxième
module.** Avant ça, on devine ; après, on sait.

**La modélisation de l'Athlete.** En relisant mon plan de façon critique, j'ai tiqué : un `Athlete` est-il
un aggregate root à part entière, ou une entité interne de `Roster` ? J'avais le réflexe « tout est un
aggregate ». Mauvais réflexe. Un athlète n'a pas de sens hors d'une écurie. Décision : `Roster` est le
**seul** aggregate root, `Athlete` est une entité interne — pas de repository propre, créé uniquement par
`Roster.addMirror()` ou `Roster.recruit()`, et l'invariant « au plus un athlète miroir » est protégé au
niveau de l'écurie. Cette décision a tout simplifié en aval : un seul point de persistance, une sécurité
naturelle (un athlète qui n'est pas dans *ton* roster renvoie 404).

Ça m'a coûté un `LazyInitializationException` en chemin — charger l'aggregate entier avec sa collection
d'athlètes hors transaction. Corrigé en rendant l'adapter de lecture `@Transactional`. Un grand classique
JPA que je connais mieux, maintenant.

## La persistance qui ne salit pas le domaine

Dernier morceau dont je suis fier : stocker la génétique d'un athlète — des `Map` de potentiels par
groupe musculaire et par mouvement — en colonne `jsonb` PostgreSQL, **sans** importer Jackson dans mon
domaine pur.

Trois couches : le domaine reste un `record` sans aucune annotation ; un DTO d'infrastructure porte
l'annotation `@JdbcTypeCode(SqlTypes.JSON)` (support JSON *natif* d'Hibernate 7 — **aucune dépendance
ajoutée**, pas de hypersistence-utils) ; et un mapper manuel traduit entre les deux. J'ai vérifié sur un
vrai PostgreSQL que le round-trip est exact (la précision des `double` survit) et qu'une requête
`jsonb_typeof(genetics) = 'object'` confirme qu'on a bien du jsonb natif, pas du texte déguisé.

## Verdict

Alors, combien ça coûte de réutiliser un pattern DDD ?

Moins cher que je le craignais — à condition d'avoir **mesuré** au lieu de deviner. La structure se copie
à 100 %. Le modèle, lui, demande du jugement : c'est là que part l'énergie (l'aggregate, la rareté, le
JSONB). Les frictions n'ont pas été dans le template, mais dans les **angles morts du Sprint 1** que seul
un deuxième module pouvait révéler : un kernel incomplet, une frontière de persistance pas encore éprouvée.

Le DDD a tenu sous pression. Mieux : il m'a *forcé* à voir les fissures tôt — au moment de la
modélisation, pas en production. Et il y a quelque chose de satisfaisant à recruter un athlète dans une
UI, voir sa génétique de force boostée par mes vrais 1RM, et savoir que derrière, c'est le même squelette
propre que pour la page de connexion.

Prochain sprint : les vraies séances IRL. Mon athlète miroir va commencer à *vivre*.
