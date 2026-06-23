# Sprint 02 — Roster, génération procédurale & réutilisation d'un template DDD

> Mini-cours plus court que le sprint 1 : les fondations DDD/hexagonal/Modulith sont posées (cf.
> `sprint-01-identity-and-ddd.md`). Ici on capitalise — et on apprend ce qui change quand on
> *réutilise* un pattern au lieu de le découvrir.

## Ce qu'on a appris

Le sprint 1 nous a appris le DDD. Le sprint 2 nous a appris **si le DDD tient sous pression** : un
deuxième module, une structure de données plus riche (un aggregate avec une collection d'entités), une
mécanique de jeu (génération procédurale), et une nouvelle frontière de persistance (JSONB). Six concepts.

---

## Concept 1 : Réutiliser un template DDD (et ce que ça coûte vraiment)

### Définition
Le module `roster` a été construit en **copiant la structure** du module `identity` : mêmes couches
(`api`/`domain`/`application`/`infrastructure`), mêmes patterns (value objects auto-validants,
`reconstitute()`, ports/adapters, mappers manuels, `DomainException`, events en records).

### Pourquoi c'est important
Un template DDD n'a de valeur que s'il se **réutilise sans le réinventer**. Le vrai test : combien
faut-il *adapter* ? Réponse mesurée ce sprint — la structure se copie à 100 %, mais la *modélisation*
s'adapte (collection d'entités, VO à Maps, JSONB). On n'a inventé aucune architecture : 4 ADRs nouveaux,
tous bâtis sur ~8 décisions existantes réutilisées.

### Comment c'est utilisé dans Atlas
Cas emblématique : `ScoutedCandidate` a réutilisé **consciemment le pattern `MagicLink`** (objet
temporaire à TTL, consommé une fois). On n'a pas inventé un pattern — on a *reconnu* qu'un pattern
existant s'appliquait. Et on l'a **vérifié par l'architecture** : un test ArchUnit prouve que le module
respecte le squelette (4 couches, domaine pur, mappers manuels, events = records).

### Exemple minimal hors Atlas
Un `PasswordResetToken` et un `EmailVerificationToken` partagent le même pattern « jeton temporaire à
usage unique » que `MagicLink`. Les reconnaître comme *un seul pattern* évite trois implémentations
divergentes.

### Pièges classiques
- **Copier-coller sans adapter la modélisation** : le template donne la *structure*, pas le *modèle*.
- **Réinventer un pattern qu'on a déjà** : si tu écris un nouveau « truc temporaire », demande-toi
  d'abord s'il ne ressemble pas à un objet existant.

### Pour aller plus loin
Vaughn Vernon, *Implementing DDD*, ch. sur les *Bounded Contexts* et la réutilisation de building blocks.

---

## Concept 2 : Aggregate avec entité interne vs deuxième aggregate root

### Définition
Un **aggregate** est une grappe d'objets traités comme une unité de cohérence, avec **une seule racine**
(aggregate root) comme point d'entrée. Une **entity interne** vit *dans* l'aggregate : elle a une
identité, mais pas de repository propre.

### Pourquoi c'est important
La décision « `Athlete` est-il un aggregate root ou une entity interne de `Roster` ? » change tout :
les invariants, la persistance, l'API. Se tromper crée soit des incohérences (deux racines qui se
marchent dessus), soit un aggregate géant.

### Comment c'est utilisé dans Atlas
`Roster` est le **seul** aggregate root ; `Athlete` est une **entity interne** (ADR-019). Conséquences
concrètes :
- pas d'`AthleteRepository` — on charge le `Roster` et on navigue ;
- pas de factory publique sur `Athlete` — c'est `Roster.addMirror()`/`recruit()` qui orchestre ;
- l'invariant « un seul miroir » se protège **au niveau `Roster`** ;
- persistance via `@OneToMany` (cascade + orphanRemoval), `AthleteMapper` = helper interne du `RosterMapper`.

### Exemple minimal hors Atlas
`Order` (root) + `OrderLine` (entity interne) : on ne sauve jamais une `OrderLine` seule, on sauve la
commande entière. L'invariant « total = somme des lignes » vit sur `Order`.

### Pièges classiques
- Donner un repository à l'entity interne « parce que c'est pratique » → on perd la cohérence de l'aggregate.
- Charger l'aggregate entier sans transaction → `LazyInitializationException` (vécu : corrigé avec
  `@Transactional` sur l'adapter de lecture).

### Pour aller plus loin
Evans, *DDD*, le chapitre Aggregates ; règle « un aggregate = une transaction ».

---

## Concept 3 : Génération procédurale déterministe

### Définition
Générer du contenu (ici, des athlètes) par algorithme plutôt qu'à la main. **Déterministe** : la même
*seed* produit toujours le même résultat.

### Pourquoi c'est important
Sans déterminisme, on ne peut pas **tester** une génération aléatoire (chaque run diffère), ni vérifier
une distribution, ni reproduire un bug. Le hasard doit être **injecté** (seed/`Random` en paramètre),
jamais caché dans un `new Random()` interne.

### Comment c'est utilisé dans Atlas
`ProceduralAthleteGenerator.generateCandidate(seed, rarity)` est pur et seedé. On peut donc tester :
« même seed → même candidat », et vérifier la distribution de rareté sur **10 000 tirages seedés**
(observé 65.4/24.6/8.1/2.0 %, à ±0.5 % de la cible) — un test déterministe, pas *flaky*.

### Exemple minimal hors Atlas
`new Random(42).nextInt()` donne toujours la même suite. Un générateur de niveaux de jeu seedé permet de
rejouer exactement le même niveau en partageant juste la seed (cf. Minecraft).

### Pièges classiques
- `new Random()` caché dans le domaine → intestable et non reproductible.
- Calibration « au feeling » sans la sourcer : nos seuils de force sont sourcés (ExRx, Nuckols/Stronger
  By Science) et marqués « à revérifier sprint 4 ».

### Pour aller plus loin
« Property-based testing » (jqwik, arrive au sprint 4) pour tester des *invariants* sur des entrées aléatoires.

---

## Concept 4 : Rareté par spécialisation (vs gatcha classique)

### Définition
Modéliser la rareté comme une **spécialisation** (exceptionnel sur un axe) plutôt qu'un **niveau global**
(« meilleur partout »).

### Pourquoi c'est important
Le « Légendaire 99 en tout » est scientifiquement faux (personne n'est élite partout) et porteur de
pay-to-win. La spécialisation est crédible *et* anti-gatcha.

### Comment c'est utilisé dans Atlas
Les tiers diffèrent par le **nombre d'axes spécialisés** ET la magnitude (ADR-020) : Generic 0 spike,
Promising 1 axe modeste, Specialist 2 axes francs, Prodigy 1 axe exceptionnel + base plus variable (le
« génie déséquilibré »). C'est *visible* dans l'UI : un Phénomène a un pic net et des points faibles assumés.

### Exemple minimal hors Atlas
Football Manager : un ailier *wonderkid* a une accélération d'élite mais une finition moyenne. Sa rareté
= la spécialisation extrême, pas « 20 partout ».

### Pièges classiques
- Différencier les tiers par la *magnitude seule* → frontières floues (Promising/Specialist se
  chevauchent). On l'a corrigé en ajoutant la dimension « nombre d'axes ».

---

## Concept 5 : Immutabilité réelle avec des collections

### Définition
Un value object immutable contenant une `Map`/`List` n'est *vraiment* immutable que si la collection est
**copiée défensivement** à la construction (sinon l'appelant garde une référence mutable).

### Pourquoi c'est important
`record Genetics(Map<...> ...)` ne suffit PAS : un record stocke la *référence* passée. Si l'appelant
mute sa map après, l'« immutable » change. Faille silencieuse.

### Comment c'est utilisé dans Atlas
Le constructeur compact de `Genetics` valide *puis* fait `Map.copyOf(...)` (copie non modifiable). Prouvé
par test : muter la map d'entrée après construction ne change rien à la `Genetics`.

### Exemple minimal hors Atlas
```java
record Team(List<String> players) {
  Team { players = List.copyOf(players); } // sans ça, l'appelant peut muter la liste interne
}
```

### Pièges classiques
- Croire que `record` = immutable. Il l'est pour les champs *de surface*, pas pour le contenu des
  collections/objets mutables qu'il référence.

---

## Concept 6 : JSONB sans Jackson dans le domaine (3 couches)

### Définition
Stocker une structure complexe (la `Genetics`) en colonne `jsonb` PostgreSQL, **sans** importer Jackson
dans le domaine pur.

### Pourquoi c'est important
Le domaine doit rester pur (ADR-003 : zéro framework). Mais la sérialisation JSON, c'est Jackson. Il faut
une frontière.

### Comment c'est utilisé dans Atlas
Trois couches (ADR-019, extension d'ADR-015) :
1. **domaine pur** : `Genetics` (record, zéro Jackson) ;
2. **DTO d'infrastructure** (`persistence/json/`) annoté `@JdbcTypeCode(SqlTypes.JSON)` — support JSON
   *natif* d'Hibernate 7, **aucune dépendance ajoutée** (pas de hypersistence-utils) ;
3. **mapper manuel** qui traduit domaine ↔ DTO JSON.

Prouvé sur vrai PostgreSQL : round-trip exact (égalité par valeur) et `jsonb_typeof = 'object'` (vrai
jsonb natif, pas du text/bytea).

### Pièges classiques
- Mettre `@Column(columnDefinition="jsonb")` sans `@JdbcTypeCode(SqlTypes.JSON)` → Hibernate ne sait pas
  sérialiser.
- Laisser Jackson fuiter dans le domaine « pour aller vite » → la pureté est perdue, dur à récupérer.

---

## Bonus : ce qu'on a appris du multi-module

Au sprint 1, `shared` (le kernel) était quasi vide — et ça ne se voyait pas, car tout vivait dans un seul
module. `UserId` était dans `identity.domain.model` (interne à un module CLOSED). Le sprint 2 a **révélé
la dette** : `Roster.ownerId` est un `UserId`, mais roster ne peut pas importer l'intérieur d'identity
(Spring Modulith l'interdit). Le mono-module *masquait* l'incohérence ; le multi-module l'a forcée au jour.

Leçon : **un kernel partagé ne se peuple correctement que sous la pression d'un deuxième consommateur.**
On a promu `UserId`, `Weight`, `OneRepMax`, `MovementPattern`, `MuscleGroup` vers `shared` (chacun
validé contre les deux critères d'ADR-017 : transverse ET fondamental).

---

## Auto-évaluation

1. Pourquoi `Athlete` n'a-t-il pas de repository propre dans le module roster ?
2. Qu'est-ce qui casse si on génère un athlète avec `new Random()` au lieu d'une seed injectée ?
3. En quoi « rareté par spécialisation » diffère-t-elle d'un système de niveau global, côté gameplay ET
   côté crédibilité scientifique ?
4. Un `record Genetics(Map<...> m)` est-il immutable ? Que faut-il ajouter, et pourquoi ?
5. Décris les 3 couches qui permettent de stocker la `Genetics` en jsonb sans Jackson dans le domaine.
6. Pourquoi le kernel `shared` était-il sous-peuplé jusqu'au sprint 2 ?
7. Qu'est-ce qu'un test ArchUnit vérifie qu'un test unitaire classique ne vérifie pas ?
