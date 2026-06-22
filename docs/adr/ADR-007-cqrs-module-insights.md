# ADR-007 : Module Insights séparé en CQRS pour le read-side analytics

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Atlas a besoin de présenter aux joueurs des statistiques riches et engageantes : tonnage cumulé, progression historique, comparaisons cross-joueurs, achievements, heatmaps par groupe musculaire, "year wrapped" annuel style Spotify, etc.

Ces lectures analytiques ont des caractéristiques très différentes des lectures métier classiques :
- **Agrégations sur de longues périodes** (toute l'historique d'un athlète, classement global)
- **Dénormalisation utile** pour la performance et la simplicité des queries
- **Pas de contrainte transactionnelle forte** avec les écritures
- **Modèle de données optimisé lecture** différent du modèle d'écriture

Deux approches étaient possibles :

1. **Mélanger les queries analytiques dans les modules existants** (Athletics expose un endpoint "stats de l'athlète", PersonalTraining expose "tonnage total", etc.). Simple mais pollue les modules métier avec de la logique d'agrégation et de présentation.

2. **Séparer en module dédié** suivant le pattern **CQRS** (Command Query Responsibility Segregation) : un module dédié `Insights` écoute les events des autres modules, maintient des projections optimisées pour la lecture, expose les queries analytiques.

## Décision

Le projet adopte le pattern **CQRS via un module Insights dédié**.

**Architecture du module** :
- Le module **Insights** écoute les events publiés par les autres modules : `WorkoutLogged`, `AthleteStatsUpdated`, `PRBeaten`, `CompetitionFinished`, etc.
- Pour chaque type d'event, un **event handler** met à jour une ou plusieurs **projections** (vues matérialisées dénormalisées).
- Les projections vivent dans des tables séparées des tables métier, optimisées pour la lecture.
- Les controllers d'Insights exposent des endpoints REST pour les dashboards Angular.

**Important** : Insights est read-only par rapport au reste du système. Il n'envoie jamais d'event, ne modifie jamais d'aggregate des autres modules. Pure consommation, pure exposition.

**Projections typiques** (MVP et au-delà) :
- `AthleteProgressionTimeSeries` : 1RM par mouvement par semaine (graphe de progression)
- `PlayerTonnageHistory` : tonnage cumulé par jour, semaine, mois, total
- `PRTimeline` : tous les PR avec date et delta
- `MuscleGroupHeatmap` : volume relatif par groupe musculaire sur les N dernières semaines
- `LeaderboardEntry` : classements globaux et locaux
- `MonthlyRecap` / `YearlyWrapped` : pré-calculé en fin de période

**Pas de full Event Sourcing en MVP** :
On ne stocke pas le journal complet des events. Si une projection doit être recalculée, on peut la reconstruire à partir des tables métier (rebuild script). Évolution possible vers full event sourcing si nécessaire.

**Distinction claire avec le product analytics** :
- **Insights** = stats pour les **joueurs** (visible dans l'app, contenu de jeu)
- **PostHog** = product analytics pour **Ryan** (rétention, funnel, dropoffs) — cross-cutting concern, pas un module

## Conséquences

**Positives**
- Démonstration claire de CQRS, pattern enterprise très valorisé en entretien tech lead.
- Les modules métier restent focalisés sur leur logique propre, pas pollués par de la logique d'agrégation.
- Performance des queries analytiques optimale via projections dénormalisées.
- Évolution facile : ajouter une nouvelle stat = ajouter un nouvel event handler + projection, sans toucher au métier.
- Couplage faible avec les autres modules (via events).
- Contenu de jeu riche et viral potentiel (year wrapped partageable).

**Négatives**
- Boilerplate des event handlers et projections.
- Risque de désynchronisation si un event est perdu — mitigé par Spring Modulith Event Publication Registry (events durables).
- Rebuild d'une projection nécessite du code custom si on veut le faire sans full event sourcing. Acceptable car rare.

**Neutres**
- L'introduction de CQRS justifie aussi pourquoi on garde un domaine pur dans les modules write-side : la séparation des responsabilités write/read renforce l'isolation.
