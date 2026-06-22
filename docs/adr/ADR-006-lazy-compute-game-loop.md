# ADR-006 : Lazy compute pour la game loop idle

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Atlas a une dimension idle / simulation continue : les athlètes virtuels progressent (ou régressent) selon les programmes qu'on leur applique, même quand le joueur est offline. Le modèle Fitness-Fatigue évolue dans le temps (décroissance exponentielle au repos), des séances virtuelles s'accumulent, des compétitions arrivent.

Trois architectures de game loop étaient possibles :

1. **Polling périodique global** (`@Scheduled`) : un job toutes les X minutes parcourt tous les athlètes actifs et recalcule leur état. Simple à comprendre mais coûteux à grande échelle, inutile pour les athlètes inactifs.

2. **Lazy compute** : on stocke l'état avec un timestamp `lastUpdated`. Quand l'athlète est query (page chargée, calcul nécessaire), on calcule la trajectoire entre `lastUpdated` et `now`. Pas de calcul si pas de query.

3. **Event scheduling** : on schedule des events futurs (drop de loot à T+30min) dans une queue persistante. Plus riche fonctionnellement mais beaucoup plus complexe pour le MVP.

## Décision

Le projet adopte **lazy compute** comme stratégie principale de game loop pour le MVP.

**Mécanique** :
- Chaque `Athlete` stocke `lastUpdated: Instant` dans son `FitnessFatigueState`.
- À chaque lecture significative (chargement de page, calcul de performance, application d'un stimulus), un domain service `AthleteTimeAdvancer` est appelé pour calculer la trajectoire entre `lastUpdated` et `now`.
- L'application de la décroissance exponentielle Fitness/Fatigue et l'exécution des séances virtuelles programmées dans l'intervalle sont calculées à la volée.
- Le nouvel état est persisté, `lastUpdated` est mis à jour.

**Avantages techniques** :
- Pas de scheduler global, donc pas de coût pour les athlètes inactifs.
- Pas de problème de "perte" si le serveur est down : la prochaine lecture rattrape tout l'intervalle.
- Le calcul est un domain service pur, testable en isolation (passer un état initial + un intervalle de temps, vérifier le nouvel état).
- Compatible avec un déploiement scale horizontal sans coordination entre instances.

**Cas spéciaux** :
- **Notifications proactives** (ex : "ton athlète a battu un PR pendant que tu n'étais pas là") : nécessitent un mécanisme complémentaire si on veut les envoyer en push. Pour le MVP, les notifications sont passives (visibles à la prochaine connexion). Post-MVP, on pourra ajouter un mini-scheduler ciblé sur les athlètes avec compétitions proches.
- **Compétitions programmées dans le futur** : nécessitent un trigger temporel. On utilisera un Spring Modulith scheduled event ou un `@Scheduled` minimal qui ne traite que la queue des compétitions à exécuter (peu nombreuses), pas tous les athlètes.

**Évolution future** :
Si l'application scale au point où la latence de "rattrapage" devient un problème (athlète inactif pendant 6 mois → calcul lourd à la première reconnexion), un cap sera mis (ex : décroissance au-delà de 30 jours est traitée comme "détraining complet"). Pas un problème en MVP.

## Conséquences

**Positives**
- Architecture beaucoup plus scalable que polling global.
- Domain service pur, testable, élégant à expliquer en entretien.
- Pas de risque de désynchronisation entre scheduler et lectures.
- Simple à implémenter en MVP.

**Négatives**
- Calculs concentrés au moment de la lecture : si le joueur revient après 3 mois d'absence, la première lecture peut prendre quelques centaines de ms. Acceptable, mitigeable par un cap.
- Pas de notifications proactives natives. Pas un manque critique en MVP.

**Neutres**
- Pattern bien connu dans les idle games (Cookie Clicker, AdVenture Capitalist, etc.), donc défendable et reconnaissable.
