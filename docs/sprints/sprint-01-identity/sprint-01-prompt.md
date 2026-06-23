# Sprint 1 — Identity & onboarding

> Prompt complet à coller dans Claude Code pour exécuter le Sprint 1. Premier sprint de logique métier — première vraie modélisation DDD du projet.

---

## Contexte projet (lecture préalable obligatoire)

Avant d'exécuter ce sprint, relis dans cet ordre :

1. `CLAUDE.md` à la racine — contexte général, conventions, mode de collaboration prof-élève
2. `docs/vision.md` — pourquoi on construit Atlas
3. `docs/domain/glossary.md` — vocabulaire métier officiel (notamment Player, User)
4. Tous les ADRs dans `docs/adr/` (ADR-001 à ADR-010)
5. `docs/learning/sprint-00-bootstrap.md` — mini-cours du sprint précédent
6. `docs/sprints/sprint-00-bootstrap/RETROSPECTIVE.md` — ce qui s'est bien passé et mal passé

Si une de ces lectures est obsolète ou que tu identifies une contradiction avec ce prompt, **arrête et signale à Ryan** avant de coder.

---

## Objectif du Sprint 1

À la fin de ce sprint, un Player peut :

1. Demander un magic link en saisissant son email
2. Recevoir un email avec un lien à usage unique
3. Cliquer sur le lien pour se connecter
4. Voir une page d'accueil minimale avec son nom
5. Se déconnecter

Et techniquement, le projet aura :

- Un **module Identity bootstrapé complètement** avec aggregate User, value objects, repository, application services, controllers, persistence
- **Spring Security configuré** avec authentification par session (cookie HttpOnly)
- Un **service Email abstrait** (port `EmailSender` dans domain/) avec deux implémentations : Resend en prod, log-only en dev (et Mailhog en option)
- Un **test de violation Modulith actif** qui prouve que l'isolation est détectée (pas juste validée sur repo vide comme au sprint 0)
- Un **mini-cours sprint-01-identity-and-ddd.md** qui sera la pièce centrale de la pédagogie DDD pour la suite du projet
- Un **devblog post #2** "Mon premier aggregate DDD : design et leçons apprises"

---

## Décisions prises en amont (à respecter, ne pas re-débattre)

Ces décisions ont été tranchées avec Ryan avant ce sprint. Elles sont gravées dans le prompt. Si tu identifies une raison de les remettre en question, **arrête et signale**, ne dévie pas unilatéralement.

| Décision | Choix retenu | Justification courte |
|----------|--------------|---------------------|
| Méthode d'auth | Magic link (email-only, pas de password) | Pas de gestion password, plus moderne, audience lifter sérieux a un email valide |
| Structure aggregate | User unique avec profil intégré | Simple, suffisant en MVP, extractable plus tard si besoin |
| Gestion de session | Spring Session + cookies HttpOnly | Plus sécurisé que JWT pour web app, révocation triviale, pas de refresh token |
| Email provider | Resend en prod, Mailhog en dev | Free tier généreux, API moderne, abstraction propre derrière `EmailSender` |
| Token magic link | UUID v7 + TTL 15 min + one-time use | Sécurité standard, expiration courte mitige le risque |

---

## Portée technique

### Module Identity — structure complète

Tu dois bootstraper le module Identity avec sa structure DDD complète :

```
backend/src/main/java/dev/ryanfoerster/atlas/identity/
├── api/
│   ├── events/
│   │   ├── PlayerRegistered.java          # record event
│   │   └── PlayerLoggedIn.java
│   └── IdentityQueryPort.java             # interface publique pour autres modules
├── domain/
│   ├── model/
│   │   ├── User.java                      # aggregate root
│   │   ├── UserId.java                    # value object (UUID v7)
│   │   ├── Email.java                     # value object avec validation
│   │   ├── DisplayName.java               # value object
│   │   └── MagicLink.java                 # entity
│   ├── service/
│   │   ├── MagicLinkTokenGenerator.java   # domain service stateless
│   │   └── MagicLinkExpirationPolicy.java # domain policy
│   └── port/
│       ├── UserRepository.java            # interface (port secondaire)
│       ├── MagicLinkRepository.java
│       └── EmailSender.java               # port secondaire pour l'email
├── application/
│   ├── command/
│   │   ├── RequestMagicLinkUseCase.java
│   │   ├── ConsumeMagicLinkUseCase.java
│   │   └── LogoutUseCase.java
│   ├── query/
│   │   └── GetCurrentUserUseCase.java
│   └── eventhandler/                      # vide pour l'instant
└── infrastructure/
    ├── persistence/
    │   ├── UserJpaEntity.java             # entité JPA, séparée du domain
    │   ├── MagicLinkJpaEntity.java
    │   ├── UserJpaRepository.java         # Spring Data
    │   ├── MagicLinkJpaRepository.java
    │   ├── UserPersistenceAdapter.java    # impl du port UserRepository
    │   ├── MagicLinkPersistenceAdapter.java
    │   └── mapper/
    │       ├── UserMapper.java            # MapStruct
    │       └── MagicLinkMapper.java
    ├── email/
    │   ├── ResendEmailSender.java         # impl prod
    │   ├── LogOnlyEmailSender.java        # impl dev par défaut
    │   └── EmailTemplates.java
    ├── web/
    │   ├── AuthController.java            # endpoints REST
    │   ├── dto/
    │   │   ├── RequestMagicLinkDto.java
    │   │   └── CurrentUserDto.java
    │   └── SecurityConfig.java            # Spring Security config
    └── config/
        └── IdentityModuleConfig.java      # config Spring du module
```

### Value Objects DDD — règles strictes

C'est le premier sprint DDD et les value objects sont la base de tout. Règles non-négociables :

**`UserId`** :
- Record encapsulant un `UUID` (v7)
- Factory `UserId.generate()` qui produit un UUID v7 (utilise `java.util.UUID` + lib comme `uuid-creator` ou impl maison documentée)
- Factory `UserId.from(String)` qui parse et valide
- Throw `IllegalArgumentException` si le UUID est null ou malformé

**`Email`** :
- Record encapsulant une `String` normalisée (lowercase, trim)
- Factory `Email.of(String)` qui valide via regex stricte (pas trop permissive : refuser les emails évidemment invalides)
- Refuser null, vide, plus de 254 caractères (RFC 5321)
- Throw `InvalidEmailException` (exception métier nommée dans le domain) avec message clair

**`DisplayName`** :
- Record encapsulant une `String`
- 2 à 50 caractères, alphanumérique + espaces + accents + tirets/apostrophes
- Trim automatique, refuser les noms vides après trim

Tous les value objects sont **immutables** (records), **comparables par valeur** (égalité structurelle), et **auto-validants** (impossible d'en construire un invalide).

### Aggregate User

```
User (aggregate root)
├── id: UserId
├── email: Email
├── displayName: DisplayName
├── locale: Locale (java.util)
├── timezone: ZoneId (java.time)
├── createdAt: Instant
├── lastLoginAt: Instant (nullable, Optional<Instant>)
└── Behaviors:
    + static register(email, displayName, locale, timezone, now): User
    + recordLogin(now): User                     # retourne un nouveau User (immutable)
    + updateDisplayName(newName): User
    + updateLocale(newLocale): User
```

**Invariants** :
- Email unique au niveau du repository (vérifié à la création)
- DisplayName toujours valide (garanti par le VO)
- `createdAt` ≤ `lastLoginAt` quand `lastLoginAt` est présent
- L'aggregate retourne **toujours de nouvelles instances** après mutation (style fonctionnel, pas de setter)

### Entity MagicLink

```
MagicLink (entity, pas aggregate root — appartient au contexte de User)
├── token: MagicLinkToken (value object, UUID v7 encapsulé)
├── userEmail: Email                        # pas userId : le user peut ne pas exister encore
├── createdAt: Instant
├── expiresAt: Instant
├── consumedAt: Optional<Instant>
├── ipAddress: Optional<String>             # capturé à la création
├── userAgent: Optional<String>
└── Behaviors:
    + isExpired(now): boolean
    + isConsumed(): boolean
    + canBeConsumed(now): boolean
    + consume(now): MagicLink                # retourne nouvelle instance
```

**Important** : on stocke `userEmail` (pas `userId`) parce qu'au moment de la demande de magic link, le User peut ne pas encore exister (premier login = signup implicite). Lors de la consommation, on cherche ou on crée le User.

### Magic link flow — logique métier précise

**Demande de magic link** (`RequestMagicLinkUseCase`) :

1. Valider l'email reçu (via `Email.of()`)
2. Optionnel : check rate limit (max 5 demandes par email par heure, à implémenter via une table ou Bucket4j). Pour le MVP, on peut sauter ça et l'ajouter en post-MVP.
3. Générer un nouveau token (UUID v7)
4. Créer un MagicLink avec TTL 15 min
5. Persister
6. Envoyer l'email via `EmailSender` (port abstrait)
7. Retourner OK (jamais d'erreur "email inconnu" → on ne révèle pas si l'email existe en DB)

**Consommation** (`ConsumeMagicLinkUseCase`) :

1. Récupérer le MagicLink par token (UUID)
2. Si absent → throw `InvalidMagicLinkException`
3. Si expiré ou déjà consommé → throw `MagicLinkNotUsableException`
4. Marquer comme consommé
5. Chercher le User par email
6. Si absent → créer le User (signup implicite : on demande le displayName à ce moment-là via un formulaire frontend après consommation réussie, OU on génère un displayName par défaut depuis l'email — à décider)
7. Si présent → mettre à jour `lastLoginAt`
8. Créer la session Spring Security
9. Publier l'event `PlayerLoggedIn` (et `PlayerRegistered` si nouveau user)

**Note sur l'UX du signup** : il faut décider — au choix :
- **A.** Le premier magic link demandé déclenche le signup, le user choisit son nom après avoir cliqué le lien (page intermédiaire "Bienvenue, comment veux-tu t'appeler ?")
- **B.** Le formulaire de demande de magic link demande **à la fois email ET nom** dès le départ
- **C.** Le user est créé avec un nom dérivé de l'email (`ryan@xxx.com` → `ryan`), modifiable après

**Recommandation : option A.** UX la plus propre, sépare clairement "vérification de l'email" de "création du compte". À implémenter.

### Endpoints REST

```
POST /api/auth/magic-link/request
  Body: { "email": "ryan@example.com" }
  Response: 202 Accepted (toujours, même si email inconnu)

GET /api/auth/magic-link/consume?token=<uuid>
  Response: 
    - Si new user → 200 OK { "newUser": true, "tempSessionToken": "..." }
      (le frontend redirige vers /onboarding pour saisir displayName)
    - Si existing user → 200 OK { "newUser": false } + cookie de session activé

POST /api/auth/complete-signup
  Body: { "tempSessionToken": "...", "displayName": "Ryan" }
  Response: 200 OK + cookie de session activé

GET /api/auth/me
  Response (authentifié): 200 OK { "id": "...", "email": "...", "displayName": "...", ... }
  Response (non authentifié): 401 Unauthorized

POST /api/auth/logout
  Response: 204 No Content + cookie supprimé
```

### Spring Security configuration

- Activation Spring Security 7
- Filter chain : `/api/auth/**` publique sauf `/me` et `/logout`, `/api/**` protégé
- Session policy : `IF_REQUIRED`
- Cookie : `HttpOnly`, `Secure` en prod, `SameSite=Lax`
- CSRF : activé pour les POST (le frontend Angular doit envoyer le token CSRF)
- CORS : déjà configuré au sprint 0, vérifier que ça marche avec credentials

### Frontend Angular

Pages à créer :

- **`/login`** : input email + bouton "Recevoir le lien"
- **`/login/sent`** : page "Vérifie ton email, lien envoyé à xxx"
- **`/auth/callback`** : page qui lit le token de l'URL, appelle l'API, redirige selon nouveau/existant
- **`/onboarding`** : formulaire displayName pour les nouveaux users
- **`/home`** : page d'accueil minimale "Bonjour {displayName}", bouton logout

Services Angular :
- `AuthService` (signals-based) qui maintient `currentUser$` (signal)
- HTTP interceptor pour CSRF token
- Guard `authGuard` pour les routes protégées

Design : Tailwind, minimaliste mais soigné. Pas de lib UI lourde. Tu peux t'inspirer du style de la page Hello world du sprint 0.

### Tests requis

**Tests unitaires de domaine (TDD strict)** :
- `EmailTest` : valide acceptés, invalides rejetés, normalisation (lowercase, trim), longueur max
- `DisplayNameTest` : pareil
- `UserIdTest` : génération, parsing, validation
- `UserTest` : invariants, comportements (register, recordLogin, update)
- `MagicLinkTest` : expiration, consommation, double-consumption refusée
- **Property-based** : pour Email, génère 1000 strings aléatoires et vérifie que seuls les emails RFC-valides passent

**Tests d'intégration** :
- `RequestMagicLinkUseCaseIntegrationTest` : flux complet sur Testcontainers
- `ConsumeMagicLinkUseCaseIntegrationTest` : flux nouveau user, flux existing user, flux expiré, flux déjà consommé
- `AuthControllerIntegrationTest` : endpoints REST avec MockMvc

**Test Spring Modulith (le grand test)** :
- `AtlasApplicationModulesTest.verifiesModuleIsolation()` : doit toujours passer (régression)
- **NOUVEAU : `ModuleViolationDetectionTest`** : un test qui crée volontairement une violation contrôlée (un fichier dans un autre module qui importe `identity.domain.User`) et vérifie que Spring Modulith la détecte. C'est la preuve active qu'on attendait depuis le sprint 0.

### Migrations Flyway

- **V002__create_users_table.sql** : table `users` (id UUID PK, email VARCHAR UNIQUE, display_name VARCHAR, locale VARCHAR, timezone VARCHAR, created_at TIMESTAMPTZ, last_login_at TIMESTAMPTZ nullable)
- **V003__create_magic_links_table.sql** : table `magic_links` (token UUID PK, user_email VARCHAR indexed, created_at TIMESTAMPTZ, expires_at TIMESTAMPTZ, consumed_at TIMESTAMPTZ nullable, ip_address VARCHAR nullable, user_agent VARCHAR nullable)

Indexes sur `magic_links.user_email` et `magic_links.expires_at` (pour cleanup).

### ADRs à créer dans ce sprint

- **ADR-011** : Magic link authentication (justification du choix vs password, vs OAuth)
- **ADR-012** : Spring Session + HttpOnly cookies pour la gestion de session (justification vs JWT)
- **ADR-013** : Email service strategy (port `EmailSender` + Resend prod + LogOnly/Mailhog dev)
- **ADR-014** : UUID v7 pour les identifiants d'aggregate (déjà mentionné dans ADR-008, à formaliser ici avec la lib choisie)

---

## Règles d'architecture applicables (rappel — non-négociables)

- **DDD tactique strict** : domaine pur dans `domain/`, zéro Spring/JPA/Jackson. Voir ADR-003.
- **Aggregate User immutable** : pas de setters, méthodes qui retournent de nouvelles instances.
- **Value objects auto-validants** : impossible de construire un Email invalide.
- **Ports & adapters** : `UserRepository` et `EmailSender` sont des interfaces dans `domain/port/`, leurs impls dans `infrastructure/`.
- **MapStruct pour le mapping domain ↔ JPA**.
- **Isolation Modulith** : aucun import inter-modules autre que via `api/`.
- **Tests sur Testcontainers PostgreSQL**, jamais H2.
- **Conventional Commits** pour chaque commit.

---

## Definition of Done

Le sprint 1 est considéré terminé quand TOUS ces critères sont vérifiés :

- [ ] `./mvnw clean verify` passe (0 erreur, tous tests verts)
- [ ] Test Modulith verify passe (toujours)
- [ ] Test ModuleViolationDetectionTest passe (la violation contrôlée est bien détectée)
- [ ] Coverage domain Identity ≥ 80% (mesurable via JaCoCo)
- [ ] Migrations V002 + V003 appliquées en local
- [ ] Endpoint `POST /api/auth/magic-link/request` fonctionne (mail visible dans Mailhog local ou logs)
- [ ] Endpoint `GET /api/auth/magic-link/consume?token=...` fonctionne et crée le User en DB
- [ ] Onboarding flow complet en local : email → clic lien → saisie nom → page home avec mon nom
- [ ] `GET /api/auth/me` répond correctement
- [ ] `POST /api/auth/logout` détruit la session
- [ ] Cookie HttpOnly + Secure (en prod) + SameSite=Lax vérifié
- [ ] CSRF activé et fonctionnel
- [ ] CI GitHub Actions verte sur le commit final
- [ ] ADR-011, 012, 013, 014 rédigés et commités
- [ ] `docs/learning/sprint-01-identity-and-ddd.md` rédigé (mini-cours complet, voir format CLAUDE.md section 6)
- [ ] Devblog `docs/blog/02-mon-premier-aggregate-ddd.md` rédigé
- [ ] `docs/sprints/sprint-01-identity-onboarding/RETROSPECTIVE.md` rédigé
- [ ] CLAUDE.md mis à jour si nouvelles conventions émergent
- [ ] Récap pédagogique de session (format A) à la fin du sprint
- [ ] Glossary mis à jour si nouveaux termes métier introduits

---

## Contraintes à respecter

- **Pas de raccourcis sur le DDD.** Si tu es tenté de mettre Spring dans domain/ "pour gagner du temps", arrête et signale.
- **Pas de mutation directe d'aggregate** : User.recordLogin() retourne un nouveau User, on ne mute pas en place.
- **Pas d'exceptions techniques fuitant dans le domain.** Si Hibernate throw, on catch en infrastructure et on traduit en exception métier.
- **Aucune dépendance circulaire entre modules.** Identity ne dépend de personne au-delà de shared/.
- **Décisions de design métier non triviales** (ex : option A/B/C sur le signup flow) → confirme avec Ryan avant de trancher si ce n'est pas écrit dans ce prompt.

---

## Contexte métier (le pourquoi)

Identity est le **module-passerelle** : tout Player commence ici. Mais notre vraie complexité métier est ailleurs (Athletics, Programming). Identity doit donc être :

- **Solide** : zéro bug d'auth, sécurité de base correcte
- **Sobre** : pas de feature inutile (pas de 2FA, pas d'OAuth, pas d'avatar — tout ça plus tard)
- **Propre** : c'est la vitrine DDD du projet — quand un recruteur ou un dev senior regardera Atlas, c'est probablement le premier module qu'il lira

L'objectif est de poser un **pattern d'implémentation DDD** que tous les modules suivants vont copier : value objects, aggregate immutable, ports, adapters, application services, tests par couche. Identity = le template DDD.

---

## Format de récap attendu

À la fin de ce sprint :

**Récap pédagogique de session** (format A, voir CLAUDE.md §6) avec focus particulier sur :
- Les choix de modélisation (pourquoi cet aggregate, pourquoi ces value objects)
- Les difficultés DDD rencontrées et comment elles ont été résolues
- Les concepts Spring Security 7 utilisés (configuration moderne, SecurityFilterChain)

**Mini-cours `sprint-01-identity-and-ddd.md`** (format C, voir CLAUDE.md §6) — **plus important que le sprint 0 parce que c'est le template DDD du projet**. Concepts à couvrir :

1. Value Objects en DDD : pourquoi, comment, exemples Email/DisplayName/UserId
2. Aggregate Root : invariants, immutabilité, méthodes métier
3. Ports & Adapters concrets : UserRepository, EmailSender, comment les ports vivent dans domain et les adapters dans infrastructure
4. Mapping domain ↔ JPA avec MapStruct : pourquoi cette séparation, comment MapStruct nous aide
5. Application Services / Use Cases : différence avec Domain Services, orchestration des side effects
6. Domain Events : PlayerRegistered, PlayerLoggedIn, comment ils sont publiés via Spring Modulith
7. Spring Security 7 moderne : SecurityFilterChain bean, configuration programmatique
8. Magic Link auth : pourquoi c'est plus moderne que password, threat model, mitigations
9. Test de violation Modulith : comment on prouve activement l'isolation
10. UUID v7 : pourquoi v7 plutôt que v4, impact sur les indexes Postgres

Ce mini-cours sera la référence DDD pour tout le projet. Investis dans sa qualité.

---

## Première instruction concrète

Quand tu commences l'exécution :

1. Lis toutes les sources préalables (section "Contexte projet").
2. Confirme à Ryan que tu as bien tout lu et que tu comprends l'objectif du sprint.
3. **Important** : avant de coder, propose un plan séquencé en sous-étapes (S1 à SN) avec un livrable concret par étape. Vu la densité du sprint, je veux qu'il soit découpé en au moins 8-10 étapes avec des paliers de validation.
4. Pose à Ryan les questions de clarification éventuelles (notamment sur l'option A/B/C du signup flow si ce n'est pas suffisamment clair).
5. Attends la validation du plan **avant de coder**.
6. Exécute étape par étape avec paliers de validation, comme au sprint 0.

**Pas de mode "génère tout d'un coup".** Ce sprint pose le pattern DDD du projet — on prend le temps de bien faire.

---

*Sprint 1 — Identity & onboarding. Estimé 2-3 semaines à 10-15h/semaine.*
