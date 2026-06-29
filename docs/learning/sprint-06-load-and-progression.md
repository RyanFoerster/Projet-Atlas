# Sprint 6 — Charge (%1RM) & progression structurelle

> Mini-cours (format C). Les sprints 4–5 avaient construit la forme **court terme** (Banister, par muscle,
> individualisée). Le sprint 6 ferme le moteur de simulation : la **charge** (%1RM) entre enfin dans le
> stimulus, et les **CurrentStats** (le 1RM réel) se mettent à **progresser** dans le temps — lentement,
> presque irréversiblement. C'est le sprint qui fait coexister **trois échelles de temps** dans un même
> athlète sans les re-mélanger. Cinq concepts denses, chacun isolé pour bien le comprendre hors d'Atlas.

## Ce qu'on a appris

Faire cohabiter trois horloges physiologiques (fatigue, forme, force) sans les confondre ; ajouter une 3ᵉ
dimension de dose **orthogonale** aux deux autres ; faire émerger les rendements décroissants et le plateau
**sans les coder** ; trancher la propriété d'une donnée mutable partagée entre deux modules **sans créer de
cycle** ; et distinguer deux cycles de vie de lecture (figé-dénormalisé vs mutable-lu-frais). Plus trois
leçons vécues : une migration qui « réussit » sans rien faire, la frontière calcul ≠ affichage, et une
boucle d'auto-régulation qui se stabilise toute seule.

---

## Concept 1 — Trois échelles de temps dans un même système

### Définition
Modéliser dans un seul athlète **trois** dynamiques temporelles distinctes, chacune avec sa constante de
temps : la **fatigue** (jours, τ ≈ 7), la **fitness/forme** (semaines, τ ≈ 42), et le **1RM structurel**
(mois, **quasi-irréversible**). La règle d'or : **ne jamais re-mélanger** ces horloges.

### Pourquoi c'est important
C'est la distinction qui fait la crédibilité d'un simulateur d'entraînement. Un deload (semaine légère)
**baisse la forme** — l'athlète est moins affûté — mais ne touche **pas** sa force structurelle : on ne
« désapprend » pas un squat à 140 kg en une semaine de repos. Un débutant qui s'arrête un mois perd de la
fraîcheur, garde son 1RM. Si les trois échelles partageaient une seule variable, le deload effacerait la
progression, ou la progression empêcherait la fatigue — les deux absurdes. Séparer les horloges est ce qui
permet d'avoir à la fois un cycle hebdomadaire vivant **et** une trajectoire long terme stable.

### Comment c'est utilisé dans Atlas
- **Fatigue** et **fitness** vivent dans `FitnessFatigueState` (module athletics), décroissent à chaque
  lecture (lazy compute, τ_fatigue ≈ 7 j modulé génétiquement, τ_fitness ≈ 42 j).
- **Le 1RM** vit dans `CurrentStats` (module roster) et progresse via un **accumulateur de charge chronique**
  séparé (`PatternProgress.chronicLoad`, τ_chronic = **90 jours**), matérialisé en 1RM par le
  `StructuralProgressionModel`.
- Les deux états sont **portés par des aggregates différents, dans des modules différents** (athletics pour
  la forme, roster pour la force) — la séparation physique **incarne** la séparation conceptuelle.

La **preuve end-to-end** (test du gate conceptuel) : squat départ 140 kg, plafond 195,5 kg ; 12 semaines à
charge fixe → 1RM monte à **144,72 kg** ; puis 6 semaines de repos → la **fitness s'effondre** (1,264 →
0,465, soit e⁻¹ sur une constante de temps) **mais le 1RM reste à 144,72 kg, exactement**. Les trois
horloges tournent à des vitesses différentes dans le même athlète, et ne se contaminent pas.

### Exemple minimal hors Atlas
Un compte bancaire avec trois soldes : le **solde du jour** (varie à chaque transaction), la **moyenne
glissante sur 30 jours** (lisse les pics), et le **capital investi** (ne bouge qu'aux versements). Retirer
20 € fait plonger le solde du jour, bouge à peine la moyenne, ne touche pas le capital. Trois vues du même
argent, trois constantes de temps. Les afficher comme une seule ligne serait une erreur de modélisation.

### Pièges classiques
- **Re-mélanger les horloges.** La tentation de faire « progresser le 1RM = un peu de fitness qui se
  cristallise » lie les deux échelles — et alors un deload (qui baisse la fitness) finit par grignoter le
  1RM. Atlas garde **deux accumulateurs strictement séparés**, qui ne partagent qu'un timestamp de lecture.
- **Une seule constante de temps pour tout.** Si fatigue et fitness décroissaient à la même vitesse, il n'y
  aurait **pas de supercompensation** (le pic de forme après récupération vient justement de τ_fatigue <
  τ_fitness). Trois échelles ⇒ trois τ distincts, non-négociable.

### Pour aller plus loin
Le modèle de Banister original (impulse-response) ne porte que deux échelles (fitness/fatigue) ; la
troisième (structurelle) est une extension Atlas. Lire sur la distinction *adaptation neuromusculaire aiguë*
vs *hypertrophie/remodelage structurel* (Zatsiorsky & Kraemer, *Science and Practice of Strength Training*).

---

## Concept 2 — Une 3ᵉ dimension de dose **orthogonale** (charge vs effort)

### Définition
Ajouter au stimulus une dimension **charge** (`loadFactor`, fonction du %1RM) **indépendante** de la
dimension **effort** (`effortFactor`, fonction du RPE) : les deux se **multiplient** mais mesurent des choses
différentes — la **tension mécanique** (proximité du 1RM) vs la **proximité de l'échec** (reps en réserve).

### Pourquoi c'est important
Charge et effort sont souvent confondus, mais ils se dissocient. Un 5×5 à 70 % du 1RM mené à RPE 9 est un
**effort élevé** (proche de l'échec) à **charge moyenne**. Un single à 95 % à RPE 7 est l'inverse : **charge
maximale**, **effort modéré** (2 reps en réserve). Les deux construisent du muscle, mais pas pareil. Modéliser
les deux axes séparément, c'est ce qui permet au moteur de distinguer un travail de volume d'un travail de
force — et c'est ce qui **résout enfin le déséquilibre composé/isolation** laissé ouvert au sprint 5 : un
squat à 140 kg porte une charge absolue qu'un curl à 20 kg n'a pas, donc reprend son ascendant.

### Comment c'est utilisé dans Atlas
Le stimulus d'une série devient `reps × effortFactor(rpe) × loadFactor(%1RM)` (ADR-034) :
- `effortFactor(rpe) = clamp((rpe − 4) / 6, 0, 1)` — seuil convexe doux (hérité du sprint 5).
- `loadFactor(%1RM) = 0,40 + 0,60 × clamp((%1RM − 0,30) / (0,90 − 0,30))` — **plancher à 0,40** (le travail
  léger haut-volume compte quand même), montée linéaire entre 30 % et 90 %, **plafond à ≥ 90 %** (tension
  maximale).
- **Orthogonalité assumée par les nullables** : `rpe` absent → effort neutre (RPE 7) ; `%1RM` absent
  (accessoire, ou composé sans 1RM connu) → `loadFactor` **au plancher**, mais l'effort, lui, reste compté.
  Un accessoire garde son crédit d'effort, pas son crédit de tension. Les deux axes ne se contaminent pas.

`NORMALIZATION` a été recalibrée **0,013 → 0,014** : ajouter `loadFactor` (souvent < 1) réduit un peu les
magnitudes, on réajuste l'échelle verticale — recalibrage légitime d'une échelle libre, pas un fudge.

### Exemple minimal hors Atlas
Le risque d'un prêt = `montant × probabilité_de_défaut`. Le **montant** (exposition) et la **probabilité**
(qualité de l'emprunteur) sont orthogonaux : un gros prêt à un bon payeur, un petit prêt à un mauvais. On ne
les additionne pas dans une seule « note de risque » — on les garde sur deux axes et on les multiplie. Les
écraser en un seul nombre perd l'information.

### Pièges classiques
- **Plancher à zéro.** Si `loadFactor` valait 0 sous 30 %, tout le travail léger (et tous les accessoires
  sans 1RM) ne stimulerait **rien** — faux : un 3×15 de curls construit du muscle. D'où le plancher à 0,40.
- **Confondre les deux axes.** Mettre le %1RM dans la formule d'effort (ou l'inverse) re-couple ce qu'on
  voulait séparer : un single lourd à RPE 7 paraîtrait « facile » et sous-stimulant, ce qui est faux. Garder
  `loadFactor` et `effortFactor` comme deux fonctions distinctes, multipliées.

### Pour aller plus loin
Helms et al. sur RPE/RIR (proximité de l'échec) vs les recommandations de %1RM par objectif (force ≈ 85 %+,
hypertrophie ≈ 65–85 %). Les deux grilles coexistent dans tout programme sérieux — preuve qu'elles mesurent
des choses différentes.

---

## Concept 3 — Cible convergente + cliquet : rendements décroissants **émergents**

### Définition
Faire progresser le 1RM vers un **plafond génétique** par une fonction qui **converge** (s'approche du
plafond sans l'atteindre), verrouillée par un **cliquet** (le 1RM ne peut que monter, jamais redescendre).
La formule : `mérité(C) = plafond − (plafond − départ) · exp(−C / SCALE)`, où `C` est la charge chronique
accumulée et `SCALE = 20`. Le cliquet : `1RM = max(1RM, mérité)`.

### Pourquoi c'est important
C'est le concept le plus subtil du sprint : **les rendements décroissants et le plateau ne sont pas codés —
ils émergent de la forme convergente**. Aucune ligne ne dit « si l'athlète est avancé, ralentir ». La
décroissance vient mécaniquement de l'**écart au plafond** : loin du plafond (débutant), `exp` est raide →
gros gains ; près du plafond (avancé), `exp` s'aplatit → gains minuscules. Le célèbre « newbie gains » et la
stagnation de l'athlète confirmé tombent du **même** modèle, sans cas particulier. Et le **plateau à volume
constant est une feature, pas un bug** : à charge fixe, la charge chronique se stabilise (l'accumulation
compense exactement la décroissance τ=90 j), donc `mérité` plafonne — physiologiquement correct, il **faut**
augmenter la charge pour reprogresser (surcharge progressive).

### Comment c'est utilisé dans Atlas
`StructuralProgressionModel` (domaine pur, athletics) :
- **Plafond** = `bodyweight × ratio_ÉLITE(pattern, gender) × strengthAffinity(pattern)` — le potentiel
  génétique individualisé (les `StrengthStandards` donnent les ratios de force par lift/sexe ; l'affinité
  génétique module). C'est là que l'axe génétique **structurel** réservé au sprint 5 trouve enfin sa place.
- **`mérité(C)`** converge du départ vers ce plafond ; **`chronicLoad`** accumule le stimulus et décroît à
  τ = 90 jours (l'arrêt de l'entraînement fige la progression, ne la fait pas régresser).
- **Cliquet à la frontière d'émission** : on **n'émet un `CurrentStatsProgressed` que si `mérité > courant`**.
  Le 1RM matérialisé est monotone par construction.

**Calibration validée** (sim Python + test de calibration Java) : débutant **+19 kg / 12 sem** (newbie
gains), athlète avancé **+7 kg**, écart inter-génétique **×2,3**, tous bornés **sous le plafond**, trajectoire
stable (pas de divergence). Le plateau apparaît à volume fixe, exactement comme attendu.

### Exemple minimal hors Atlas
La charge d'une batterie : `V(t) = V_max · (1 − e^{−t/τ})`. Les premières minutes chargent vite, les
dernières traînent — **les rendements décroissants émergent de la forme exponentielle**, personne ne code
« ralentir près de 100 % ». Ajoute un cliquet (la batterie ne se décharge pas quand tu débranches) et tu as
le modèle Atlas.

### Pièges classiques
- **Coder explicitement la décroissance** (un `if trainingAge > 2 ans then ×0.5`). Inutile et fragile : la
  forme convergente la produit gratuitement, et de façon continue plutôt que par paliers arbitraires.
- **Oublier le cliquet à la bonne frontière.** Si on appliquait `max` côté roster après coup, une émission
  « négative » transitoire pourrait fuiter. Atlas n'**émet** que les hausses — le cliquet est dans la
  décision d'émettre, pas seulement dans le consommateur.
- **Lire le plateau comme un bug.** « Mon athlète ne progresse plus à 120 kg » est **correct** : à charge
  fixe, on plafonne. La réponse est la surcharge progressive, pas un patch du modèle.

### Pour aller plus loin
Les courbes de progression de force (ex. les travaux de Lyle McDonald sur les « years of training » et les
gains attendus par niveau) montrent empiriquement cette forme convergente. Mathématiquement : tout système
du premier ordre vers un point fixe (`dx/dt = (cible − x)/τ`) donne cette exponentielle.

---

## Concept 4 — Propriété d'une donnée mutable partagée + résolution de cycle

### Définition
Décider **quel module possède** une donnée mutable que **deux modules** doivent toucher (Athletics produit le
stimulus, mais le 1RM vit dans Roster), et le faire **sans créer de dépendance circulaire** entre modules.

### Pourquoi c'est important
C'est une décision **structurante**, pas un détail. Le 1RM (`CurrentStats`) vit dans Roster (ADR-019, c'est
l'identité de l'athlète) ; mais c'est Athletics qui sait **de combien** il doit progresser (il porte le
modèle). Deux mauvaises réponses possibles : (a) déplacer les CurrentStats dans Athletics (casse l'ownership
établi), (b) un **port de commande synchrone** Athletics → Roster (« fais progresser ce 1RM ») — ce que Ryan
a nommé un **piège** : ça viole la règle CLAUDE.md §3.6 (les side-effects inter-modules passent par **events**,
pas par commandes synchrones), et ça crée un couplage temporel. La bonne réponse préserve les deux modules
**et** la règle.

### Comment c'est utilisé dans Atlas
**Ownership par event** (ADR-032) : Athletics **émet** `CurrentStatsProgressed(athleteId, pattern, newOneRm,
at)`, Roster le **consomme** (`CurrentStatsProgressedHandler` → `Roster.progressAthleteStat` → save,
copy-on-write). Athletics ne mute jamais le 1RM directement ; il **annonce** un fait.

Mais cela crée un **cycle** : Athletics lisait déjà `roster.api` (pour le 1RM frais, concept 5), et voilà
Roster qui écoute un event d'Athletics → `athletics → roster` **et** `roster → athletics`. Spring Modulith le
détecte et fait **échouer le build**. **Résolution** : déplacer **le contrat de l'event** (`CurrentStatsProgressed`)
dans le **kernel partagé** `shared/events`. Les deux modules dépendent alors de `shared` (autorisé), plus
l'un de l'autre. **Seul le contrat descend** dans shared — la **logique métier** (le `StructuralProgressionModel`,
le calcul du mérité, le cliquet) **reste dans athletics**. C'est la distinction clé : on partage un *type de
message*, pas un *comportement*.

### Exemple minimal hors Atlas
Deux microservices, `Commandes` et `Facturation`. La facture appartient à `Facturation`, mais c'est
`Commandes` qui sait quand une commande est payée. Mauvais : `Commandes` appelle `Facturation.créerFacture()`
(couplage synchrone, cycle si Facturation appelle déjà Commandes). Bon : `Commandes` émet `CommandePayée` sur
un bus, `Facturation` s'abonne. Le **schéma de l'event** vit dans une lib partagée ; la **logique de
facturation** reste dans `Facturation`.

### Pièges classiques
- **Descendre la logique dans le shared** avec le contrat. Le kernel partagé doit rester **minimal** (CLAUDE.md
  §3) : seulement des types, jamais des services métier. Si on y mettait le modèle de progression, tout le
  monde en dépendrait et l'isolation s'effondrerait.
- **Choisir le port synchrone « parce que c'est plus simple ».** Il l'est à court terme, mais il crée le
  cycle, viole la règle event-driven, et couple les transactions. L'event asynchrone (AFTER_COMMIT,
  REQUIRES_NEW) découple proprement.

### Pour aller plus loin
Spring Modulith : `ApplicationModules.verify()` et la détection de cycles ; la notion de **Shared Kernel**
en DDD (Evans) et pourquoi il doit rester aussi petit que possible (chaque ajout est un couplage partagé par
tous).

---

## Concept 5 — Deux cycles de vie de lecture (figé-dénormalisé vs mutable-lu-frais)

### Définition
Distinguer, quand un module lit une donnée d'un autre, **deux régimes** : une donnée **immuable** peut être
**lue une fois et dénormalisée** (copiée) ; une donnée **mutable** doit être **relue fraîche** à chaque usage,
jamais mise en cache.

### Pourquoi c'est important
C'est la règle qui dit **quand on a le droit de copier** une donnée d'un autre module. Copier de l'immuable
est sûr (il ne changera jamais, la copie ne périme pas). Copier du mutable est un **bug en puissance** : la
copie diverge de la source. Confondre les deux, c'est soit dupliquer du mutable (cache empoisonné), soit
relire de l'immuable à chaque fois (gaspillage). Le sprint 6 a **les deux régimes côte à côte** sur le même
voisin (Roster), ce qui rend la distinction très concrète.

### Comment c'est utilisé dans Atlas
Athletics lit deux choses chez Roster, avec **deux régimes opposés** :
- **Plafond génétique** (`findStrengthCeiling`) — dérivé de la `Genetics`, qui est **immuable**. Lu **une
  seule fois** à l'initialisation de la progression d'un pattern, puis **dénormalisé** dans `PatternProgress.
  ceilingOneRmKg`. Il ne changera jamais → le figer est correct (et c'est la même justification que la
  dénormalisation des `GeneticModifiers` au sprint 5).
- **1RM courant** (`findLoadProfile`) — **mutable** (il progresse, justement). Relu **frais à chaque séance**
  pour calculer le %1RM. Le mettre en cache serait fatal : c'est précisément la donnée que la boucle fait
  bouger.

Et c'est là que naît la **boucle d'auto-régulation**, qui se referme **pour de vrai** ce sprint : le 1RM
monte (concept 3) → la même charge absolue devient un **%1RM plus bas** → `loadFactor` baisse → le stimulus
baisse → la progression ralentit. **Negative feedback** émergent — l'athlète qui force toujours pareil
progresse de moins en moins, sans qu'on code ce ralentissement. La boucle est **stable** (vérifié en
calibration : convergence, pas d'emballement) **précisément parce que** le 1RM courant est relu frais ; un
1RM mis en cache aurait cassé le feedback.

### Exemple minimal hors Atlas
Une commande lit, d'un service Client : son **pays de création** (immuable → on le copie sur la commande,
pour l'historique) et son **adresse de livraison actuelle** (mutable → on la relit au moment d'expédier,
jamais on ne la fige à la création, sinon on livre à l'ancienne adresse). Même source, deux régimes selon la
mutabilité.

### Pièges classiques
- **Cacher du mutable.** Dénormaliser le 1RM courant « pour la perf » casserait l'auto-régulation : la boucle
  lirait une valeur périmée et n'amortirait jamais. Le mutable se relit, point.
- **Relire de l'immuable en boucle.** À l'inverse, re-dériver le plafond génétique à chaque séance serait du
  gaspillage (et exposerait à des incohérences si la dérivation changeait). L'immuable se fige une fois.
- **Ne pas se demander « est-ce que ça change ? ».** La question n'est pas « est-ce coûteux à relire ? » mais
  « est-ce que ça **peut changer** entre deux lectures ? ». La mutabilité, pas la perf, décide du régime.

### Pour aller plus loin
CQRS et la fraîcheur des données de lecture ; la notion de *staleness* acceptable ; en DDD, la différence
entre copier une **value** (immuable, sûr) et référencer une **entity** par identité (mutable, à relire).

---

## Trois leçons vécues (transversales)

- **Une migration peut « réussir » sans rien faire.** `V013` naviguait la colonne JSONB `exercises` comme un
  tableau direct, alors qu'elle contient un objet `{"exercises": [...]}`. La clause `WHERE` a matché **zéro
  ligne** : Flyway a reporté `success=true` sans rien backfiller. `V014` a fait le vrai travail **et** ajouté
  un **garde-fou de complétude** (`RAISE EXCEPTION` si une seule série reste sans `loadType`) — sans cet
  assert, un backfill silencieusement incomplet passe inaperçu. Leçon : **une migration de données doit
  vérifier qu'elle a bien agi**, pas seulement s'exécuter sans erreur. (Et : on ne modifie jamais une
  migration déjà appliquée — on en ajoute une.)
- **Calcul ≠ affichage.** Le squat affichait `103,0122747238` à l'écran. La correction n'est **pas** d'arrondir
  en base — au contraire : on **garde la pleine précision en stockage** (sinon `+0,4 kg` sur un 1RM stocké à
  `103` ne change rien et le 1RM ne monte **jamais** visiblement, le pire des bugs) et on **arronditseulement
  à l'affichage** (jalon entier « 103 kg » + delta cumulé à 1 décimale « +3,0 kg » pour rendre la progression
  lente perceptible). La frontière de présentation normalise ; le domaine garde la vérité fine.
- **Une boucle d'auto-régulation se stabilise toute seule — si on la branche bien.** Le 1RM↑ → %1RM↓ →
  loadFactor↓ → stimulus↓ → progression↓ est un *negative feedback* qu'on n'a pas eu à « calmer »
  explicitement : il converge parce que les trois échelles de temps et la cible convergente sont correctes.
  Le rôle du dev n'était pas d'amortir la boucle, mais de **ne pas la casser** (lire le 1RM frais, concept 5).

---

## Auto-évaluation

1. Cite les trois échelles de temps du sprint 6, leur constante de temps approximative, et ce qu'un **deload**
   fait (ou ne fait pas) à chacune.
2. Pourquoi un 1RM mis en cache côté Athletics casserait-il la boucle d'auto-régulation ?
3. `loadFactor` et `effortFactor` mesurent quoi, exactement, chacun ? Donne un cas où l'un est haut et l'autre
   bas.
4. Pourquoi le `loadFactor` a-t-il un **plancher** à 0,40 plutôt que de tomber à 0 sous 30 % du 1RM ?
5. Où, précisément, les **rendements décroissants** sont-ils codés dans `StructuralProgressionModel` ?
   (Question piège.)
6. Pourquoi le **plateau à volume constant** est-il une feature et non un bug, et quelle est la « réponse »
   physiologique attendue ?
7. À quelle **frontière** le cliquet (1RM monotone) est-il appliqué, et pourquoi pas côté consommateur Roster ?
8. Quel **cycle** l'ownership-par-event a-t-il créé entre athletics et roster, et comment l'a-t-on résolu **sans
   déplacer la logique métier** ?
9. Athletics lit deux choses chez Roster : le **plafond génétique** et le **1RM courant**. Lequel est
   dénormalisé, lequel est relu frais, et quelle propriété décide du régime ?
10. Pourquoi garde-t-on la **pleine précision** du 1RM en stockage alors qu'on n'affiche qu'un entier ? Que se
    passerait-il si on arrondissait en base ?
11. Qu'est-ce qui a rendu `V013` « réussie mais inopérante », et quel garde-fou `V014` a-t-il ajouté pour que
    ce type d'erreur échoue **bruyamment** la prochaine fois ?
</content>
</invoke>
