# Sprint 2 — Roster minimal + premier athlète

> Prompt complet à coller dans Claude Code pour exécuter le Sprint 2. Premier sprint qui réutilise le template DDD posé au Sprint 1, premier sprint de modélisation métier riche (génétique, rareté, scouting).

---

## Contexte projet (lecture préalable obligatoire)

Avant d'exécuter ce sprint, relis dans cet ordre :

1. `CLAUDE.md` à la racine — contexte général, conventions, mode de collaboration, design system §6
2. `docs/vision.md` — pourquoi on construit Atlas
3. `docs/domain/glossary.md` — vocabulaire métier officiel (Athlete, Roster, Genetics, MirrorAthlete)
4. Tous les ADRs dans `docs/adr/` (001 à 018)
5. `docs/learning/sprint-01-identity-and-ddd.md` — mini-cours du sprint précédent, **réutilisation centrale ce sprint**
6. `docs/sprints/sprint-01-identity-onboarding/RETROSPECTIVE.md` — leçons concrètes du sprint 1
7. `docs/design-system/atlas-design-system.md` et `preview.html` — pour toute production frontend
8. Le code du module Identity (notamment User aggregate, Email value object, mapper manuel, AuthService) — **c'est le template à copier**

Si une de ces lectures est obsolète ou que tu identifies une contradiction avec ce prompt, **arrête et signale à Ryan** avant de coder.

---

## Objectif du Sprint 2

À la fin de ce sprint, un Player peut :

1. Créer son **athlète miroir** en saisissant ses vrais 1RM (squat, bench, deadlift, OHP)
2. **Scouter** de nouveaux athlètes virtuels un par un (génération procédurale avec rareté basée sur la spécialisation)
3. **Accepter ou refuser** chaque candidat scouté
4. Voir la **liste de son écurie** sur une page Roster
5. Consulter la **fiche détaillée** de chaque athlète (stats, génétique, profil)

Et techniquement, le projet aura :

- Un **module Roster bootstrapé complètement** avec aggregate Roster + entité Athlete + value objects Genetics et CurrentStats
- Un **mécanisme de scouting procédural** isolé en domain service stateless
- Un **système de rareté basé sur la spécialisation** (Generic / Promising / Specialist / Prodigy)
- La **réutilisation du template DDD** posé au sprint 1 (value objects auto-validants, aggregate immutable, mappers manuels, ports/adapters)
- Un **test de réutilisation** : le pattern Identity sert effectivement de template, on prouve qu'on n'invente pas un nouveau pattern à chaque module
- Un **mini-cours sprint-02** plus court que le précédent (les concepts DDD sont posés, on capitalise)
- Un **devblog post #3** "Réutiliser un pattern DDD : combien ça coûte vraiment ?" — sujet original et frappant en entretien

---

## Décisions prises en amont (à respecter, ne pas re-débattre)

| Décision | Choix retenu | Justification courte |
|----------|--------------|---------------------|
| Périmètre | B — Sprint complet réaliste (miroir + génétique complète) | Centrale au modèle métier, repousser créerait de la dette |
| Génétique du miroir | C — Hybride : 1RM saisis influencent une génétique aléatoire | Évite mauvaise UX (génétique de merde alors que le joueur est bon) sans coder une inférence pure complexe |
| Athlètes virtuels | Génération procédurale avec rareté | Cohérent avec le thème Football Manager (recrutement, pas création ex nihilo) |
| 1RM à saisir | A — Les 4 grands obligatoires (squat, bench, DL, OHP) | Universellement mesurés par les lifters sérieux, suffisants pour initialiser CurrentStats |
| /home après sprint 2 | B — Redirection vers /roster (ou création miroir si vide) | Premier flow naturel pour un user qui se reconnecte |
| Roster | A — Aggregate à part entière | Invariants futurs (taille max, règles d'éligibilité), évite refactor plus tard |
| Système de rareté | Spécialisation, pas niveau global | Cohérent avec réalisme scientifique : Prodigy = exceptionnel sur 1 axe, pas 99 partout |
| Mode scouting | Un athlète à la demande (bouton "scout") | Pas de pool/économie au sprint 2 — vrai système au sprint 6+ |
| Système complet de recrutement | Reporté au sprint 6+ | Demande économie temporelle (besoin du système idle Banister du sprint 4) |

---

## Modélisation métier

### L'aggregate Athlete (entité racine du module Roster)

```
Athlete (aggregate root)
├── id: AthleteId (UUID v7)
├── rosterId: RosterId (owner)
├── name: AthleteName (value object 2-50 caractères, similaire à DisplayName)
├── age: Integer (16-50 au sprint 2, validation au constructeur)
├── bodyWeight: Weight (value object, déjà défini dans shared)
├── bodyHeight: Height (nouveau value object, en cm)
├── gender: Gender (enum: MALE, FEMALE)
├── genetics: Genetics (value object immutable)
├── currentStats: CurrentStats (value object — sera enrichi au sprint 4)
├── rarity: Rarity (enum: GENERIC, PROMISING, SPECIALIST, PRODIGY)
├── isMirror: boolean
├── recruitedAt: Instant
└── Behaviors:
    + static createMirror(rosterId, name, age, bodyWeight, bodyHeight, gender, oneRepMaxes, generator): Athlete
    + static recruit(rosterId, candidate, now): Athlete
    + currentOneRepMax(pattern): OneRepMax (computed depuis CurrentStats)
    + ageInYears(now): Integer
```

### Le value object Genetics (les 5 axes)

Repris directement de la spec dans CLAUDE.md §4 et glossary.md :

```
Genetics {
  hypertrophyPotentialByMuscleGroup: Map<MuscleGroup, double>  // ex: 0.85 à 1.30
  strengthAffinityByPattern: Map<MovementPattern, double>       // ex: 0.80 à 1.25
  baseRecoveryRate: double                                       // ex: 0.85 à 1.20
  fiberTypeProfile: double                                       // 0 (pure endurance) à 1 (pure force)
  trainingResponseSensitivity: double                            // bruit dans la réponse
}
```

**Invariants** :
- Toutes les valeurs sont dans des plages cohérentes (validées au constructeur)
- `fiberTypeProfile` entre 0 et 1
- Génétique immutable une fois créée (record)

### Le value object CurrentStats au sprint 2

Version simplifiée du CurrentStats final (sprint 4 va l'enrichir avec masse musculaire, VO2max, etc.) :

```
CurrentStats {
  oneRepMaxByPattern: Map<MovementPattern, OneRepMax>
}
```

Au sprint 2, on a seulement les 1RM. C'est suffisant pour la fiche athlète. Les autres stats (masse musculaire estimée, VO2max, bodyFatPercentage) viendront au sprint 4 avec Athletics.

### Le value object Rarity

```
public enum Rarity {
    GENERIC(0.65),
    PROMISING(0.25),
    SPECIALIST(0.08),
    PRODIGY(0.02);

    private final double probability;
    // ...
}
```

Rappel important : **la rareté est de la spécialisation, pas du niveau global**. Generic = équilibré, Prodigy = exceptionnel sur 1 axe précis avec d'autres axes variables. Pas "Legendary 99 partout".

### L'aggregate Roster

```
Roster (aggregate root)
├── id: RosterId
├── ownerId: UserId (le Player qui possède ce roster)
├── athletes: List<Athlete> (relation gérée comme partie de l'aggregate)
├── createdAt: Instant
└── Behaviors:
    + static createFor(ownerId, now): Roster
    + addMirror(name, age, bodyWeight, bodyHeight, gender, oneRepMaxes, generator): Roster (immutable)
    + recruit(candidate, now): Roster
    + mirrorAthlete(): Optional<Athlete>
    + virtualAthletes(): List<Athlete>
    + hasMirror(): boolean
    + size(): int
```

**Invariants** :
- Un Roster a au plus **un seul athlète miroir** (`isMirror = true`)
- Pas de taille max au sprint 2 (viendra avec CoachLevel au sprint 9)
- Un athlète appartient à un seul Roster

### Domain services

**`AthleteGenerator`** — domain service stateless qui génère un candidat à scouter.

Signature conceptuelle :
```
AthleteGenerator {
  generateCandidate(seed: Long, rarityRoll: double): AthleteCandidate
  generateGeneticsForMirror(oneRepMaxes: Map<MovementPattern, OneRepMax>, bodyWeight: Weight, seed: Long): Genetics
}
```

`AthleteCandidate` est un value object DTO contenant tout ce qu'il faut pour créer un Athlete : name, age, bodyWeight, bodyHeight, gender, genetics, baseOneRepMaxes, rarity. C'est ce que l'utilisateur voit avant d'accepter.

**`RarityRoller`** — domain service stateless qui détermine le tier de rareté à partir d'un nombre aléatoire (0 à 1).

**Important pour la testabilité** : tous les services qui utilisent du hasard doivent prendre un `Random` ou `Long seed` en paramètre, jamais générer en interne. Sinon impossible de tester les invariants de façon reproductible.

### Génération hybride de la génétique du miroir

Spécifique au miroir (athlète procédural normal n'a pas ce traitement) :

1. Le joueur saisit ses vrais 1RM (squat, bench, DL, OHP) et son poids de corps
2. On calcule ses **ratios force/bodyweight** (utilisés comme indicateurs de talent)
3. La génétique de base est tirée aléatoirement
4. **Les axes correspondants** sont influencés par les ratios :
   - Un haut ratio bench/BW → boost `strengthAffinityByPattern[BENCH_PRESS]`
   - Un haut ratio squat/BW → boost `strengthAffinityByPattern[SQUAT]`
   - Etc.
5. Les autres axes (récupération, fibres, etc.) restent purement aléatoires

Cette logique vit dans `AthleteGenerator.generateGeneticsForMirror()`. Documenter la formule de boost en JavaDoc avec ratio thresholds (ex: bench/BW < 0.8 = pas de boost, 1.0 = boost moyen, 1.5+ = boost fort, plafonné). Pas de magie, des seuils explicites et calibrés sur la littérature lifting.

### Game logic du scouting

Quand le joueur clique "Scout new athlete" :

1. Le backend tire un `Random` avec une seed (peut être `System.nanoTime()` ou un seed du joueur stocké, peu importe pour le MVP)
2. `RarityRoller` détermine le tier (Generic 65%, Promising 25%, Specialist 8%, Prodigy 2%)
3. `AthleteGenerator.generateCandidate()` produit un `AthleteCandidate` cohérent avec le tier
4. Le candidat est retourné au frontend sans être persisté (c'est juste une "proposition")
5. Si le joueur accepte (POST /api/roster/recruit), on crée l'Athlete et on l'ajoute au Roster
6. Si le joueur refuse, le candidat est jeté (pas de persistence du refus)

**Note** : pas d'économie au sprint 2, le joueur peut spammer scout autant qu'il veut. On verra au sprint 6+ pour limiter (économie temporelle quand le système idle existe).

---

## Réservation des numéros d'ADR (leçon rétro sprint 1)

**Réservés à l'avance pour ce sprint** : ADR-019, ADR-020, ADR-021.

| Numéro | Sujet pressenti | Quand le rédiger |
|--------|-----------------|------------------|
| ADR-019 | Aggregate Roster + relation Roster-Athlete (modélisation DDD) | Sprint 2 — début S2 |
| ADR-020 | Système de rareté par spécialisation (vs gatcha classique niveau global) | Sprint 2 — milieu S2 |
| ADR-021 | Génération procédurale hybride pour génétique miroir (1RM-influenced) | Sprint 2 — milieu S2 |

Si un autre sujet émerge qui mérite un ADR, prends le numéro suivant disponible (022, 023...). Si l'un des sujets ci-dessus s'avère ne pas mériter un ADR (décision triviale), supprime le numéro et signale-le.

---

## Portée technique

### Module Roster — structure complète

```
backend/src/main/java/dev/ryanfoerster/atlas/roster/
├── api/
│   ├── events/
│   │   ├── RosterCreated.java
│   │   ├── MirrorAthleteCreated.java
│   │   └── AthleteRecruited.java
│   └── RosterQueryPort.java
├── domain/
│   ├── model/
│   │   ├── Roster.java                      # aggregate root
│   │   ├── RosterId.java
│   │   ├── Athlete.java                     # entity dans l'aggregate Roster
│   │   ├── AthleteId.java
│   │   ├── AthleteName.java
│   │   ├── Genetics.java
│   │   ├── CurrentStats.java
│   │   ├── AthleteCandidate.java            # DTO de scouting
│   │   ├── Rarity.java                      # enum
│   │   ├── Height.java                      # nouveau VO
│   │   ├── Gender.java                      # enum
│   │   └── exceptions/
│   │       ├── InvalidGeneticsException.java
│   │       ├── InvalidAthleteNameException.java
│   │       ├── MirrorAlreadyExistsException.java
│   │       └── InvalidAgeException.java
│   ├── service/
│   │   ├── AthleteGenerator.java            # domain service stateless
│   │   └── RarityRoller.java                # domain service stateless
│   └── port/
│       ├── RosterRepository.java
│       └── AthleteRepository.java           # si nécessaire séparément
├── application/
│   ├── command/
│   │   ├── CreateMirrorUseCase.java
│   │   ├── ScoutAthleteUseCase.java
│   │   └── RecruitAthleteUseCase.java
│   ├── query/
│   │   ├── GetRosterUseCase.java
│   │   └── GetAthleteUseCase.java
│   └── eventhandler/                        # vide au sprint 2
└── infrastructure/
    ├── persistence/
    │   ├── RosterJpaEntity.java
    │   ├── AthleteJpaEntity.java
    │   ├── RosterJpaRepository.java
    │   ├── AthleteJpaRepository.java
    │   ├── RosterPersistenceAdapter.java
    │   ├── AthletePersistenceAdapter.java
    │   └── mapper/
    │       ├── RosterMapper.java            # mapper manuel (ADR-015)
    │       └── AthleteMapper.java
    ├── web/
    │   ├── RosterController.java
    │   ├── ScoutingController.java          # ou intégré dans RosterController
    │   └── dto/
    │       ├── RosterDto.java
    │       ├── AthleteDto.java
    │       ├── AthleteCandidateDto.java
    │       ├── CreateMirrorDto.java
    │       └── RecruitAthleteDto.java
    └── config/
        └── RosterModuleConfig.java
```

### Migration Flyway

- **V004__create_rosters_table.sql** : table `rosters` (id UUID PK, owner_id UUID UNIQUE FK vers users, created_at)
- **V005__create_athletes_table.sql** : table `athletes` (id UUID PK, roster_id UUID FK, name, age, body_weight_kg, body_height_cm, gender, rarity, is_mirror, genetics JSONB, current_stats JSONB, recruited_at)

**Décision sur le stockage Genetics/CurrentStats** : JSONB Postgres. Justification : ce sont des structures complexes (Maps), mais on n'a pas besoin de query dessus en SQL au sprint 2 (filtrage par génétique viendra plus tard si jamais). JSONB nous évite de créer 20 colonnes et reste évolutif. Documenter ce choix dans ADR-019.

### Index Roster

- `rosters.owner_id` UNIQUE (un Player a au plus un Roster)
- `athletes.roster_id` indexed
- `athletes (roster_id, is_mirror) WHERE is_mirror = true` UNIQUE partial index (un seul miroir par roster)

### Endpoints REST

```
POST /api/roster/mirror
  Body: {
    "name": "Ryan",
    "age": 30,
    "bodyWeightKg": 80,
    "bodyHeightCm": 178,
    "gender": "MALE",
    "oneRepMaxes": { "SQUAT": 140, "BENCH_PRESS": 100, "DEADLIFT": 180, "OVERHEAD_PRESS": 60 }
  }
  Response: 201 Created + AthleteDto
  Erreurs: 400 si invariants violés, 409 si miroir existe déjà

GET /api/roster
  Response: 200 OK + RosterDto (liste des athlètes avec leurs stats minimales)

GET /api/roster/athletes/:id
  Response: 200 OK + AthleteDto (détail complet) ou 404

POST /api/roster/scout
  Response: 200 OK + AthleteCandidateDto (candidat généré, pas encore recruté)

POST /api/roster/recruit
  Body: { ...AthleteCandidateDto reçu de /scout... }
  Response: 201 Created + AthleteDto (recruté et persisté)
  Note: on renvoie le candidat complet dans recruit, pas un id, pour éviter d'avoir à persister les candidats refusés
```

### Frontend Angular

**Pages à créer** :

- **`/roster`** : liste des athlètes du joueur. Si vide, affiche un CTA "Crée ton athlète miroir". Si miroir existe, affiche la grille des athlètes avec leurs portraits/stats résumées + bouton "Scouter un athlète"
- **`/roster/mirror/new`** : formulaire de création du miroir (nom, âge, poids, taille, genre, 4 1RM). Validation côté client + serveur
- **`/roster/scout`** : page qui affiche un candidat scouté (sa génétique visible, sa rareté indiquée), avec deux boutons "Recruter" et "Refuser → générer un autre"
- **`/roster/athletes/:id`** : fiche détail d'un athlète

**Modifications à `/home`** : redirection automatique vers `/roster` après login si user a un roster. Si user n'a pas encore de roster (= jamais créé son miroir), redirection vers `/roster/mirror/new`.

**Composants design system** :
- Réutilise Button, Input, FocusLayout déjà créés au sprint 1
- **Nouveaux probables** : un composant `Stat`/`MetricBlock` pour afficher une métrique chiffrée, un composant `Card` pour la fiche athlète dans la grille, peut-être un composant `RarityBadge` pour visualiser le tier
- Si un composant manque, applique la procédure §6 (proposer la spec, ajouter au design system, mettre à jour preview.html, puis coder)

### Tests requis

**Tests unitaires de domaine** (TDD strict comme au sprint 1) :
- `GeneticsTest` : validation des plages, immutabilité, égalité par valeur
- `RarityTest` : probabilités cohérentes (somme = 1.0)
- `HeightTest`, `AthleteNameTest` : value objects auto-validants
- `AthleteTest` : invariants, créateurs (createMirror, recruit), méthodes computed
- `RosterTest` : invariant un seul miroir, addMirror, recruit, immutabilité
- `AthleteGeneratorTest` : génération déterministe avec seed fixe (même seed → même candidat), distribution de rareté sur N tirages, génétique du miroir cohérente avec les 1RM saisis
- `RarityRollerTest` : test randomisé sur 10000 tirages, vérifier que les probabilités convergent vers les valeurs attendues à ε près

**Tests d'intégration** :
- `CreateMirrorUseCaseIntegrationTest` : flux nominal, refus si miroir existe
- `ScoutAthleteUseCaseIntegrationTest` : génération cohérente
- `RecruitAthleteUseCaseIntegrationTest` : ajout à la DB, events publiés
- `RosterControllerIntegrationTest` : endpoints REST avec MockMvc

**Tests de réutilisation du template** (NOUVEAU concept) :
- Confirme dans un test que le module Roster respecte la même structure de packages qu'Identity (api / domain / application / infrastructure)
- Confirme que le module utilise un mapper manuel (pas MapStruct) à la frontière persistence
- Confirme l'isolation Modulith (toujours)

### Coverage cible

Maintenir 80%+ sur `roster.domain.*` (enforcé par JaCoCo). Les tests TDD du domaine devraient l'atteindre naturellement.

---

## Règles d'architecture applicables (rappel — non-négociables)

- **DDD tactique strict** : domaine pur dans `domain/`, zéro Spring/JPA/Jackson (ADR-003)
- **Aggregate Roster immutable** : méthodes qui retournent de nouvelles instances
- **Value objects auto-validants** : Genetics, Height, AthleteName impossibles à construire en état invalide
- **Mappers manuels** à la frontière domain ↔ JPA (ADR-015), pas MapStruct
- **Ports & adapters** : RosterRepository dans `domain/port/`, impl dans `infrastructure/persistence/`
- **Isolation Modulith** : Roster expose son `api/`, dépend d'Identity uniquement via son `api/` (typiquement UserId)
- **Tests sur Testcontainers**, jamais H2
- **Erreur technique vs violation métier** : DomainException pour violations métier (400), IllegalArgumentException pour bugs caller (500)
- **Conventional Commits**

---

## Leçons rétro sprint 1 à appliquer ce sprint

**Issues de la rétrospective sprint 1, à mettre en pratique systématiquement** :

1. **Réserver les numéros d'ADR à l'avance** : déjà fait (ADR-019, 020, 021 réservés ci-dessus). Si tu en utilises moins, signale-le.

2. **Simuler la topologie réelle (navigateur) tôt sur les sujets cross-cutting** : dès que le backend Roster a un endpoint qui marche en curl, teste-le aussi dans le navigateur avec le frontend (même si le frontend est encore en cours). Évite de découvrir une friction CORS/CSRF au gate final.

3. **Prouver le contrat backend en curl avant de coder le frontend** : avant de coder une page Angular qui consomme un endpoint, fais un curl du endpoint pour confirmer le shape exact de la réponse. Documente le contrat trouvé dans un commentaire ou dans la PR.

4. **Garder les paliers de validation à des moments clés**, pas systématiquement. Sur ce sprint je propose : palier après modélisation domaine (avant persistence), palier après backend complet (avant frontend), palier final.

---

## Definition of Done

Le sprint 2 est considéré terminé quand TOUS ces critères sont vérifiés :

- [ ] `./mvnw clean verify` passe
- [ ] Test Modulith verify passe (régression)
- [ ] Test ModuleViolationDetectionTest passe (régression)
- [ ] Coverage domain Roster ≥ 80% (JaCoCo enforced)
- [ ] Migrations V004 + V005 appliquées en local
- [ ] Endpoint POST /api/roster/mirror fonctionne (création du miroir)
- [ ] Endpoint GET /api/roster fonctionne
- [ ] Endpoint GET /api/roster/athletes/:id fonctionne
- [ ] Endpoint POST /api/roster/scout génère un candidat
- [ ] Endpoint POST /api/roster/recruit crée un Athlete et le persiste
- [ ] Flow complet en navigateur : login → create mirror → scout → recruit → voir le roster
- [ ] Test de génération déterministe : avec seed fixe, le candidat produit est reproductible
- [ ] Test de distribution de rareté : sur 10000 tirages, les % convergent vers Generic 65% / Promising 25% / Specialist 8% / Prodigy 2% à ε près
- [ ] CI GitHub Actions verte
- [ ] ADR-019, 020, 021 rédigés et commités (ou justification d'absence si abandonnés)
- [ ] `docs/learning/sprint-02-roster-and-procgen.md` rédigé (mini-cours plus court que sprint 1, focus sur les concepts nouveaux : génération procédurale déterministe, rareté par spécialisation, réutilisation de template DDD)
- [ ] Devblog `docs/blog/03-reutiliser-un-pattern-ddd.md` rédigé
- [ ] `docs/sprints/sprint-02-roster/RETROSPECTIVE.md` rédigé
- [ ] CLAUDE.md mis à jour si nouvelles conventions émergent
- [ ] Récap pédagogique format A
- [ ] Glossary mis à jour (notamment Roster, Rarity, AthleteCandidate, AthleteGenerator)

---

## Contraintes à respecter

- **Pas de raccourcis sur le DDD.** Le template Identity est ton guide, copie-le.
- **Pas d'invention de pattern.** Si tu hésites sur un choix de modélisation, regarde comment Identity a fait et adopte le même pattern. Si Identity n'a pas de cas équivalent, signale et demande à Ryan.
- **Pas de mutation directe d'aggregate** : style fonctionnel comme au sprint 1.
- **Décisions de design métier non triviales** (ex : faut-il ajouter une stat à Genetics qu'on n'avait pas prévue ?) → confirme avec Ryan avant de trancher.
- **Pas d'optimisation prématurée du gameplay.** Le sprint 2 pose les fondations procédurales, pas un système de jeu complet.

---

## Contexte métier (le pourquoi)

Roster est le **module-pivot** du jeu. Tout passe par lui : recrutement, gestion de l'écurie, point d'entrée vers la fiche athlète. Mais sa logique métier propre est relativement simple — la richesse métier vraie est dans Athletics (modèle Fitness-Fatigue au sprint 4).

L'objectif clé du sprint 2 c'est de **valider que le template DDD posé au sprint 1 tient la route sur un deuxième module**. Si tu te retrouves à inventer un nouveau pattern à chaque ligne, c'est que le template est trop rigide ou pas bien posé. Si au contraire la modélisation se fait fluidement en suivant le pattern, c'est un signal très fort pour la suite du projet.

Deuxième objectif : **introduire le concept de rareté/procédural sans tomber dans le gatcha**. On pose la fondation d'un système qui sera enrichi au sprint 6+ avec un vrai marché de recrutement, sans s'embarquer dans son scope complet maintenant.

---

## Format de récap attendu

À la fin de ce sprint :

**Récap pédagogique de session** (format A, CLAUDE.md §7).

**Mini-cours `sprint-02-roster-and-procgen.md`** (format C). Concepts à couvrir :

1. **Réutilisation d'un template DDD** : qu'est-ce qui se copie, qu'est-ce qui doit être adapté, comment éviter de réinventer
2. **Génération procédurale déterministe** : pourquoi un seed est obligatoire pour la testabilité, comment isoler le hasard du domaine
3. **Rareté par spécialisation** : différence avec une rareté par niveau global, pourquoi c'est plus crédible scientifiquement, comment ça crée de la diversité gameplay
4. **Aggregate avec collection d'entités** : Roster contient une liste d'Athletes, pattern d'invariants au niveau aggregate (un seul miroir), différence avec un aggregate "simple" comme User
5. **JSONB pour les value objects complexes** : quand utiliser JSONB Postgres vs colonnes plates, trade-offs

Plus court que le mini-cours sprint 1 (les concepts DDD sont posés). 5 concepts vs 9 au sprint 1. Auto-évaluation 5-8 questions.

---

## Première instruction concrète

Quand tu commences l'exécution :

1. Lis toutes les sources préalables (section "Contexte projet"). Notamment le code d'Identity pour avoir le template en tête.
2. Confirme à Ryan que tu as bien tout lu, et fait une lecture critique : où penses-tu pouvoir copier le pattern Identity directement, où vois-tu une adaptation nécessaire ?
3. Propose un plan séquencé en sous-étapes (S1 à SN), 6-8 étapes pour ce sprint, avec paliers de validation.
4. Pose les questions de clarification si besoin (notamment sur la modélisation : Roster aggregate + Athlete entity dans le même aggregate, c'est bien le pattern voulu ?).
5. Attends la validation du plan avant de coder.
6. Exécute étape par étape avec paliers comme au sprint 1.

---

*Sprint 2 — Roster minimal + premier athlète. Estimé 1.5-2 semaines à 10-15h/semaine. Plus court que le sprint 1 (réutilisation du template DDD).*
