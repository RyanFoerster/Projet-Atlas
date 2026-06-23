# ADR-014 : UUID v7 pour les identifiants d'aggregate, et libs purement utilitaires autorisées dans le domaine

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Le Sprint 1 introduit les premiers identifiants d'aggregate du projet (`UserId`, `MagicLinkToken` dans le module identity). Deux questions se posent, liées par leur réponse technique.

**1. Quel type d'identifiant ?** ADR-008 a déjà tranché « UUID v7, générés en application » pour les clés primaires des aggregates, mais sans formaliser ni le *pourquoi du v7* ni la *façon de le générer*. Il faut le faire ici, car c'est le premier sprint qui en génère réellement.

- **UUID v4** (aléatoire pur) : simple, supporté nativement par `java.util.UUID.randomUUID()`. Mais purement aléatoire → inséré en clé primaire d'un index B-tree PostgreSQL, chaque insert tombe à une position aléatoire dans l'index, ce qui fragmente les pages, multiplie les écritures et dégrade le cache. Connu pour pénaliser les gros volumes.
- **BIGSERIAL** (auto-incrément base) : séquentiel et compact, mais centralise la génération dans la base (impossible de connaître l'id avant l'insert, couplage fort) et expose le volume métier (ids devinables, énumérables).
- **UUID v7** (RFC 9562) : les 48 bits de poids fort sont un timestamp Unix en millisecondes, le reste est aléatoire. Résultat : **ordonné dans le temps** (donc quasi-séquentiel dans l'index, comme un BIGSERIAL) **tout en restant généré côté application** et non devinable. Le meilleur des deux mondes pour notre cas.

**2. Comment générer un UUID v7 en Java 25 ?** `java.util.UUID` ne génère nativement que les versions 3 et 4 ; il ne produit pas de v7. Il faut donc soit une lib, soit une implémentation maison. Or `UserId` et `MagicLinkToken` vivent dans `domain/`, où ADR-003 interdit toute dépendance externe au-delà de `java.*` et du `shared/domain/`. Ajouter une lib dans le domaine **demande donc d'amender ADR-003** — ou de renoncer à la lib.

Options pour la génération :
- **(a) Lib `uuid-creator`** (`com.github.f4b6a3:uuid-creator`) : implémentation éprouvée de la RFC 9562, gère correctement les détails subtils (monotonicité intra-milliseconde, gestion d'horloge). Mais introduit une dépendance dans le domaine pur.
- **(b) Générateur v7 maison** (~30 lignes) : zéro dépendance, mais c'est à nous d'assurer la correction de la spec (et ses pièges).

## Décision

### Décision 1 — UUID v7 pour tous les identifiants d'aggregate

Tous les identifiants d'aggregate et de value objects techniques (`UserId`, `MagicLinkToken`, et leurs équivalents futurs) encapsulent un **UUID v7** (RFC 9562), généré côté application via la factory `…​.generate()` du value object. On ne manipule jamais un `UUID` nu dans le domaine : il est toujours enveloppé dans un value object typé.

La génération s'appuie sur la lib **`uuid-creator`** (option a), via `UuidCreator.getTimeOrderedEpoch()`.

### Décision 2 — Amendement d'ADR-003 : les libs purement utilitaires sont autorisées dans `domain/`

ADR-003 interdisait toute dépendance externe dans `domain/`. On l'**amende** pour autoriser une catégorie précise et bornée : les **libs purement utilitaires**. Une dépendance est « purement utilitaire » — et donc admissible dans le domaine pur — si et seulement si elle respecte **les quatre critères suivants** :

1. **Pas d'I/O** : aucun accès réseau, fichier, base de données, horloge système imposée, ni aucune ressource externe.
2. **Pas de framework** : ni Spring, ni JPA/Hibernate, ni Jackson, ni aucun conteneur d'inversion de contrôle ou mécanisme d'auto-configuration.
3. **Pas d'effet de bord global** : pas d'état mutable partagé, pas de registre statique, pas de modification de l'environnement ; un appel ne dépend que de ses arguments.
4. **Implémente une spécification publique** : RFC, norme mathématique, algorithme documenté — la lib calcule une fonction bien définie (ex. « produire des octets selon la RFC 9562 »), pas une politique métier propre à un produit.

`uuid-creator` coche les quatre critères : elle transforme une horloge (passée implicitement) en octets selon la RFC 9562, sans I/O, sans framework, sans état global métier. Elle est donc autorisée dans `domain/`.

Cette règle vaut pour **tous les modules**, pas seulement identity. Elle servira notamment au Sprint 4 pour les éventuelles libs mathématiques du modèle Fitness-Fatigue de Banister (fonctions pures sur des nombres), qui relèvent exactement de cette catégorie.

Ce qui reste **interdit** dans `domain/` est inchangé : tout ce qui touche à l'infrastructure (Spring, JPA, Jackson, clients HTTP, accès DB/fichier), conformément à ADR-003.

## Conséquences

**Positives**
- Index PostgreSQL quasi-séquentiels (UUID v7) → bien moins de fragmentation et d'écritures que le v4, sans recentraliser la génération dans la base.
- Identifiants générés côté application : on connaît l'id avant l'insert, on découple le domaine de la base, on ne fuite pas le volume métier.
- La correction de la génération v7 (monotonicité, horloge) est déléguée à une lib éprouvée plutôt que réimplémentée et maintenue par nous.
- Le critère des « libs purement utilitaires » donne une **règle claire et testable** pour les futurs cas (libs math au Sprint 4), au lieu d'un débat au cas par cas. Le domaine reste pur sur le fond (zéro framework, zéro I/O), seul l'absolutisme « zéro dépendance » est assoupli.

**Négatives**
- Une dépendance de plus dans le domaine — à surveiller pour qu'elle ne devienne pas un cheval de Troie : tout ajout futur dans `domain/` doit être confronté explicitement aux quatre critères, sinon il est refusé.
- `uuid-creator` est une lib tierce mono-mainteneur ; risque faible (lib stable, spec figée) mais réel. Mitigation : la génération est encapsulée dans les factory `…​.generate()`, remplaçables sans toucher au reste du domaine.

**Neutres**
- Le choix du v7 rend les ids faiblement corrélés au temps de création (les 48 bits de timestamp sont lisibles). Acceptable ici : la date de création d'un compte n'est pas un secret. À garder en tête si un jour un identifiant doit être strictement opaque.

## Révisions

- *(création)* — Sprint 1 : formalise UUID v7 (annoncé dans ADR-008) et amende ADR-003 pour autoriser les libs purement utilitaires dans le domaine. Voir la section « Révisions » d'ADR-003.
