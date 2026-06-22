# Vision — Atlas

> Document de référence pour la vision long terme du projet. Lu par tout agent IA qui assiste le développement et par Ryan lui-même pour rester aligné sur le "pourquoi" du projet quand le quotidien technique brouille la perspective.

---

## Le pitch en trois phrases

Atlas est un jeu de coaching fitness type Football Manager où le joueur dirige une écurie d'athlètes de force et les amène en compétition, avec une simulation poussée basée sur la sport science réelle. Un athlète miroir progresse en fonction des vraies séances d'entraînement du joueur, et compléter un cycle de programme IRL débloque ce programme pour l'appliquer à ses athlètes virtuels. Pas de gatcha prédateur, pas de free-to-play déguisé : un produit premium one-shot pour une communauté qui prend le lifting au sérieux.

---

## Le problème qu'on adresse

La gamification fitness existe et marche (Strava, Hevy, Strong, Forge), mais elle reste superficielle : badges, streaks, classements de tonnage. Personne ne propose une vraie simulation de coaching qui prend la sport science au sérieux et qui transforme le lifting en un univers de jeu profond.

À l'inverse, le marché des jeux de simulation sportive est dominé par Football Manager et ses dérivés (Out of the Park Baseball, Motorsport Manager, etc.) — mais aucun ne couvre le lifting/strength sports. Or, c'est un domaine qui s'y prête remarquablement : programmation cyclique, périodisation, pic de forme pour les compétitions, individualité génétique, gestion de la fatigue. Tous les ingrédients d'un grand jeu de gestion existent.

**Atlas vise le croisement de ces deux mondes** : un Football Manager du lifting, avec un hook IRL qui crée un lien unique entre la pratique réelle du joueur et son expérience de jeu.

---

## Pour qui

**Cible primaire** : lifters sérieux qui s'intéressent à la programmation. Powerlifting, strongman, bodybuilding scientifique, calisthenic avancée. Ils suivent des programmes structurés, lisent Stronger by Science, écoutent Iron Culture, suivent Helms et Israetel. Ils ont déjà 1-5+ ans d'expérience et ont une opinion sur 5/3/1 vs nSuns vs Sheiko.

**Cible secondaire** : fans de jeux de simulation/gestion qui découvrent le sujet. Football Manager players, OOTP players, joueurs de simulation qui apprécient les systèmes profonds. Ils n'ont pas forcément l'expertise lifting mais ils sont attirés par la profondeur mécanique.

**Cible tertiaire** : débutants curieux qui veulent apprendre la programmation. Le jeu devient un outil éducatif déguisé. C'est aussi le pipeline d'acquisition long terme — un débutant qui joue à Atlas pendant 6 mois en apprend plus sur la programmation que beaucoup de pratiquants amateurs.

---

## Positionnement et différenciation

**Ce qu'Atlas est** :
- Une simulation de coaching profonde, scientifiquement crédible
- Un produit premium one-shot, payé une fois, joué autant qu'on veut
- Un objet de fierté communautaire — quelque chose que les nerds du lifting montrent à leurs amis
- Un produit qui respecte le temps et l'attention du joueur

**Ce qu'Atlas n'est PAS** :
- Pas un tracker fitness (Forge fait ça mieux, et c'est un autre produit)
- Pas un jeu mobile gatcha prédateur à daily login
- Pas un Strava-like social où le but est de battre ses potes
- Pas un coach virtuel qui te dit quoi soulever (le joueur reste maître de ses choix IRL)
- Pas un jeu casual à 5 minutes par session — c'est un jeu de gestion exigeant qui respecte son joueur

**Différenciateurs clés** :
1. **Réalisme scientifique** comme arme compétitive. Cardio pur n'augmente pas le bench, point. La communauté lifting le reconnaît immédiatement.
2. **Hook IRL via l'athlète miroir** que personne ne peut copier sans avoir leur propre app de tracking.
3. **Monétisation honnête** dans un marché saturé d'arnaques gatcha.
4. **Profondeur mécanique** héritée de la vraie science du sport, pas inventée pour le jeu.

---

## Principes design non-négociables

Ces principes guident toutes les décisions produit. Si une feature les viole, elle est rejetée même si elle "marcherait" commercialement.

**1. Réalisme > Arcade**
Quand on doit choisir entre crédibilité scientifique et fun arcade, on choisit la crédibilité. Le fun vient de la profondeur, pas du dopage de chiffres. C'est notre fossé compétitif.

**2. Respect du joueur**
Pas de FOMO, pas de daily login obligatoire, pas de notifications anxiogènes, pas de timer pay-to-skip, pas de mécanique de rétention prédatrice. Le joueur joue quand il en a envie, et est récompensé de la même façon que les autres.

**3. Premium one-shot, jamais de monétisation invasive**
Un prix juste à l'achat (~15€), c'est tout. Pas de loot box payant. Si on ajoute du contenu majeur, ce sera une extension payante explicite, ou un DLC, jamais un système de microtransactions in-game. Le joueur sait ce qu'il paie.

**4. Pas de tricherie de l'extérieur**
Le système doit refléter la science, pas la satisfaction immédiate. Si une feature pousse à mentir sur ses workouts pour gagner dans le jeu, c'est mauvais. Si elle pousse à mieux s'entraîner ou à mieux programmer pour gagner, c'est bon.

**5. Transparence des mécaniques**
Les joueurs avancés doivent pouvoir comprendre les formules. La doc des mécaniques est publique (devblog, wiki communautaire encouragé). Pas de boîte noire qui sentirait l'arnaque.

**6. Communauté avant marketing**
Le jeu se construit avec et pour la communauté lifting. Beta fermée tôt, devblog technique honnête, présence sur les forums lifting (r/weightroom, r/powerlifting, r/StartingStrength, r/Stronger\_by\_Science). Le bouche-à-oreille communautaire est notre canal d'acquisition principal.

---

## Modèle de monétisation

**MVP et v1** : premium one-shot à environ 15€. Pas d'abonnement, pas de microtransactions. Le joueur paie une fois et a accès complet.

**Démo gratuite envisagée** : version d'essai temporaire (premier mois, ou jusqu'au premier meet) pour permettre l'évaluation. À décider en bêta.

**Évolutions long terme possibles** :
- Extensions payantes (~5-10€) qui ajoutent des verticales : Strongman, Olympic Lifting, Crossfit, etc.
- Pack cosmétiques optionnel pour personnaliser l'écurie (zéro impact gameplay).
- Version "club" pour les coachs réels qui veulent utiliser Atlas comme outil pédagogique (modèle SaaS B2B léger, post-validation marché).

**Jamais** :
- Loot box, gatcha payant, RNG payant
- Pay-to-win
- Subscription obligatoire
- Énergie/timers à débloquer par paiement
- Publicités

---

## Roadmap stratégique

**Phase 1 — MVP (mois 0 à 6)**
Bootstrap, modélisation Athletics, premier gameplay loop, beta fermée avec 10-20 lifters expérimentés. Objectif : valider que le modèle est crédible et que le hook IRL fonctionne.

**Phase 2 — Lancement public (mois 6 à 9)**
Polish, marketing communautaire (Reddit, Discord lifting, podcasts science), lancement payant. Objectif : 500-2000 ventes la première année, communauté Discord active.

**Phase 3 — Itération long terme (mois 9+)**
Roadmap publique avec votes communautaires, extensions thématiques, amélioration continue du modèle de simulation. Le projet devient un produit vivant porté par sa communauté.

---

## Ce qui justifie l'investissement personnel

Au-delà du produit lui-même, ce projet sert trois objectifs liés pour Ryan :

1. **Vitrine technique pour un poste tech lead** — démontrer la maîtrise d'une architecture DDD moderne (Spring Modulith, modular monolith, hexagonal), du modélisation de domaine complexe, et de l'ingénierie produit complète (de la vision au déploiement live).

2. **Apprentissage profond** — utiliser ce projet pour solidifier Java/Spring moderne et acquérir DDD en autonomie. Le mode prof-élève avec Claude (cf. CLAUDE.md) est central dans cette logique.

3. **Side project potentiellement rémunérateur** — si l'exécution est bonne et la communauté répond, Atlas peut devenir une source de revenu complémentaire stable. Marché de niche mais avec un willingness-to-pay élevé.

Ces trois objectifs s'alignent : un projet techniquement excellent attire la communauté, qui valide le marché, qui devient une référence pour la recherche d'emploi. Pas de conflit, juste un projet bien fait.

---

## Inspirations explicites

- **Football Manager** (Sports Interactive) — profondeur de simulation, attachement aux joueurs/athlètes virtuels, satisfaction long terme.
- **Out of the Park Baseball** — gestion analytique, communauté hardcore.
- **Loop Hero** — innovation mécanique, format inhabituel, succès indépendant.
- **Melvor Idle** — modèle premium one-shot dans un marché saturé de free-to-play prédateur, succès communautaire.
- **Stronger by Science**, **3D Muscle Journey**, **Renaissance Periodization** — rigueur scientifique appliquée au coaching, ton respectueux du pratiquant.

---

*Document vivant — révisable à chaque pivot stratégique significatif. Maintenu par Ryan Foerster.*
