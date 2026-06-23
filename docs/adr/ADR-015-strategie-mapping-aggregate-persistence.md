# ADR-015 : Stratégie de mapping aggregate ↔ persistence (mappers manuels, MapStruct réservé aux DTO)

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

ADR-003 impose un domaine pur (aggregates riches, value objects, constructeurs privés, factories métier, invariants garantis au constructeur), et ADR-008 sépare les entités JPA (`infrastructure/persistence/`) des entités du domaine, avec un mapping entre les deux — « via MapStruct ou mappers manuels ». CLAUDE.md pinne MapStruct 1.6.3.

Au moment de bootstraper la persistence du module identity (Sprint 1), la question concrète se pose : **MapStruct ou mappers manuels pour la frontière `domain ↔ JPA` ?** L'essai de MapStruct sur l'aggregate `User` révèle trois incompatibilités structurelles, toutes dues à la richesse *voulue* du domaine :

1. **Constructeurs privés + factories métier.** `User` n'expose qu'un constructeur privé. Sa factory de création `register(...)` *génère un nouvel identifiant* — donc inutilisable pour reconstruire une instance depuis un état stocké. MapStruct, qui s'appuie sur un constructeur public, un builder ou des setters, n'a aucune porte d'entrée.
2. **Accesseurs « record-style ».** Le domaine expose `user.id()`, `user.email()` — pas `getId()`/`getEmail()`. La stratégie d'accesseurs par défaut de MapStruct ne reconnaît que `getX()/isX()/setX()` (ou les composants d'un *record*). Sur une *classe* aux accesseurs nus, MapStruct ne détecte aucune propriété sans une `AccessorNamingStrategy` custom.
3. **Value objects à (dé)wrapper.** Chaque champ (`Email`↔`String`, `UserId`↔`UUID`, `Locale`↔`String`, `ZoneId`↔`String`…) exige une méthode de conversion explicite.

Conséquence : même la direction `domain → entity` échoue sans configuration ad hoc, et la direction `entity → domain` doit de toute façon être écrite à la main. MapStruct se retrouve contourné de partout, conservé « pour la forme » alors que les conversions sont manuelles. La cause racine est connue : **MapStruct est conçu pour des beans anémiques** (`getX/setX`, constructeurs publics, zéro logique) — l'exact opposé d'un aggregate DDD.

## Décision

On adopte une **doctrine de mapping par frontière**, valable pour **tous les modules** (identity, athletics, programming, etc.) :

- **Frontière `domain ↔ persistence` (JPA)** → **mappers écrits à la main.** Un mapper manuel par aggregate/entity, dans `infrastructure/persistence/mapper/`. Il connaît les value objects, appelle les factories adéquates, et reste 100 % lisible et debuggable. C'est le seul style compatible avec un aggregate riche sans l'affaiblir.
- **Frontière `application ↔ DTO web` (REST)** → **MapStruct.** Les DTO d'entrée/sortie sont anémiques *par design* (records plats, getters générés, pas d'invariant) : c'est exactement le terrain de MapStruct. Introduit au Sprint 6 (controllers REST).

### Pattern `reconstitute()` (réhydratation)

Pour reconstruire un aggregate depuis un état persistant **sans passer par la factory de création** (qui porte une sémantique métier — `register()` génère un id, fixe `createdAt`, etc.), chaque aggregate/entity expose une factory de réhydratation dédiée :

```java
User.reconstitute(UserId id, Email email, …, Instant lastLoginAt)
```

- **Intention** : distincte de `register()`. `register()` = « un *nouveau* Player naît » (génère l'identité, applique les règles de création). `reconstitute()` = « ce Player *existait déjà*, on le recharge tel quel » (l'identité et les timestamps viennent du stockage).
- **Invariants** : `reconstitute()` passe par le **même constructeur privé** que `register()` — donc les invariants techniques (non-null, cohérence temporelle) restent garantis. On ne crée pas une porte dérobée qui contourne la validation.
- **Visibilité** : **`public`**, faute de mieux structurellement. Le package-private serait idéal (réserver l'accès à la couche persistence) mais il est **impossible ici** : l'aggregate vit dans `domain/model/` et son mapper dans `infrastructure/persistence/` — deux packages distincts (le découpage hexagonal voulu). Mettre le mapper dans `domain/model/` pour gagner le package-private tirerait les types JPA dans le domaine et violerait ADR-003. Spring Modulith ne restreint pas la visibilité intra-module entre packages. On assume donc le `public` **avec une garde JavaDoc explicite** : `FOR PERSISTENCE LAYER ONLY — DO NOT USE FROM APPLICATION/WEB (use register() to create)`.

## Conséquences

**Positives**
- L'aggregate reste **intact** : aucun setter, aucun constructeur public, aucune concession à un outil. La richesse du domaine (ADR-003) est préservée à 100 %.
- Mapping totalement explicite, lisible, debuggable pas à pas — pas de génération « magique » à déboguer.
- MapStruct est conservé **à sa juste place** (DTO anémiques, S6), où il apporte une vraie valeur sans friction.
- Doctrine claire et réutilisable : tous les futurs modules savent quoi faire sans re-débattre.

**Négatives**
- Boilerplate de mapping manuel à écrire et maintenir pour chaque aggregate (mitigé par la régularité du pattern et la couverture de tests d'intégration sur Testcontainers).
- La factory `reconstitute()` est `public` : l'encapsulation repose sur une convention documentée, pas sur le compilateur. Risque faible (garde JavaDoc + revue de code), assumé.

**Neutres**
- Référence doctrinale : la distinction « création vs réhydratation » d'un aggregate est classique en DDD (Vaughn Vernon, *Implementing Domain-Driven Design*, ch. Factories & Repositories ; Eric Evans, *DDD*, sur les Factories qui reconstituent). On l'applique telle quelle.
- Si un jour un aggregate devenait réellement anémique (rare, et souvent le signe d'un sous-domaine pauvre), MapStruct redeviendrait envisageable pour sa persistence — mais ce serait l'exception, pas la règle.
