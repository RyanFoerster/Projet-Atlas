---
title: "Distribuer un stimulus d'entraînement sur les muscles : du mapping EMG au code"
date: 2026-06-29
author: Ryan Foerster
tags: [atlas, devblog, sport-science, emg, hypertrophie, modeling, ddd, honnêteté-épistémique]
status: draft
---

# Distribuer un stimulus d'entraînement sur les muscles : du mapping EMG au code

Dans [Atlas](#), un jeu de gestion d'écurie d'athlètes où la simulation repose sur de la vraie
sport-science, le sprint précédent avait fait tourner le modèle de Banister sur une **stat globale** : une
séance = une impulsion, une forme unique par athlète. Ce sprint, je l'ai rendu réaliste : la forme devient
un état **par groupe musculaire**, et chaque exercice distribue son stimulus sur les muscles qu'il travaille
vraiment. Un squat charge les jambes et le bas du dos, pas les biceps. Voici comment on traduit
« l'activation musculaire » en pondérations de code — et pourquoi le plus important, c'est d'être honnête
sur ce qu'on ne sait pas.

## Le problème : où va le stimulus d'un squat ?

Quand tu logges un squat 5×5, combien de ce travail va aux quadriceps ? Aux fessiers ? Aux érecteurs du
rachis ? La réponse intuitive — « regarde l'EMG » — cache un piège que j'ai mis du temps à formuler
proprement.

**L'EMG mesure l'activation électrique d'un muscle, pas le stimulus qu'il reçoit pour s'adapter.** Un muscle
peut s'allumer fort à l'EMG sans être le facteur limitant de l'exercice, et l'amplitude EMG ne se convertit
pas linéairement en hypertrophie. Donc utiliser l'EMG comme proxy de « combien de stimulus va à ce muscle »
est un **choix de modélisation**, pas une mesure.

J'avais le même dilemme qu'au sprint précédent avec la calibration de Banister :

1. Inventer des pondérations et les présenter comme « basées sur l'EMG ».
2. Construire des pondérations raisonnables, **dire explicitement** ce qui est sourcé et ce qui est une
   interprétation maison.

J'ai choisi (2). Et la distinction que j'ai posée est celle-ci : **le classement est sourcé, les nombres
sont assumés.**

## Classement sourcé, nombres assumés

Que les quadriceps soient le moteur primaire d'un squat et les ischio-jambiers secondaires — ça, la
littérature EMG et biomécanique le dit clairement (les ischios travaillent quasi-isométriquement au squat,
ils ne se raccourcissent presque pas). Que ce soit exactement 0.42 contre 0.08 — ça, c'est moi, calibré
pour la plausibilité. Ma table pour le squat :

```
SQUAT → QUADS 0.42, GLUTES 0.30, BACK_LOWER 0.10, CORE 0.10, HAMSTRINGS 0.08   (somme = 1)
```

Les poids somment à 1 par exercice : on **répartit** la magnitude de l'exercice, on ne la crée pas. Dans le
code, le mapping est un domain service pur, et le statut épistémique est écrit noir sur blanc :

```java
/**
 * L'EMG mesure l'activation, pas le stimulus pour l'adaptation : l'utiliser comme proxy de
 * répartition est un choix de modélisation Atlas. Le classement des muscles (primaire/secondaire)
 * est sourcé ; les nombres exacts sont une interprétation Atlas calibrée pour la plausibilité.
 */
```

J'ai fait valider la table par un œil de lifter (le mien, mais en mode critique) : la part fessiers du squat,
le split postérieur du soulevé de terre, la part biceps du tirage horizontal vs du tirage vertical
(supination → plus de biceps). C'est exactement le genre de revue qu'un powerlifter ou un coach repérerait
en deux secondes si elle était fausse — et c'est cette critiquabilité qui rend le modèle crédible.

## Les deux frictions qu'on assume

Deux cas ne tombent pas juste, et je les ai tracés plutôt que cachés :

- **Le dos.** Mon enum de muscles distingue `BACK_UPPER` et `BACK_LOWER`, mais un exercice accessoire loggé
  comme « dos » est plus grossier. Décision assumée : 80 % haut, 20 % bas.
- **Les avant-bras.** Il n'y a pas de muscle « avant-bras » dans le modèle. Un curl d'avant-bras doit
  produire *quelque chose* (pas un trou déroutant), alors je le replie sur les biceps (fléchisseurs
  adjacents, le grip co-charge le biceps). Imprécis, marginal, assumé.

Vérification utile au passage : mes 11 groupes musculaires couvraient déjà les 6 patterns de force sans
trou. Le prompt de départ craignait qu'enrichir l'enum ne casse le système génétique (qui doit couvrir tous
les muscles). Une fois le code vérifié, **le risque n'existait pas** : pas de nouveau muscle, pas de
migration. Lire le code avant de redouter le ripple.

## La limite que je n'ai pas patchée

Voici le point le plus intéressant. Avec « somme = 1 » et la charge absolue exclue du calcul (le RPE capture
déjà l'intensité relative), un curl isolé dépose **tout** son stimulus sur les biceps, tandis qu'un squat le
**répartit** sur cinq muscles. À volume et RPE égaux, le curl donne donc plus à ses biceps que le squat à
ses quads.

Physiquement, c'est faux à l'échelle systémique : un squat à 140 kg est plus « gros » qu'un curl à 20 kg.
Mais ce qui rend le squat plus gros, c'est la **charge** — 140 contre 20 kg — et la charge est explicitement
le sujet du sprint prochain. La tentation était d'ajouter tout de suite un « facteur d'ampleur composé vs
isolation ». Je ne l'ai pas fait, pour trois raisons :

1. Ce serait un proxy arbitraire d'un mécanisme (la charge) déjà planifié pour arriver proprement.
2. À l'échelle d'un muscle, ce n'est même pas faux : le poids < 1 du moteur primaire d'un composé capture
   qu'il n'est pas pris aussi près de **son** échec individuel qu'un muscle isolé (un squat à RPE 8 prend le
   *système* à 2 reps en réserve, pas forcément les quads).
3. Une variable à la fois : j'ajoutais déjà la distribution ce sprint.

Alors je l'ai **tracée comme limite assumée**, et je l'ai **observée** : une simulation de programme
asymétrique n'a montré aucun comportement absurde. Si un playtest futur prouve qu'un isolé domine de façon
irréaliste, je rouvrirai — sur observation, pas par anticipation.

## Ce que je retiens

- **Sourcer le classement, assumer les nombres.** C'est la version « mapping musculaire » de l'honnêteté
  épistémique : tracer précisément la frontière entre ce que la science dit et ce que tu interprètes.
- **Lire le code avant de redouter un ripple.** La peur d'une migration génétique s'est dissoute en
  vérifiant que les muscles existants suffisaient.
- **Ne pas patcher un trou qu'un sprint futur va combler proprement.** Un facteur d'ampleur maintenant
  aurait masqué ce que la charge modélisera mieux. Tracer la limite, l'observer, attendre.

Le moteur sait maintenant qu'un squat ne fatigue pas les biceps. Au prochain sprint, la charge absolue
entre en jeu — et le squat lourd reprendra enfin l'ascendant qu'il mérite sur le curl.

*Stack : Java 25, Spring Boot 4, Spring Modulith, PostgreSQL. Domaine pur (zéro framework), testé avec
Testcontainers. Code et ADR [ici](#).*
