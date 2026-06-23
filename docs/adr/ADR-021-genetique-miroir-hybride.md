# ADR-021 : Génétique hybride de l'athlète miroir (influencée par les 1RM)

**Statut** : Accepté
**Date** : Sprint 2
**Décideur** : Ryan Foerster

## Contexte

L'athlète miroir progresse en fonction des vraies séances du joueur (hook IRL, vision §). À sa création,
il faut lui attribuer une génétique. Trois options :

1. **Aléatoire pure** : mauvaise UX — un joueur fort (1RM élevés) pourrait hériter d'une génétique
   médiocre, ce qui casse le lien « c'est mon avatar ».
2. **Inférence pure depuis les 1RM** : déduire toute la génétique des performances. Complexe, et
   scientifiquement bancal (la performance = génétique × entraînement × technique ; on ne peut pas
   isoler la génétique).
3. **Hybride** : génétique de base aléatoire, dont les axes de force sont *influencés* par les ratios
   force/poids de corps saisis.

## Décision

**Hybride** (option 3), implémenté dans `AthleteGenerator.generateGeneticsForMirror` :

1. Le joueur saisit ses 4 grands 1RM (squat, bench, deadlift, OHP) et son poids de corps.
2. La génétique de base est **tirée aléatoirement** (seedé) dans les plages normales.
3. Pour chaque lift saisi, on calcule le **ratio force/BW** et un niveau de **talent** `t ∈ [0,1]` par
   rapport aux standards du pattern **et du genre** :
   `t = clamp((ratio − seuil_intermédiaire) / (seuil_elite − seuil_intermédiaire), 0, 1)`.
4. L'affinité de force du pattern est **relevée** vers `1.0 + t × 0.20` (jamais abaissée), **plafonnée
   à 1.20** — la frange 1.20–1.25 restant réservée à l'aléa génétique pur (plafond dur de `Genetics` :
   1.25). Les autres axes (récupération, fibres, hypertrophie…) restent purement aléatoires.

> **Boost miroir plafonné à 1.20 (sub-cap de la plage 0.80–1.25)** pour réserver la frange 1.20–1.25 à
> l'aléa génétique pur des candidats scoutés, traduisant que **la mesure par 4 lifts ne peut pas saturer
> le potentiel génétique théorique** : le miroir reflète tes performances *mesurées*, pas un potentiel
> idéal qu'on ne peut pas observer par 4 lifts. (Réconcilie une suggestion initiale de plafond 1.30 — qui
> aurait violé l'invariant `Genetics` 0.80–1.25 — en deux niveaux : sub-cap 1.20 + plafond dur 1.25.)

### Standards de force (seuils), sources citées en JavaDoc

Ratios 1RM/poids de corps, **homme adulte**, [intermédiaire ; elite] — sources **ExRx.net Strength
Standards** et **Greg Nuckols / Stronger By Science** :

| Lift | Intermédiaire | Elite |
|---|---|---|
| Squat | 1.5 | 2.3 |
| Bench | 1.0 | 1.65 |
| Deadlift | 1.75 | 2.7 |
| OHP | 0.6 | 1.0 |

**Femmes** : ratios ≈ ×0.65 (haut du corps : bench, OHP), ×0.75 (bas du corps : squat, deadlift) — ExRx.
Là où la littérature ne tranche pas, valeurs marquées *calibration par défaut, à revérifier sprint 4*.

**Plafonnement crucial** : même un bench énorme ne dépasse pas 1.20 d'affinité — sinon la cohérence du
modèle casserait au sprint 4 (application de Banister).

### Exemples vérifiés (test)

- Profil « Ryan » : bench 100 kg / BW 80 → ratio 1.25 → `t ≈ 0.38` → affinité bench **1.137**.
- Profil « Elite » : bench 150 kg / BW 80 → ratio 1.875 → `t = 1` (saturé) → affinité bench **1.200**
  (plafonnée).

## Conséquences

**Positives**
- **Bonne UX** : un joueur fort obtient une génétique de force plausiblement bonne sur ses points forts
  — l'avatar « lui ressemble ».
- **Humilité scientifique** : une force vérifiée implique une affinité *élevée mais non maximale*
  (l'entraînement compte) ; on ne prétend pas mesurer la génétique pure.
- **Testable** : seedé → reproductible ; les seuils sont des constantes documentées et sourcées.

**Négatives**
- Calibration sensible : des seuils mal choisis fausseraient le ressenti. Mitigé par les sources citées
  et la revérification prévue au sprint 4.

**Neutres**
- MVP binaire sur le genre ; affinement (catégories de poids, âge) possible plus tard.
