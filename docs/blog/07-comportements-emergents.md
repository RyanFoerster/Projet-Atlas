---
title: "La physiologie que je n'ai pas codée : rendements décroissants, plateau et auto-régulation émergents"
date: 2026-06-30
author: Ryan Foerster
tags: [atlas, devblog, sport-science, modeling, emergence, feedback-loop, ddd]
status: draft
---

# La physiologie que je n'ai pas codée

Dans [Atlas](#), mon jeu de gestion d'écurie d'athlètes de force où la simulation repose sur de la vraie
sport-science, j'ai passé les sprints précédents à construire la **forme** : combien un athlète est affûté un
jour donné, par groupe musculaire, modulé par sa génétique. Ce sprint, j'ai ajouté la pièce qui manquait : la
**force réelle qui progresse dans le temps** — le 1RM qui monte, séance après séance, mois après mois.

Et la chose la plus intéressante de tout le sprint, c'est ce que je **n'ai pas eu à écrire**. Trois
comportements physiologiques que tout lifter connaît — les *newbie gains*, le plateau, et le ralentissement
quand on force toujours pareil — sont **sortis tout seuls** d'un modèle qui ne les mentionne nulle part.
Voici comment.

## Le piège que je voulais éviter : tout coder à la main

La façon naïve de simuler la progression de force, c'est une table de cas : « si débutant, +5 kg par
semaine ; si intermédiaire, +2 kg ; si avancé, +0,5 kg ; si plateau détecté, stopper ». Ça marche, et c'est
une horreur. Chaque seuil est arbitraire, chaque transition est un palier brutal, et le jour où un joueur
fait un truc imprévu (s'arrêter deux mois, changer radicalement de volume), la table ne sait pas répondre.

Je voulais l'inverse : **un seul modèle continu** d'où ces comportements **émergent** comme conséquences, pas
comme cas particuliers. Voici la formule que j'ai retenue (ADR-033). Le 1RM « mérité » par la charge chronique
accumulée `C` vise un plafond génétique, en convergeant vers lui :

```
mérité(C) = plafond − (plafond − départ) · exp(−C / SCALE)
```

C'est tout. Une exponentielle qui s'approche d'un plafond sans jamais l'atteindre. Regardons ce qui en tombe.

## Comportement émergent n°1 : les *newbie gains*

Loin du plafond — un débutant — l'exponentielle est **raide** : chaque dose de charge chronique produit un
gros saut de 1RM. Près du plafond — un athlète confirmé — la courbe s'**aplatit** : la même dose ne donne
presque rien. Le « newbie qui prend 20 kg au squat en trois mois pendant que le compétiteur national en
gratte deux » n'est pas codé quelque part. C'est **l'écart au plafond** qui pilote tout, et la même formule
produit les deux athlètes.

Ma calibration le confirme : sur 12 semaines identiques, un débutant prend **+19 kg**, un athlète avancé
**+7 kg**, avec un écart de **×2,3** entre les profils génétiques extrêmes — et tout le monde reste **sous son
plafond**. Aucun `if trainingAge`. Aucun palier. Juste `exp(−C/SCALE)` et un plafond individualisé par la
génétique.

## Comportement émergent n°2 : le plateau, qui est une *feature*

Voici celui qui m'a fait sourire. Si un athlète s'entraîne **à charge constante** — toujours 120 kg au squat,
semaine après semaine — son 1RM **plafonne**. Pas parce que j'ai écrit « détecter la stagnation », mais parce
que la charge chronique `C` se **stabilise** : elle accumule à chaque séance et décroît avec une constante de
temps de 90 jours, et au point d'équilibre, l'accumulation compense exactement la décroissance. `C` se fige,
donc `mérité(C)` se fige.

Et c'est **physiologiquement correct**. Un corps s'adapte à une charge donnée puis cesse de progresser : il
faut **augmenter** la charge pour reprovoquer une adaptation. C'est le principe de surcharge progressive, le
plus fondamental de la musculation. Mon modèle le reproduit sans le connaître. « Mon athlète ne progresse plus
à 120 kg » n'est pas un bug à patcher — c'est le jeu qui dit *mets plus lourd*.

## Comportement émergent n°3 : la boucle d'auto-régulation

Le plus beau pour la fin. J'avais aussi ajouté ce sprint la **charge** dans le calcul du stimulus : une série
compte d'autant plus qu'elle est lourde **en pourcentage du 1RM** (`loadFactor`, fonction du %1RM). Et le
1RM, justement, c'est ce que le modèle précédent fait monter.

Regardez la boucle qui se forme toute seule :

> le 1RM monte → la même charge absolue (disons 100 kg) devient un **%1RM plus bas** → `loadFactor` baisse →
> le stimulus de cette charge baisse → la progression ralentit.

C'est une **boucle de rétroaction négative**. L'athlète qui soulève toujours exactement le même poids voit son
travail devenir, relativement, de plus en plus léger à mesure qu'il se renforce — donc de moins en moins
stimulant. Exactement ce qui se passe dans la vraie vie. Je n'ai écrit aucune ligne pour « ralentir la
progression quand l'athlète force pareil » : la boucle le fait, et — vérifié en calibration — elle **converge**
au lieu de s'emballer.

Mon seul vrai travail sur cette boucle n'a pas été de la *calmer*, mais de **ne pas la casser** : il fallait
relire le 1RM **frais** à chaque séance (jamais le mettre en cache), sinon le feedback lit une valeur périmée
et n'amortit rien. La donnée mutable se relit, toujours.

## Trois horloges qui ne se mélangent pas

Tout ça ne tient que parce que le sprint sépare proprement **trois échelles de temps** dans le même athlète :

- la **fatigue** (jours) — monte vite, descend vite ;
- la **forme** (semaines) — l'affûtage neuromusculaire ;
- le **1RM structurel** (mois) — quasi-irréversible.

Mon test du gate conceptuel le prouve de bout en bout : un athlète s'entraîne 12 semaines, son squat passe de
140 à **144,72 kg**. Puis il se repose 6 semaines : sa **forme s'effondre** (elle perd un facteur e sur une
constante de temps) **mais son 1RM reste à 144,72 kg, au gramme près**. Un deload te rend rouillé, pas faible.
Si ces trois horloges partageaient une seule variable, le repos effacerait la progression — et tout
s'écroulerait. Les garder séparées, c'est ce qui permet à l'émergence de tenir.

## Ce que je retiens

- **Chercher l'émergence avant de coder le comportement.** Une bonne forme fonctionnelle (ici une convergence
  exponentielle vers un plafond) produit gratuitement des comportements riches que des `if` arbitraires
  imiteraient mal. Quand un comportement « tombe tout seul » du modèle, c'est généralement signe que le modèle
  capture quelque chose de réel.
- **Un plateau n'est pas toujours un bug.** Avant de patcher « l'athlète stagne », demande-toi si la stagnation
  n'est pas le bon comportement — et si la vraie réponse n'est pas dans les mains du joueur (charge plus lourd).
- **Une boucle de feedback, on la branche bien, on ne la bricole pas.** L'auto-régulation est stable parce que
  les ingrédients (échelles de temps séparées, cible convergente, lecture fraîche du mutable) sont corrects.
  Le rôle du dev est de ne pas casser la boucle, pas de la dompter à coups de constantes.

Le moteur de simulation d'Atlas est maintenant complet : la charge entre dans la dose, la force monte dans le
temps, et la physiologie que je voulais — rendements décroissants, plateau, auto-régulation — est là **sans
que je l'aie écrite**. C'est, je crois, la meilleure preuve que le modèle dit quelque chose de vrai.

*Stack : Java 25, Spring Boot 4, Spring Modulith, PostgreSQL. Domaine pur (zéro framework), calibré par
simulation et testé avec Testcontainers. Code et ADR [ici](#).*
</content>
