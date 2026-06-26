---
title: "Modéliser l'adaptation à l'entraînement : le modèle de Banister en Java"
date: 2026-06-26
author: Ryan Foerster
tags: [atlas, devblog, sport-science, banister, simulation, java, ddd, modeling]
status: draft
---

# Modéliser l'adaptation à l'entraînement : le modèle de Banister en Java

Je construis [Atlas](#), un jeu de gestion d'écurie d'athlètes type Football Manager, mais où la
simulation d'entraînement repose sur de la vraie sport-science. Ce sprint, j'ai attaqué le cœur : faire
qu'une séance d'entraînement *fasse réellement progresser* un athlète, selon un modèle scientifiquement
défendable. Voici ce que j'ai appris en codant le **modèle de Banister** — et, surtout, ce qui se passe
quand la littérature s'arrête au milieu du gué.

## Le modèle, en une équation

Le modèle Fitness-Fatigue de Banister (1975) dit qu'une séance n'est pas « du progrès ». C'est une
**impulsion** qui déclenche deux réponses opposées :

- la **fitness** (adaptation positive) monte modérément et redescend **lentement** (constante de temps
  `τ ≈ 42 jours`) ;
- la **fatigue** monte fortement et redescend **vite** (`τ ≈ 7 jours`).

Et la performance disponible un jour donné :

```
performance = k1 · fitness − k2 · fatigue        (k1 = 1, k2 = 2)
```

La beauté du truc : la **supercompensation** n'est pas codée, elle *émerge*. Juste après une grosse
séance, la fatigue domine → tu es « cuit », performance basse. Quelques jours plus tard, la fatigue
(τ court) s'est effacée bien plus que la fitness (τ long) → ta performance **dépasse** le niveau de
départ. C'est tout le principe du deload et de la périodisation, qui tombe gratuitement de deux constantes
de temps différentes.

## Le piège : il n'existe aucune valeur pour « séance → impulsion »

Voici ce que les articles ne disent pas. Le modèle de Banister a été calibré pour la **performance
d'endurance**, où l'impulsion d'une séance se mesure en TRIMP (*training impulse* = durée × intensité
cardiaque). Pour la **musculation**, il n'existe **aucune valeur de littérature** pour convertir « 5×5 au
squat à RPE 8 » en une impulsion scalaire de Banister.

J'avais deux options :

1. Inventer une formule et la présenter comme « basée sur la science ».
2. Construire une formule raisonnable, **dire explicitement** que c'est une calibration maison, et la
   valider par simulation.

J'ai choisi (2). C'est, je pense, le point le plus important de tout le sprint. Ma formule au sprint 4 :

```
S = NORM · Σ (reps × effort(rpe))
```

Deux des trois variables de dose de la musculation : le **volume** (reps, driver primaire — Schoenfeld &
Krieger) et l'**intensité d'effort** (`effort(rpe) = rpe/10`, proximité de l'échec — Helms). La troisième,
l'intensité de **charge** (%1RM), je l'ai *volontairement exclue* : le RPE capture déjà l'intensité
relative à la capacité (RPE 8 = 2 reps en réserve, quel que soit le poids absolu). Et dans le code, en
toutes lettres :

```java
/**
 * Les constantes de temps proviennent de la littérature endurance classique (Banister 1975 ; Calvert
 * 1976). Aucune valeur de littérature n'existe pour transposer ce modèle à une « forme » de musculation :
 * ces constantes sont donc une calibration par défaut, validée et ajustée par les scénarios de simulation.
 */
public static final double TAU_FITNESS_DAYS = 42.0;
public static final double TAU_FATIGUE_DAYS = 7.0;
```

Modéliser honnêtement *là où la science s'arrête* — c'est ce qui rend un modèle défendable face à une
communauté qui s'y connaît. Les lifters sérieux verront tout de suite si un athlète progresse n'importe
comment. Mieux vaut dire « voici mon hypothèse » que faire semblant.

## L'ingénierie : forme récursive discrète + lazy compute

Comment calculer l'état d'un athlète à l'instant *t* ? La tentation est de ré-intégrer tout l'historique
des séances à chaque lecture. Mauvaise idée : coûteux, et ça ne passe pas à l'échelle quand tu as des
milliers d'athlètes idle.

À la place, **forme récursive discrète** : on ne stocke que l'état courant + son timestamp, et on décroît
exponentiellement à la demande.

```java
public FitnessFatigueState applyStimulus(FitnessFatigueState s, TrainingStimulus stim, Instant at) {
    FitnessFatigueState decayed = decayedTo(s, at);                 // exp(−Δt/τ) depuis lastUpdated
    return new FitnessFatigueState(
            decayed.fitness() + stim.magnitude(),                  // même impulsion sur les deux :
            decayed.fatigue() + stim.magnitude(),                  // l'asymétrie vit dans τ et k1/k2
            at);
}
```

L'exponentielle est sans mémoire : décroître en deux étapes = décroître en une seule. La récurrence est
donc *exacte*, et l'état stocké (deux `double` + un `Instant`) suffit. C'est le pattern *lazy compute* :
pas de scheduler qui tick les athlètes, on calcule au moment où on lit.

## La validation : la courbe comme preuve

Un modèle peut passer cinquante tests unitaires et produire une trajectoire absurde sur trois mois. Donc
le vrai test n'est pas unitaire : c'est une **simulation de 12 semaines** (4 séances/sem, deload en
semaine 7) qui imprime sa trajectoire, et dont on juge la **forme**.

```
Semaine | Fitness | Fatigue | Performance
   1    |   2.900 |   1.847 |    -0.793   ← « cuit » au démarrage
   6    |  11.943 |   2.914 |     6.115   ← pic d'accumulation
   7    |  10.546 |   1.350 |     7.846   ← DELOAD : la fatigue s'effondre, la perf BONDIT (+28 %)
  12    |  15.266 |   2.911 |     9.444
```

La supercompensation est là, émergente : pendant le deload, la performance bondit de +28 %, et tout le
bloc suivant s'installe sur un plateau plus haut. Et — détail qui compte — le test n'asserte pas une
valeur absolue (elle dépend de `NORM`, mon paramètre d'échelle arbitraire) mais une **forme** : *la chute
relative de fatigue est plus grande que la chute relative de fitness pendant le deload*. C'est vrai quelle
que soit l'échelle.

Un point qui m'a fait réfléchir : pendant le deload, la fitness baisse de ~12 %. Bug ? Non — **feature**.
La fitness de Banister modélise l'affûtage *court terme*, qui doit redescendre quand tu réduis le volume.
C'est distinct de la force *structurelle* (le 1RM réel), qui, elle, ne bouge pas. Cette distinction
court/long terme est le cœur de la crédibilité : un athlète qui se repose une semaine perd de la fraîcheur,
pas de la force. Allonger la constante de temps pour « adoucir » le dip aurait brouillé exactement ce que
je cherchais à montrer.

## Ce que je retiens

- **Sourcer ce qui existe, assumer ce qui n'existe pas.** L'honnêteté épistémique n'est pas un aveu de
  faiblesse, c'est ce qui rend le modèle critiquable — donc crédible.
- **Valider par simulation, pas seulement par assertions.** Et asserter la forme, pas les valeurs liées à
  un paramètre d'échelle.
- **Dérisquer par phasing.** J'ai fait la stat globale d'abord (architecture), le par-muscle viendra
  ensuite (science). Si un athlète progresse mal au sprint prochain, je saurai que c'est la science, pas
  le câblage.

Le pont IRL ↔ jeu est désormais un vrai moteur : tes vraies séances loggées font évoluer la forme de ton
athlète miroir, selon Banister. Au prochain sprint, on passe au modèle par groupe musculaire et à
l'individualisation génétique. C'est là que ça devient vraiment intéressant.

*Stack : Java 25, Spring Boot 4, Spring Modulith, PostgreSQL. Tout le domaine est pur (zéro framework),
testé avec Testcontainers. Le code et les ADR sont [ici](#).*
