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

Les tiers sont rendus **distincts par le NOMBRE d'axes spécialisés ET la magnitude** du spike — pas
seulement la magnitude, sinon la frontière Promising/Specialist serait floue. L'« axe » spécialisé est
soit un pattern de force, soit un groupe musculaire (axes distincts, tirés par un shuffle seedé).

| Tier | Proba | Base (axes « moyens ») | Bande axe(s) spécialisé(s) — force / hypertrophie | Nb axes spécialisés |
|------|------:|------------------------|---------------------------------------------------|:-------------------:|
| **GENERIC**    | 65 % | 0.88 – 1.05 | — (équilibré) | 0 |
| **PROMISING**  | 25 % | 0.88 – 1.05 | 1.08 – 1.16 / 1.13 – 1.21 | 1 (modeste) |
| **SPECIALIST** |  8 % | 0.88 – 1.05 | 1.12 – 1.22 / 1.17 – 1.27 | 2 (francs) |
| **PRODIGY**    |  2 % | 0.85 – 1.12 | 1.20 – 1.25 / 1.25 – 1.30 | 1 (exceptionnel) |

> La bande hypertrophie = bande force + 0.05 (l'hypertrophie monte jusqu'à 1.30, la force à 1.25),
> bornée aux plages de `Genetics`. PRODIGY a une base un peu plus variable (« les autres axes restent
> variables »). Le tier est tiré par `RarityRoller` (probabilités cumulées), la génétique par
> `AthleteGenerator.specializedGenetics`. Bandes = *calibration par défaut, à revérifier au sprint 4*.

**Différence exacte Promising vs Specialist vs Prodigy** : Promising = **un** axe modeste ;
Specialist = **deux** axes francs (spécialisation en largeur) ; Prodigy = **un** axe exceptionnel
(spécialisation en hauteur). Vérifié par test (seed 7) : peaks GENERIC 1.042 → PROMISING 1.192 →
SPECIALIST 1.247 (2 axes ≥ 1.20) → PRODIGY 1.289 (1 axe).

**Vérifié** (test seedé 10 000 tirages) : distribution observée GENERIC 65.4 % / PROMISING 24.6 % /
SPECIALIST 8.1 % / PRODIGY 2.0 % — dans ±0.5 % de la cible.

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
