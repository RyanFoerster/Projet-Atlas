# ADR-004 : Modèle Fitness-Fatigue de Banister par groupe musculaire

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Atlas se positionne comme une simulation poussée de coaching fitness. La crédibilité auprès de la communauté lifting est l'asset principal du projet. Une simulation arcade-réaliste serait insuffisante : un cardio de 20 minutes ne doit pas booster la force au bench.

Le modèle de référence scientifique pour la planification d'entraînement est le modèle **Fitness-Fatigue de Banister** (1975, raffiné 1991), qui modélise deux variables opposées suite à chaque stimulus :
- **Fitness** : adaptation positive long terme (constante de temps ~30-50 jours)
- **Fatigue** : effet négatif court terme (constante de temps ~7-15 jours)
- **Performance = k1 · Fitness − k2 · Fatigue**

Trois niveaux de granularité étaient possibles :

1. **Une seule paire (fitness, fatigue) globale par athlète** — simple mais irréaliste : un athlète peut être frais au bench et cuit au squat le même jour.
2. **Une paire par groupe musculaire** (chest, back, quads, hamstrings, etc.) — modélise la localité de la récupération.
3. **Une paire par pattern moteur** (squat, bench, deadlift, OHP) — trop granulaire pour le MVP, et chevauchements entre patterns difficiles à modéliser.

## Décision

Le projet adopte le modèle Fitness-Fatigue de Banister avec **granularité par groupe musculaire**, complété par le suivi séparé des filières aérobie et anaérobie.

**Structure cible** :
- `fitnessByMuscleGroup: Map<MuscleGroup, double>` (~10-12 groupes en MVP)
- `fatigueByMuscleGroup: Map<MuscleGroup, double>`
- `aerobicFitness, aerobicFatigue` (non localisés)
- `anaerobicFitness, anaerobicFatigue` (non localisés)

**Granularité MVP** :
- ~10-12 groupes musculaires (CHEST, BACK_UPPER, BACK_LOWER, QUADS, HAMSTRINGS, GLUTES, SHOULDERS, BICEPS, TRICEPS, CALVES, CORE)
- 5-6 patterns moteurs (SQUAT, BENCH_PRESS, DEADLIFT, OVERHEAD_PRESS, ROW, CHIN_UP)
- 5 filières énergétiques (STRENGTH, HYPERTROPHY, ENDURANCE_ANAEROBIC, ENDURANCE_AEROBIC, POWER)

**Évolution progressive** (option B validée précédemment) :
- **Sprint 4** : implémentation initiale avec 1 stat globale uniquement, pour valider le câblage event-driven et la persistence.
- **Sprint 5** : raffinement vers granularité par groupe musculaire et pattern.
- **Post-MVP** : ajout de la modélisation d'interférence avancée, de la nutrition, du sommeil, etc.

**Calibration empirique** :
La calibration des constantes du modèle (τ_fitness, τ_fatigue, multiplicateurs k1/k2, courbes de gain par niveau d'entraînement) se fait via des **scénarios de simulation de référence** dans `athletics/test/calibration/` :
- Scénario "débutant 5/3/1 12 semaines" → attendu +15-25kg cumulés
- Scénario "avancé cycle force 16 semaines" → attendu +2-5kg
- Scénario "deload post-accumulation" → vérification surcompensation
- Scénario "interférence aérobie-force" → réduction de 20-30% des gains de force
- Scénario "détraining 8 semaines" → perte ~10-15% force, ~5-8% masse

Ces scénarios sont basés sur la littérature scientifique (Helms, Israetel, Nuckols, Banister, Schoenfeld).

**CurrentStats long terme distinctes** :
Le modèle distingue clairement :
- **Fitness/Fatigue** : adaptation neuromusculaire court/moyen terme (semaines)
- **CurrentStats** : capacités structurelles long terme (1RM réel, masse musculaire, VO2max)

Un deload baisse la fitness mais pas la masse musculaire. C'est ce qui rend le modèle psychologiquement et physiologiquement crédible.

## Conséquences

**Positives**
- Crédibilité scientifique forte, défendable face à la communauté lifting.
- Modèle riche et différenciant par rapport aux jeux fitness simplistes.
- Logique métier complexe et intéressante à modéliser, à tester (property-based), à expliquer en entretien.
- Évolution progressive (1 stat → par groupe musculaire) compatible avec le scope MVP.

**Négatives**
- Calibration empirique demande du travail itératif sur plusieurs semaines.
- Plus difficile à comprendre pour un joueur casual (mitigation : UI qui simplifie la présentation, "Form" globale visible plutôt que les 12 paires individuelles).
- Risque de progression perçue trop lente pour les joueurs casuals (mitigation : accélération temporelle in-game, gratification via achievements et compétitions).

**Neutres**
- L'utilisation d'équations différentielles discrètes (calcul exponentiel à chaque mise à jour) est performante et compatible avec le pattern lazy compute.

## Références

- Banister, E.W. (1975). *A systems model of training for athletic performance*. Aust J Sports Med.
- Helms, E., Morgan, A., Valdez, A. *The Muscle and Strength Pyramids*.
- Israetel, M., Hoffmann, J., Smith, C.W. *Scientific Principles of Strength Training*.
- Nuckols, G. — Stronger By Science (méta-analyses).
- Schoenfeld, B. — méta-analyses hypertrophie.
