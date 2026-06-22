# ADR-005 : Lien IRL ↔ jeu via athlète miroir et déblocage de programmes

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Le hook fondamental d'Atlas est le lien entre les vraies séances d'entraînement du joueur (loggées dans l'app) et la progression dans le jeu. Trois modèles étaient envisagés :

1. **Modèle "ta séance = leur séance"** : tous les athlètes virtuels font la même séance que le joueur IRL. Problème majeur : impossible si les athlètes sont en phases différentes (un en pic de force, un en hypertrophie, un en deload).

2. **Modèle "tes séances débloquent des programmes"** : compléter un cycle IRL débloque le template. Lien indirect mais cohérent.

3. **Modèle "tu pilotes un athlète miroir"** : un athlète spécifique de l'écurie progresse en fonction des vraies séances du joueur. Les autres sont gérés purement en simulation virtuelle.

## Décision

Le projet adopte un **modèle hybride : athlète miroir + déblocage de programmes**.

**Athlète miroir** :
- Un athlète unique dans l'écurie du joueur est désigné comme "athlète miroir" — c'est l'avatar lifter du joueur.
- Quand le joueur logge une séance IRL dans le module **PersonalTraining**, un event `WorkoutLogged` est publié.
- Le module **Athletics** consomme cet event et applique le stimulus calculé à l'athlète miroir, et uniquement à lui.
- L'athlète miroir progresse donc à la vitesse réelle du joueur, avec son rythme, sa cohérence, ses pauses.

**Déblocage de programmes** :
- Quand le joueur complète un cycle complet de programme IRL (ex : 12 semaines de 5/3/1), un event `ProgramCycleCompleted` est publié par **PersonalTraining**.
- Le module **Programming** consomme cet event et débloque le template du programme correspondant.
- Une fois débloqué, ce programme peut être appliqué à n'importe quel athlète virtuel de l'écurie.
- Les athlètes virtuels eux progressent en simulation pure (pas de lien IRL), à vitesse accélérée dans le temps du jeu.

**Détection du programme suivi IRL** :
En MVP, le joueur déclare manuellement quel programme il suit (sélection dans une liste). Plus tard, détection automatique par analyse de la structure de ses séances.

**Désignation de l'athlète miroir** :
À la création de compte, le joueur crée son athlète miroir avec son nom, sa génétique (saisie ou calculée à partir de ses 1RM réels), ses caractéristiques. Il ne peut y en avoir qu'un par compte.

## Conséquences

**Positives**
- Le joueur a une raison concrète de continuer à s'entraîner IRL : son athlète miroir progresse à son rythme.
- Les autres athlètes restent jouables et permettent une expérience de coach même quand le joueur ne s'entraîne pas (vacances, blessure, repos).
- Le déblocage de programmes apporte une gratification long terme : compléter un vrai cycle IRL devient un événement marqué dans le jeu.
- Pas de friction entre les phases d'entraînement différentes des athlètes (chacun a son programme indépendant).
- Audience plus large : un joueur qui ne s'entraîne pas peut quand même jouer en mode coach pur, sans athlète miroir actif.

**Négatives**
- Modélisation un peu plus complexe que "tout est miroir" ou "tout est simulation pure".
- Risque que les joueurs sentent leur athlète miroir "à la traîne" par rapport aux athlètes virtuels qui progressent plus vite (accéleration in-game). Mitigation : framing UI clair, valorisation du miroir comme "le vrai toi", possibilité d'achievements spécifiques au miroir.

**Neutres**
- L'athlète miroir est techniquement un Athlete comme les autres, avec un flag `isMirror: true` sur le profil. Pas de modélisation séparée nécessaire dans l'aggregate.
