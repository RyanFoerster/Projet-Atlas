# Notes brutes pour le mini-cours `sprint-01-identity-and-ddd.md`

> Capturé au fil de l'eau pendant l'exécution, pour que la rédaction finale (S10) soit fidèle.
> Le mini-cours est LA référence DDD du projet — ces points sont prioritaires.

## 1. Auto-validation dans le constructeur canonique d'un record (pattern DDD moderne)
- L'invariant est porté par le **constructeur canonique compact** du record, pas seulement par la factory `of()`.
- Conséquence : impossible de fabriquer un value object invalide **par aucune voie**, même en appelant `new Email(...)` directement en contournant la factory.
- La factory `of()` ne fait que la **normalisation** (trim, lowercase) puis délègue au constructeur pour la validation.
- C'est LE pattern correct avec les records Java modernes. À documenter explicitement comme le template que tous les VOs du projet suivent.
- Preuve par le test `canonical_constructor_rejects_non_normalized_value` : `new Email("UPPERCASE@x.com")` échoue.

## 2. Erreur technique vs violation métier (distinction fondamentale DDD)
- Formulation à mettre telle quelle dans le mini-cours :
  > « Erreur technique = bug du caller = 500 + alerte. Violation métier = input invalide d'un humain = 400 + message clair. Ne jamais confondre. »
- `DomainException` (hiérarchie) = **violations de règles métier uniquement** → 400 + message user.
- `IllegalArgumentException` / `IllegalStateException` = **erreurs techniques** (bug appelant, UUID malformé parsé depuis source interne, état incohérent) → 500 + alerte ops.
- Application concrète dans Identity : `Email`/`DisplayName` (saisie humaine) → `DomainException` ; `UserId`/`MagicLinkToken.from()` (id technique parsé) → `IllegalArgumentException`.
- Confondre les deux = confondre les responsabilités du global exception handler. Documenté aussi dans la JavaDoc de `DomainException`.

## 3. Test « fuzzing » avec seed fixe (qualité de test supérieure)
- Remplace temporairement le property-based test jqwik (différé Sprint 4).
- `new Random(42)` → **seed fixe = reproductible** : un échec est rejouable à l'identique, pas de flakiness.
- Propriété vérifiée = **totalité de la fonction** : pour 1000 entrées aléatoires, `Email.of` renvoie soit un Email valide normalisé, soit `InvalidEmailException` — **jamais** d'exception technique (NPE, IndexOutOfBounds).
- Différence avec un test à exemples : on couvre l'espace d'entrée, pas juste les cas qu'on a imaginés.

## 4. Pattern « retour de nouvelle instance » pour les aggregates immutables (DÉCIDÉ en S2)
- **Choix retenu : « business method + constructeur de copie privé ».** Chaque comportement
  métier (`recordLogin`, `updateDisplayName`…) est une méthode publique qui porte l'intention
  ET garantit les invariants, puis délègue la recopie au constructeur canonique privé.
- **Alternatives écartées** (à expliquer dans le mini-cours) :
  - *`withXxx()` publics* (un par champ) : exposeraient une recopie champ par champ qui
    court-circuite les invariants et n'a pas de sens métier (`withLastLoginAt` ≠ `recordLogin`).
  - *Builder / toBuilder* : cérémonie injustifiée pour 7 champs ; utile surtout quand beaucoup
    de champs optionnels.
- C'est LE pattern que tous les aggregates du projet copieront.

## 5. Value object vs Entity : égalité par valeur vs par identité (distinction DDD centrale)
- **Value object** (UserId, Email, MagicLinkToken…) = défini par sa valeur → `record`
  (égalité structurelle sur tous les champs, gratuite).
- **Entity / Aggregate** (User, MagicLink) = a une **identité** qui persiste à travers les
  changements d'état → **classe** (pas record) avec `equals`/`hashCode` **sur l'id seul**.
- Test qui prouve le concept : `user.recordLogin(t)` est **égal** au `user` d'origine (même id,
  état différent) — un record l'aurait rendu non-égal. C'est faux au sens DDD.
- Piège : un débutant fait `User` en record « parce que c'est moderne » → égalité par tous les
  champs → deux états du même Player ne sont plus égaux → bugs en collection/persistence.

## 6. Module Spring Modulith OPEN pour le kernel partagé (concept Modulith)
- Par défaut un module est **CLOSED** : seul son package de base est exposé, ses sous-packages
  sont internes → un autre module ne peut PAS les référencer. C'est voulu pour les bounded
  contexts (encapsulation).
- Le `shared/` est un **kernel** voulu accessible par tous → on le déclare `@ApplicationModule(type = OPEN)`
  dans `shared/package-info.java`.
- **Vécu réel** : la 1re référence inter-module (`identity` → `shared.domain.exceptions.DomainException`)
  a fait ÉCHOUER `modules.verify()` avec « depends on non-exposed type ». Marquer shared OPEN règle ça.
- Contrepartie : le kernel doit rester minimal (un kernel qui grossit = couplage global déguisé).
- ⚠️ À soumettre à Ryan au gate S5 : est-ce que ce choix mérite un ADR dédié, ou la JavaDoc du
  package-info + cette note suffisent ? (C'est une implémentation de la décision kernel déjà actée,
  pas une nouvelle décision — mais traçabilité à confirmer.)

## 8. MapStruct vs aggregates DDD riches : la bonne frontière (SECTION DÉDIÉE — vaut de l'or en entretien)
- **MapStruct est conçu pour les beans anémiques** : `getX()/setX()`, constructeur public, zéro
  logique, zéro invariant. Il génère du code de copie champ-à-champ par convention de nommage.
- **Un aggregate DDD est l'anti-bean** : constructeur privé, factories métier (`register()` qui
  *génère un id*), accesseurs record-style (`id()` pas `getId()`), value objects à (dé)wrapper,
  invariants garantis au constructeur. Les 3 frictions vécues (voir ADR-015) :
  1. pas de constructeur public/builder/setter → MapStruct ne peut rien construire ;
  2. accesseurs `id()` non reconnus par la stratégie par défaut (attend `getX`) ;
  3. value objects → conversions manuelles de toute façon.
- **Pattern `reconstitute()`** :
  - *Nom* : `reconstitute` (réhydratation), distinct de la factory de création.
  - *Intention* : `register()` = « un nouveau Player naît » (génère identité + timestamps + règles).
    `reconstitute()` = « ce Player existait déjà, on le recharge tel quel depuis le stockage ».
  - *Invariants* : passe par le MÊME constructeur privé → invariants techniques garantis, pas de
    porte dérobée.
  - *Visibilité* : `public` faute de mieux. Le package-private serait idéal mais IMPOSSIBLE ici :
    `domain.model` et `infrastructure.persistence` sont deux packages distincts (layering hexagonal
    voulu), et mettre le mapper dans `domain.model` y tirerait JPA (viole ADR-003). Modulith ne
    restreint pas la visibilité intra-module. → `public` + garde JavaDoc « FOR PERSISTENCE LAYER ONLY ».
- **Règle décisionnelle (doctrine projet, ADR-015)** :
  - frontière **aggregate ↔ persistence (JPA)** → **mapping manuel** ;
  - frontière **application ↔ DTO web** → **MapStruct** (objets anémiques par design, S6).
- Référence : Vernon (*IDDD*, Factories & Repositories), Evans (*DDD*, Factories qui reconstituent).
- Le piège du débutant : forcer MapStruct partout → soit affaiblir l'aggregate (setters publics =
  catastrophe DDD), soit contorsionner l'outil (coût > valeur). Savoir choisir la frontière = DDD profond.

## 7. Events inter-modules en types primitifs (découplage de frontière)
- Les events publics (`api/events/PlayerRegistered`, `PlayerLoggedIn`) portent `UUID`/`String`/`Instant`,
  PAS les value objects du domaine (`UserId`, `Email`).
- Raison : un event vit dans `api/` et franchit la frontière. S'il portait `UserId` (dans
  `domain.model`, non exporté), le module consommateur devrait importer le domaine d'identity →
  violation Modulith. Contrat stable et autonome > réutilisation des VOs internes.
