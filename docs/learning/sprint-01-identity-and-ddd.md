# Sprint 01 — Identity & DDD tactique

> Mini-cours du Sprint 1. C'est **la** référence DDD du projet : le module Identity pose le pattern
> que tous les modules suivants (Athletics, Programming…) vont copier. On va deep sur le DDD
> tactique (concepts 1 à 4), plus light sur l'outillage et le frontend (5 à 9).
>
> Format par concept : Définition → Pourquoi → Dans Atlas (code réel) → Exemple hors Atlas →
> Pièges → Pour aller plus loin. Auto-évaluation en fin.

## Ce qu'on a appris

Construire un aggregate DDD *riche* (pas un sac de getters/setters), le garder pur de tout framework,
le persister sans le trahir, l'exposer derrière une API web sécurisée, et brancher un frontend
fidèle à la prod. Au passage : où les outils (MapStruct, Spring Modulith, Angular signals) aident,
et où ils se mettent en travers.

---

## Concept 1 : Value objects auto-validants *(deep)*

### Définition
Un **value object** (VO) est un objet défini par sa **valeur**, pas par une identité. Deux VO de même
valeur sont interchangeables (`Email("a@b.com") == Email("a@b.com")`). Il est **immutable** et
**auto-validant** : il est impossible d'en construire un dans un état invalide.

### Pourquoi c'est important
Un VO déplace la validation **dans le type** plutôt que de la disperser dans des `if` partout. Une fois
que tu tiens un `Email`, tu *sais* qu'il est valide — plus jamais besoin de re-vérifier. Le compilateur
devient ton allié : une signature `register(Email, DisplayName)` ne peut pas recevoir n'importe quelle
`String`.

### Dans Atlas
Les records Java sont parfaits pour ça. Le piège classique est de ne valider que dans la factory.
Nous validons dans le **constructeur canonique**, donc *toutes* les voies de construction sont couvertes :

```java
public record Email(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    public Email {                                 // constructeur canonique : porte l'invariant
        if (value == null || value.isBlank()) throw new InvalidEmailException("…");
        if (value.length() > 254) throw new InvalidEmailException("…");   // RFC 5321
        if (!PATTERN.matcher(value).matches()) throw new InvalidEmailException("…");
    }

    public static Email of(String raw) {           // factory : normalise PUIS délègue la validation
        if (raw == null) throw new InvalidEmailException("…");
        return new Email(raw.trim().toLowerCase());
    }
}
```

Conséquence prouvée par un test : `new Email("UPPERCASE@x.com")` échoue — on ne peut pas fabriquer un
`Email` non normalisé même en contournant `of()`. C'est ça, « auto-validant ».

### Exemple minimal hors Atlas
```java
public record Percentage(int value) {
    public Percentage {
        if (value < 0 || value > 100) throw new IllegalArgumentException("0..100");
    }
}
```
`Percentage` ne peut jamais valoir 150. Tout code qui en reçoit un n'a pas à re-vérifier.

### Pièges classiques
- **Valider seulement dans la factory** → on peut créer un objet invalide via le constructeur.
- **VO anémique** (juste un wrapper sans validation) → tu as la cérémonie sans le bénéfice.
- **Normaliser dans le constructeur** → le constructeur doit *rejeter* l'invalide, pas le *réparer* ;
  la normalisation (trim, lowercase) va dans la factory.

### Pour aller plus loin
- Vaughn Vernon, *Implementing Domain-Driven Design*, ch. « Value Objects ».
- JEP records + « compact constructors ».

---

## Concept 2 : Aggregate immutable + `reconstitute()` *(deep)*

### Définition
Un **aggregate** est un cluster d'objets traité comme une unité, avec une **racine** (l'aggregate root)
qui garde les invariants. Contrairement au VO, il a une **identité** qui persiste à travers ses
changements d'état.

### Pourquoi c'est important
La distinction VO/aggregate décide de l'**égalité** :
- VO → égalité par **valeur** (tous les champs) → `record`.
- Aggregate → égalité par **identité** (l'id seul) → **classe** avec `equals`/`hashCode` sur l'id.

Mettre un aggregate en `record` est un bug DDD courant : deux états du même Player (avant/après login)
ne seraient plus égaux, ce qui casse les collections et la persistance.

### Dans Atlas
`User` est une **classe** immutable. Chaque comportement métier retourne une **nouvelle instance**
(pattern « business method + constructeur de copie privé ») ; l'égalité est sur l'`UserId` :

```java
public final class User {
    private final UserId id;
    private final Instant lastLoginAt;   // null tant qu'aucun login
    // … autres champs finals

    public User recordLogin(Instant now) {                 // ne mute pas : retourne une copie
        return new User(id, email, displayName, locale, timezone, createdAt, now);
    }

    @Override public boolean equals(Object o) {            // égalité par IDENTITÉ
        return o instanceof User other && id.equals(other.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
```

**`reconstitute()`** : recharger un aggregate depuis la base ne doit PAS passer par la factory de
création (`register()` génère un id, fixe `createdAt`…). On ajoute une factory de **réhydratation**,
distincte par l'intention, qui passe par le même constructeur privé (donc mêmes invariants) :

```java
// register() = « un NOUVEAU Player naît »      reconstitute() = « ce Player EXISTAIT, on le recharge »
public static User reconstitute(UserId id, Email email, …, Instant lastLoginAt) {
    return new User(id, email, displayName, locale, timezone, createdAt, lastLoginAt);
}
```

### Exemple minimal hors Atlas
Un `BankAccount` : `open()` crée un compte avec un solde 0 et un nouvel IBAN ; `reconstitute(iban,
balance)` recharge un compte existant avec son solde stocké. Les deux passent par le même constructeur
qui vérifie « solde ≥ découvert autorisé ».

### Pièges classiques
- **Aggregate en `record`** → égalité par valeur, faux au sens DDD.
- **Setters** sur l'aggregate → on perd le contrôle des invariants. On retourne des copies.
- **`reconstitute()` public** : idéalement package-private, mais impossible ici (le mapper vit dans
  `infrastructure`, l'aggregate dans `domain` — deux packages). On l'assume `public` + garde JavaDoc
  « FOR PERSISTENCE ONLY » (ADR-015).

### Pour aller plus loin
- Eric Evans, *DDD*, « Aggregates » et « Factories ».
- ADR-015 (réhydratation), ADR-014 (UUID v7 pour les identités).

---

## Concept 3 : Erreur technique vs violation métier *(deep)*

### Définition
Deux familles d'échecs, à ne **jamais** confondre :
- **Violation de règle métier** : un humain fournit une entrée invalide (email mal formé, lien expiré).
- **Erreur technique** : un bug de l'appelant ou de l'infra (argument `null` interdit, UUID malformé
  parsé depuis une source interne, état incohérent).

### Pourquoi c'est important
Ça décide du comportement du **gestionnaire d'erreur global** :

> Erreur technique = bug du caller = **500 + alerte**. Violation métier = input invalide d'un humain
> = **400 + message clair**. Ne jamais confondre.

Les mélanger, c'est alerter l'astreinte pour un email mal tapé, ou renvoyer un 400 « gentil » sur un
vrai bug.

### Dans Atlas
Hiérarchie `DomainException` (kernel partagé) pour le métier ; `IllegalArgumentException`/`IllegalState`
pour le technique. Le `@RestControllerAdvice` traduit la première en 400 :

```java
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> handle(DomainException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));   // 400
    }
}
```

Application concrète : `Email.of()` (saisie humaine) lève `InvalidEmailException` (→ 400) ;
`UserId.from()` (id technique) lève `IllegalArgumentException` (→ 500). **Cas de frontière** : le jeton
arrive d'une URL (entrée non fiable) ; un jeton malformé fait lever `IllegalArgumentException` par
`MagicLinkToken.from()` — on la **traduit** en `InvalidMagicLinkException` (400) dans le use case, parce
qu'à cette frontière une entrée invalide n'est pas un bug mais un mauvais lien.

### Exemple minimal hors Atlas
`Integer.parseInt(userInput)` lève `NumberFormatException` (technique). À la frontière d'une API, on
catch et on renvoie 400 « ce champ doit être un nombre » — on ne laisse pas fuiter un 500.

### Pièges classiques
- Tout passer en `RuntimeException` générique → le handler ne peut plus distinguer 400 de 500.
- Laisser fuiter `DataIntegrityViolationException` (technique) au lieu de vérifier l'invariant métier
  en amont (ex. `existsByEmail` avant l'insert).

### Pour aller plus loin
- JavaDoc de `DomainException` dans le code (elle pose la règle).
- « Fail fast » vs « exceptions métier nommées ».

---

## Concept 4 : MapStruct vs aggregates riches — la bonne frontière *(deep)*

### Définition
MapStruct génère du code de mapping entre objets par convention de nommage. Il est conçu pour des
**beans anémiques** : `getX()/setX()`, constructeur public, zéro invariant.

### Pourquoi c'est important
Un aggregate DDD est l'**anti-bean** : constructeur privé, accesseurs « record-style » (`id()` pas
`getId()`), value objects à (dé)wrapper, invariants au constructeur. Forcer MapStruct dessus mène à
deux impasses : soit on **affaiblit l'aggregate** (setters publics = catastrophe DDD), soit on
**contorsionne l'outil** (AccessorNamingStrategy custom + reconstitution écrite à la main de toute
façon). Savoir choisir la frontière distingue « faire du DDD » de « faire du DDD profond ».

### Dans Atlas (doctrine ADR-015)
- Frontière **aggregate ↔ persistence (JPA)** → **mapping manuel** (l'aggregate est riche).
- Frontière **application ↔ DTO web** → **MapStruct** (les DTO sont anémiques *par design*) — au S6.

Le mapper manuel reste lisible et explicite (conversions de VO incluses) :

```java
@Component
public class UserPersistenceMapper {
    public UserJpaEntity toEntity(User user) {
        var e = new UserJpaEntity();
        e.setId(user.id().value());
        e.setEmail(user.email().value());
        e.setLocale(user.locale().toLanguageTag());      // VO/JDK type → String
        // …
        return e;
    }
    public User toDomain(UserJpaEntity e) {
        return User.reconstitute(new UserId(e.getId()), Email.of(e.getEmail()), …);
    }
}
```

### Exemple minimal hors Atlas
Mapper un `OrderEntity` (JPA, getters/setters) vers un `OrderResponseDto` (record plat) : MapStruct
brille. Mapper le même `OrderEntity` vers un aggregate `Order` riche avec invariants : mapper manuel.

### Pièges classiques
- Mettre des setters sur l'aggregate « pour que MapStruct marche » → tu casses l'encapsulation.
- Mettre le mapper dans `domain.model` pour gagner le package-private → tu tires JPA dans le domaine
  (viole ADR-003).

### Pour aller plus loin
- ADR-015 (doctrine de mapping par frontière).
- Doc MapStruct sur les `AccessorNamingStrategy` (pour comprendre *pourquoi* ça coince).

---

## Concept 5 : Modularisation de l'autoconfigure Boot 4 *(light)*

**Définition / Dans Atlas.** Spring Boot 4 a éclaté son énorme `spring-boot-autoconfigure` en modules
par techno. Conséquence vécue **deux fois** : au Sprint 0, `flyway-core` seul ne déclenchait pas les
migrations (il fallait `spring-boot-starter-flyway`) ; au Sprint 1, `@AutoConfigureMockMvc` n'était plus
dans `spring-boot-test-autoconfigure` mais dans un module dédié **`spring-boot-webmvc-test`** (package
`org.springframework.boot.webmvc.test.autoconfigure`).

**Pourquoi / Piège.** Le symptôme est déroutant : *aucune erreur*, juste un comportement absent (Flyway)
ou une classe introuvable (MockMvc). Réflexe à acquérir : en Boot 4, **une fonctionnalité d'autoconfig
ou de test = un module/starter dédié à tirer**. Quand un truc « devrait marcher » et ne marche pas
silencieusement, vérifier qu'on a bien le module qui apporte l'auto-configuration.

**Pour aller plus loin.** Migration guide Spring Boot 3→4 (modularisation).

---

## Concept 6 : Spring Modulith — verify vert vs détection active *(light)*

**Définition / Dans Atlas.** `ApplicationModules.of(App.class).verify()` échoue le build si un module
référence l'interne d'un autre. Mais un test vert sur une app *sans* violation ne prouve pas que l'outil
**détecterait** un manquement. On ajoute donc un `ModuleViolationDetectionTest` : une fixture à deux
modules où B référence l'interne (non exposé) de A, et on vérifie que `verify()` **lève** `Violations`.

**Subtilités vécues.** (a) Le kernel `shared` est déclaré **OPEN** (`@ApplicationModule(type=OPEN)`) car
c'est un kernel transverse, pas un bounded context (ADR-017). (b) La fixture de violation vit en sources
*main* (Modulith exclut les classes de test de son scan) et hors `dev.ryanfoerster.atlas` (sinon le test
d'isolation réel la scannerait et échouerait).

**Piège.** Croire qu'un `verify()` vert suffit : il faut *aussi* prouver la détection, sinon on ne sait
pas si le garde-fou est armé.

**Pour aller plus loin.** ADR-001 (modular monolith), ADR-017 (module OPEN).

---

## Concept 7 : Hybride signals + Observables en Angular 22 *(light)*

**Définition / Dans Atlas.** Angular moderne sépare deux réactivités : **signals** pour l'**état**
synchrone (re-render déclaratif), **Observables** pour l'**I/O** asynchrone (HTTP). On ne choisit pas
l'un *contre* l'autre, on les combine. `AuthService` expose `currentUser` en signal, et ses méthodes
renvoient des Observables HTTP qui *mettent à jour* le signal :

```ts
private readonly _currentUser = signal<CurrentUser | null>(null);
readonly currentUser = this._currentUser.asReadonly();
readonly isAuthenticated = computed(() => this._currentUser() !== null);

loadCurrentUser(): Observable<CurrentUser> {
  return this.http.get<CurrentUser>('/api/auth/me').pipe(tap(u => this._currentUser.set(u)));
}
```

`effect()` applique un signal à un side-effect (ex. `ThemeService` pose `data-theme` sur `<html>`).
`APP_INITIALIZER` fait un `GET /me` au boot pour réhydrater + amorcer le CSRF.

**Piège.** Tout vouloir en signals (l'HTTP reste Observable) ou tout en RxJS (l'état d'UI est plus
simple en signals). En test zoneless, penser à `TestBed.tick()` pour flusher les `effect()`.

**Pour aller plus loin.** Doc Angular « Signals » et « Resource/httpResource » (évolutions à venir).

---

## Concept 8 : États asynchrones par composition *(light)*

**Définition / Dans Atlas.** Pas de composant page-level « Loading/Empty/Error ». On compose les
primitives existantes du design system : Button `loading` (submitting), Input « État erreur » (error
champ), une ligne `danger` + `alert-circle` (error form), un écran Focus (sent). Documenté comme
doctrine dans `docs/design-system/patterns/async-states.md`.

**Exemple — login.** idle (formulaire) → submitting (bouton spinner) → error champ (« Saisis une adresse
valide ») ou error form (« Impossible d'envoyer… ») → sent (« Vérifie ta boîte mail »).

**Piège.** Créer un composant pour chaque état → prolifération. On compose d'abord ; on ne crée un
composant que si la composition ne suffit plus (et alors on le **spécifie** dans le design system avant
de coder — CLAUDE.md §6).

**Pour aller plus loin.** `docs/design-system/patterns/async-states.md`.

---

## Concept 9 : Fidélité dev/prod via proxy same-origin *(light)*

**Définition / Dans Atlas.** En prod, frontend et backend seront **same-origin**. Développer en
cross-origin (`:4200` ↔ `:8080`) = développer contre un environnement qui n'existera jamais. Le **proxy**
du dev-server Angular (`/api` → `:8080`) rend la topologie réseau identique à la prod. Conséquences :
cookies first-party, **CSRF natif d'Angular** (plus d'interceptor custom), URL relatives (`/api/auth`).

**Friction qui a motivé ça.** Découverte à la vérification : au premier chargement, l'app n'a jamais
appelé le backend → pas de cookie `XSRF-TOKEN` → premier POST de login en **403**. Corrigé par un
`APP_INITIALIZER` (`GET /me` amorce le cookie + réhydrate ; un 401 est un état *normal*, avalé
silencieusement).

**Piège.** Confondre « ça marche en curl » et « ça marche dans le navigateur » : le cross-origin +
cookies + SameSite se comporte différemment. La fidélité topologique évite toute cette classe de bugs.

**Pour aller plus loin.** ADR-018, ADR-012 (session/cookies), ADR-011 (magic link).

---

## Auto-évaluation

1. Pourquoi valider dans le **constructeur canonique** d'un record et pas seulement dans la factory ?
2. `User` est une classe et pas un `record` : quelle règle DDD l'impose, et qu'est-ce qui casserait sinon ?
3. Quelle est la différence d'intention entre `register()` et `reconstitute()` ? Pourquoi `reconstitute()`
   ne peut-il pas être package-private ici ?
4. Un utilisateur saisit un email mal formé : quel type d'exception, quel code HTTP, et pourquoi pas l'autre ?
5. Pourquoi un jeton de lien malformé (URL) est-il traduit de `IllegalArgumentException` en
   `InvalidMagicLinkException` ?
6. Donne une frontière où MapStruct est le bon choix, et une où il ne l'est pas. Pourquoi ?
7. En Boot 4, un comportement d'auto-config « manque » sans erreur : quel est ton premier réflexe ?
8. Pourquoi un test `verify()` vert ne suffit-il pas à prouver l'isolation Modulith ?
9. Dans `AuthService`, qu'est-ce qui est un signal, qu'est-ce qui est un Observable, et pourquoi ce partage ?
10. Pourquoi le proxy same-origin en dev est-il une décision de *fidélité* avant d'être une décision de
    *robustesse CSRF* ?
