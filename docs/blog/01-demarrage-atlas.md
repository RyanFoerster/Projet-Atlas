---
title: "Démarrage d'Atlas — poser des fondations qu'on ne reniera pas"
date: 2026-06-22
author: Ryan Foerster
tags: [atlas, devblog, architecture, spring-boot, angular, ddd]
status: draft
---

# Démarrage d'Atlas

Atlas, c'est l'idée un peu folle d'un **Football Manager du lifting** : tu diriges une écurie d'athlètes de force, tu les programmes, tu les amènes en compétition — avec une simulation qui prend la sport science au sérieux. Cardio pur n'augmente pas le développé couché, point. Et un de tes athlètes, l'« athlète miroir », progresse en fonction de **tes vraies séances** loggées dans l'app.

Mais avant de simuler quoi que ce soit, il faut des fondations. Ce premier post raconte le **Sprint 0** : zéro logique métier, 100 % d'infrastructure. L'objectif n'était pas de coder vite, c'était de poser une base qu'on ne reniera pas dans six mois.

## Le pari technique

Le stack est volontairement moderne, et assumé comme tel : **Java 25 LTS**, **Spring Boot 4.1**, **Spring Modulith**, **PostgreSQL 17**, **Angular 22** (zoneless, signals), **Tailwind**. Deux raisons : ça doit être une vitrine technique crédible, et ça doit tenir dans le temps (Java 25 est supporté jusqu'en 2030).

Architecture : un **modular monolith** en DDD. Une seule application déployée, mais découpée en 8 modules métier strictement isolés (`identity`, `athletics`, `programming`, `competition`…). Spring Modulith **vérifie** cette isolation à chaque build — si un module va fouiller dans les entrailles d'un autre, le build casse. La discipline n'est pas une promesse, c'est une contrainte outillée.

Le cœur métier (le futur modèle Fitness-Fatigue) vivra dans un **domaine pur** : zéro Spring, zéro JPA, juste du Java. Testable en millisecondes, lisible comme un livre de game design.

## Ce qu'on a réellement construit

À la fin du Sprint 0 :

- Un squelette Maven multi-module (8 bounded contexts + un kernel partagé), avec le test d'isolation Spring Modulith qui **détecte bien les 9 modules** et tourne à chaque build.
- Une persistence testée sur un **vrai PostgreSQL** via Testcontainers (jamais H2), Flyway câblé, et le pattern moderne `@ServiceConnection` + container singleton.
- Un endpoint `/actuator/health`, Swagger UI, et une app **Angular 22 zoneless** qui appelle l'API en temps réel pour afficher son état (validation CORS bout en bout).
- Une **CI GitHub Actions** verte : deux jobs parallèles (backend / frontend), caches indépendants, Maven wrapper pour la reproductibilité.

Rien de spectaculaire à l'écran — une page « Hello world » et un badge vert « UP ». Mais derrière, tout est en place pour construire le reste.

## Deux histoires de guerre

Parce qu'un devblog honnête raconte aussi les galères.

**Spring Boot 4 a démantelé son auto-configuration.** En Boot 3, avoir `flyway-core` sur le classpath suffisait à déclencher les migrations. En Boot 4, l'énorme jar `spring-boot-autoconfigure` a été **éclaté en modules par techno**. Résultat : Flyway était bien là, aucune erreur… et aucune migration. La table d'historique n'était jamais créée. Il fallait le starter dédié `spring-boot-starter-flyway` (qui ramène le module d'auto-config). C'est LA rupture qui va piéger énormément de monde en migration 3→4.

**Un PostgreSQL fantôme sur le port 5432.** L'app refusait obstinément de se connecter à la base (« password authentication failed »), alors que le container Docker était nickel. Après un diagnostic en escalier — Testcontainers vert (donc la config est bonne), auth directe au container OK (donc le container est bon), puis l'app arrêtée et *quelque chose répond encore sur 5432* — le coupable est tombé : un **PostgreSQL natif** installé de longue date squattait le port. La leçon n'est pas « il y avait un autre Postgres ». La leçon est **méthodologique** : prouver d'abord que le problème est *externe* à ton code avant de le chercher *dedans*. Ça a évité des heures à débugger une config qui était correcte depuis le début.

## La suite

Le Sprint 1 attaque le premier vrai morceau : **Identity et l'onboarding** du joueur. Puis viendra le roster, les vraies séances IRL, et enfin le cœur du réacteur — la simulation Fitness-Fatigue par groupe musculaire.

Les mécaniques seront documentées publiquement, parce que la confiance de la communauté lifting est l'asset à protéger. Pas de boîte noire, pas de monétisation prédatrice. Un produit premium honnête, construit au grand jour.

À suivre.

---

*Atlas est en développement. Ce devblog documente la construction, succès et galères compris.*
