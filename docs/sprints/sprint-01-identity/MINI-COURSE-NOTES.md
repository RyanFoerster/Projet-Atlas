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

## 4. Pattern « retour de nouvelle instance » pour les aggregates immutables (à remplir en S2)
- À explorer en S2 : with-method pattern vs builder vs copy-constructor.
- Noter le choix retenu + pourquoi (lisibilité), car tous les autres aggregates le copieront.
