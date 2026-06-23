# ADR-020 : Système de rareté par spécialisation (vs niveau global)

**Statut** : Accepté
**Date** : Sprint 2
**Décideur** : Ryan Foerster

## Contexte

Le scouting d'athlètes virtuels introduit une notion de rareté. Deux façons de la modéliser :

1. **Rareté par niveau global** (gatcha classique) : un tier rare = « meilleur partout » (le fameux
   « Legendary 99 en tout »). Simple, mais scientifiquement faux et porteur de pay-to-win.
2. **Rareté par spécialisation** : un tier rare = exceptionnel sur **un axe précis**, les autres axes
   restant variables. Personne n'est élite sur tout.

Atlas est positionné sur le réalisme scientifique (vision §3) et l'anti-gatcha (§4). Un athlète
« 99 partout » contredit la réalité de la sport-science : un powerlifter d'exception sur le squat n'a
pas forcément un développé couché d'élite ni une récupération parfaite.

## Décision

La rareté est de la **spécialisation**, pas du niveau global :

- **GENERIC (65 %)** : équilibré, tous les axes proches de la moyenne (faible variance).
- **PROMISING (25 %)** : un axe modestement au-dessus.
- **SPECIALIST (8 %)** : un axe clairement haut.
- **PRODIGY (2 %)** : un axe **exceptionnel** (haut de la plage), les autres axes **variables** (variance
  élargie). Jamais « tout en haut ».

L'« axe » pointé est soit un pattern de force, soit un groupe musculaire (tiré aléatoirement). Le tier
est tiré par `RarityRoller` (probabilités cumulées), la génétique correspondante par `AthleteGenerator`
(`specializedGenetics`). Les bandes de spike sont *calibration par défaut, à revérifier au sprint 4*.

**Vérifié** (test seedé 10 000 tirages) : distribution observée GENERIC 65.4 % / PROMISING 24.6 % /
SPECIALIST 8.1 % / PRODIGY 2.0 % — dans ±0.5 % de la cible. Et un PRODIGY pointe un axe à ~1.23 quand un
GENERIC plafonne à ~1.09.

## Conséquences

**Positives**
- **Crédibilité scientifique** : aucun athlète n'est élite partout — fidèle à la réalité.
- **Diversité de gameplay** : chaque PRODIGY est différent (axe et magnitude variables) → vraie valeur
  de découverte au scouting, pas un simple chiffre de puissance.
- **Anti-gatcha** : la rareté n'est pas « plus de puissance brute », c'est « une spécificité » — pas de
  pay-to-win.

**Négatives**
- Plus complexe à communiquer à l'UI qu'un simple « niveau » (il faut montrer *sur quel axe* l'athlète
  est doué). Géré par l'affichage de la génétique sur la fiche.

**Neutres**
- Les probabilités et bandes sont des paramètres de calibration ; ils évolueront avec l'équilibrage
  gameplay et les scénarios Banister du sprint 4.
