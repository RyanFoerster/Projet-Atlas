# ADR-017 : Le kernel `shared` est un module Spring Modulith OPEN

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

ADR-001 impose une isolation stricte entre bounded contexts, vérifiée par Spring Modulith : un module n'expose que son package de base (et ses interfaces `api/`), ses sous-packages sont **internes** et inaccessibles aux autres modules.

Au Sprint 1, la première référence inter-module concrète est apparue : le module `identity` référence `shared.domain.exceptions.DomainException` (la base de toutes les exceptions métier). Spring Modulith a **fait échouer** `modules.verify()` :

```
Module 'identity' depends on non-exposed type
dev.ryanfoerster.atlas.shared.domain.exceptions.DomainException within module 'shared'!
```

C'est le comportement attendu pour un bounded context — mais `shared` n'en est pas un.

### Modulith CLOSED vs OPEN

Spring Modulith distingue deux types de modules :

- **CLOSED** (défaut) : le module **encapsule**. Seuls les types de son package de base (et ses named interfaces) sont visibles de l'extérieur ; tout le reste est privé au module. C'est ce qu'on veut pour `identity`, `athletics`, etc. — l'isolation est la règle, et la communication passe par `api/` (ports + events).
- **OPEN** : le module **n'encapsule pas** ; tous ses sous-packages sont accessibles depuis n'importe quel autre module. À réserver aux modules dont la raison d'être *est* d'être partagés.

## Décision

Le kernel `shared` est déclaré **module OPEN**, via son `package-info.java` :

```java
@org.springframework.modulith.ApplicationModule(type = ApplicationModule.Type.OPEN)
package dev.ryanfoerster.atlas.shared;
```

### Pourquoi `shared` est l'exception légitime

`shared` n'est pas un bounded context : c'est le **kernel transverse** du système (CLAUDE.md §3). Sa raison d'être est de fournir aux *autres* modules des briques fondamentales et partagées : value objects fondamentaux (`Weight`, `RPE`, `MovementPattern`, `MuscleGroup`…), base d'exceptions (`DomainException`), bases d'events. Lui appliquer l'encapsulation d'un bounded context serait un contresens — il n'a rien à cacher, tout à offrir. OPEN exprime exactement cette intention.

### Contrepartie disciplinaire : `shared` reste minimal

OPEN lève l'encapsulation ; la discipline doit donc venir de l'humain, pas de l'outil. Règle : **tout ajout dans `shared` doit prouver deux choses** —

1. **Transversalité** : il est (ou sera de façon imminente et certaine) utilisé par **au moins 2 modules**. Un type utilisé par un seul module n'a rien à faire dans le kernel.
2. **Caractère fondamental** : c'est une brique de base du langage du domaine, pas une règle métier appartenant à un contexte précis.

Si l'un des deux critères manque, le code vit **dans le module concerné**, pas dans `shared`. Un kernel qui grossit devient un couplage global déguisé — l'anti-pattern « god module » — qui réintroduit par la fenêtre le couplage que le modular monolith chasse par la porte.

### Règle anti-dérive

`shared` est la **seule** exception OPEN autorisée par défaut. **Aucun autre module ne peut devenir OPEN sans un nouvel ADR dédié** justifiant explicitement pourquoi l'isolation Modulith n'est pas pertinente pour ce cas précis. OPEN n'est jamais un raccourci pour « contourner une violation Modulith gênante » : une violation signale presque toujours un vrai problème de design (mauvaise frontière, dépendance qui devrait passer par `api/`), pas un excès de zèle de l'outil.

## Conséquences

**Positives**
- `modules.verify()` repasse au vert tout en gardant l'isolation stricte sur les vrais bounded contexts.
- L'intention de `shared` (kernel partagé) est explicite dans le code et outillée, pas seulement documentée.
- La règle anti-dérive empêche que OPEN devienne une échappatoire à l'isolation.

**Négatives**
- L'absence d'encapsulation sur `shared` repose sur la discipline (revue de code) pour rester minimal. Risque réel de dérive si la règle des 2 critères n'est pas tenue — d'où sa formalisation ici.

**Neutres**
- Décision formalisée *après* implémentation (le `package-info` OPEN a été posé au Sprint 1 pour débloquer `verify()`), mais elle découle directement de la nature de `shared` déjà actée en CLAUDE.md §3 — ce n'est pas une nouvelle direction, c'est la traçabilité d'un choix structurant.
