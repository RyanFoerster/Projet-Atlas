---
title: "Mon premier aggregate DDD : ce que User m'a appris"
date: 2026-06-23
author: Ryan Foerster
tags: [atlas, devblog, ddd, java, spring, architecture]
status: draft
---

# Mon premier aggregate DDD

Je construis **Atlas**, un « Football Manager du lifting » : tu diriges une écurie d'athlètes de force,
tu les programmes, tu les amènes en compétition, avec une simulation qui prend la sport science au
sérieux. Le cœur du jeu — le modèle Fitness-Fatigue — sera de la pure logique métier complexe. Avant
d'y toucher, je voulais poser proprement les fondations DDD. Le Sprint 1 a donc été le module
**Identity** : connexion, compte, onboarding.

Sauf que je n'avais **jamais fait de DDD**. J'ai 9 mois de Java derrière moi, datant d'il y a trois ans,
et un quotidien plutôt Next.js / NestJS / Prisma. Alors « aggregate », « value object », « domaine pur »,
c'étaient des mots de conférence pour moi. Ce post raconte ce que m'a appris l'écriture de mon premier
vrai aggregate : `User`. Spoiler — l'essentiel tient en une phrase : **un aggregate DDD n'est pas un
sac de getters et de setters**.

## Le réflexe qu'il faut désapprendre

Voilà comment j'aurais modélisé un utilisateur il y a un mois :

```java
@Entity
class User {
    @Id Long id;
    String email;
    String displayName;
    Instant lastLoginAt;
    // + getters, + setters, + une couche "Service" qui fait la logique à côté
}
```

C'est ce qu'on appelle un **modèle anémique** : des données nues, et toute la logique éparpillée dans
des `UserService` qui grossissent jusqu'à devenir des classes de 800 lignes. Ça marche. Mais la logique
métier n'est nulle part *dans* le métier — elle flotte autour, et chaque développeur qui arrive doit
reconstituer mentalement les règles à partir de dix méthodes de service.

Le DDD inverse ça : **les règles vivent dans les objets du domaine**. Et pour que ça tienne, ces objets
doivent être impossibles à mettre dans un état invalide. C'est là que tout commence.

## Étape 1 : des value objects qui refusent l'invalide

Avant l'aggregate, ses briques. Un email n'est pas une `String` — c'est un `Email`, et un `Email` qui
existe est *forcément* valide :

```java
public record Email(String value) {
    private static final Pattern PATTERN =
        Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    public Email {                          // constructeur canonique du record
        if (value == null || value.isBlank()) throw new InvalidEmailException("…");
        if (value.length() > 254)            throw new InvalidEmailException("…");
        if (!PATTERN.matcher(value).matches()) throw new InvalidEmailException("…");
    }

    public static Email of(String raw) {     // normalise PUIS valide
        return new Email(raw.trim().toLowerCase());
    }
}
```

Le détail qui change tout : la validation est dans le **constructeur**, pas seulement dans la factory
`of()`. Du coup, même `new Email("BIDON")` échoue. Une fois que je tiens un `Email`, je *sais* qu'il est
valide — je ne le re-vérifierai jamais. Le type porte la garantie. Multiplie ça par `UserId`,
`DisplayName`, `MagicLinkToken`, et soudain ta signature de méthode raconte la vérité :
`register(Email, DisplayName, …)` ne peut pas recevoir n'importe quoi.

Première leçon : **le record Java est l'outil parfait pour les value objects** — immutable, égalité par
valeur, concis. À condition de valider dans le constructeur compact.

## Étape 2 : l'aggregate, et le piège du record

Naïvement, j'ai voulu faire `User` en `record` aussi. *« C'est moderne, c'est immutable, banco. »*

Erreur. Et c'est LA leçon DDD du sprint.

Un value object est défini par sa **valeur** : deux `Email` identiques sont interchangeables. Mais un
`User` a une **identité** qui persiste à travers ses changements d'état. Le `User` avant son login et le
`User` après son login, c'est le *même* Player. Ils doivent être **égaux**. Or un `record` génère une
égalité sur *tous* les champs — donc dès que `lastLoginAt` change, les deux `User` ne seraient plus
égaux. Faux. Bugs garantis en collection et en persistance.

Donc `User` est une **classe**, immutable, avec une égalité **par identité** :

```java
public final class User {
    private final UserId id;
    private final Email email;
    private final Instant lastLoginAt;   // null tant qu'aucun login
    // … champs finals, constructeur privé

    public User recordLogin(Instant now) {      // ne mute pas : retourne une NOUVELLE instance
        return new User(id, email, displayName, locale, timezone, createdAt, now);
    }

    @Override public boolean equals(Object o) {
        return o instanceof User other && id.equals(other.id);   // par IDENTITÉ
    }
    @Override public int hashCode() { return id.hashCode(); }
}
```

Pas de setter. Aucun. Chaque comportement métier (`recordLogin`, `updateDisplayName`…) **retourne une
copie** modifiée. C'est un style fonctionnel : l'aggregate ne change pas, il *produit* sa version
suivante. Les invariants (un login ne peut pas précéder la création, par exemple) sont vérifiés dans le
constructeur privé, donc *toute* nouvelle instance est valide par construction.

Deuxième leçon : **value object = record (égalité par valeur) ; aggregate = classe (égalité par
identité)**. Confondre les deux, c'est faire du « DDD » en surface tout en cassant sa sémantique en
profondeur.

## Étape 3 : le persister sans le trahir

Un domaine pur, c'est bien. Mais il faut le sauver en base. Et là, deuxième piège.

Ma règle (héritée de la littérature DDD) : le domaine ne connaît **ni Spring ni JPA**. Les entités JPA
vivent séparément, en infrastructure. Reste à mapper l'une vers l'autre. Réflexe moderne : MapStruct,
qui génère le code de mapping. J'ai essayé. Ça s'est effondré sur trois points :

1. `User` n'a **pas de constructeur public** ni de setters → MapStruct n'a aucune porte d'entrée.
2. Ses accesseurs sont `user.id()`, pas `getId()` → la convention par défaut de MapStruct ne les voit pas.
3. Il faut (dé)wrapper les value objects à la main de toute façon.

Le diagnostic m'a pris du temps, mais la conclusion est nette : **MapStruct est conçu pour des beans
anémiques** (getters/setters, constructeur public). Un aggregate riche en est l'exact opposé. Forcer
l'outil m'aurait obligé soit à **affaiblir l'aggregate** (ajouter des setters = saboter tout ce que je
venais de construire), soit à **contorsionner MapStruct** pour un résultat à moitié écrit à la main.

J'ai donc écrit un **mapper manuel** — explicite, lisible, sans magie :

```java
public User toDomain(UserJpaEntity e) {
    return User.reconstitute(
        new UserId(e.getId()), Email.of(e.getEmail()), DisplayName.of(e.getDisplayName()),
        Locale.forLanguageTag(e.getLocale()), ZoneId.of(e.getTimezone()),
        e.getCreatedAt(), e.getLastLoginAt());
}
```

Note le `reconstitute()`. Recharger un Player depuis la base ne doit PAS passer par `register()` — qui
*génère* un id et fixe la date de création. C'est une factory de **réhydratation** : « ce Player
existait déjà, on le recharge tel quel ». Elle passe par le même constructeur privé, donc les invariants
restent garantis. La distinction `register()` (création) vs `reconstitute()` (réhydratation) est un
classique DDD (Vernon, Evans) que je n'avais jamais croisé avant — et qui résout proprement le problème.

Troisième leçon, plus subtile : **savoir où un outil aide et où il nuit**. MapStruct est excellent — à
la frontière `application ↔ DTO web`, où les objets *sont* anémiques par design. À la frontière
`domaine ↔ base`, il se bat contre la richesse qu'on a voulue. La maturité, c'est de choisir la
frontière, pas d'appliquer l'outil partout.

## Étape 4 : 400 ou 500 ?

Dernier réflexe que j'ai dû acquérir : distinguer une **violation de règle métier** d'une **erreur
technique**. Un email mal tapé par un humain n'est pas un bug — c'est une entrée invalide, qui mérite un
**400 + message clair**. Un UUID malformé parsé depuis ma propre base, lui, est un bug — un **500 +
alerte**.

J'ai matérialisé ça avec une hiérarchie `DomainException` (pour le métier) distincte des
`IllegalArgumentException` (pour le technique), et un handler qui traduit la première en 400. La règle
que je me répète :

> Erreur technique = bug du caller = 500 + alerte. Violation métier = input invalide d'un humain = 400
> + message clair. Ne jamais confondre.

Les mélanger, c'est réveiller l'astreinte pour un email mal saisi.

## Ce que je généralise

Mon premier aggregate ne fait « rien de spectaculaire » : un Player, un email, un nom, une date de
login. Mais le pattern qu'il pose vaut pour tout le reste d'Atlas — et, je pense, pour tout domaine
métier sérieux :

- **Pousse les invariants dans les types.** Un objet du domaine ne doit pas pouvoir être invalide.
- **Distingue valeur et identité.** Record pour les value objects, classe (égalité par id) pour les
  aggregates. Ce n'est pas un détail de style, c'est la sémantique.
- **Garde le domaine pur, mappe explicitement.** Le boilerplate de mapping manuel est le prix de la
  liberté du domaine — et il est plus lisible que la magie générée.
- **Nomme tes échecs.** 400 métier vs 500 technique, dès la conception.

DDD a la réputation d'être lourd, théorique, sur-architecturé. Sur un module aussi simple qu'Identity,
ça peut sembler beaucoup de cérémonie pour un formulaire de login. Mais ce module est un **investissement
de pattern** : quand j'attaquerai Athletics — la simulation Fitness-Fatigue, le vrai cœur complexe — je
n'aurai pas à réfléchir à la structure. Je copierai `User`. Et le jour où un recruteur ou un dev senior
ouvrira le code d'Atlas, c'est probablement Identity qu'il lira en premier.

Autant qu'il y trouve un vrai aggregate, pas un sac de setters.

---

*Atlas est en développement, documenté au grand jour. Le code, les ADR et les mini-cours sont publics —
parce que la confiance d'une communauté technique se gagne en montrant le travail, pas en le cachant.*
