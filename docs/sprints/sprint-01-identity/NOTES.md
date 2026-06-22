# Notes — Sprint 1 (Identity + onboarding)

> Dossier créé au sprint 0 pour ne pas perdre des éléments reportés. Nom provisoire, à fusionner avec le `prompt.md` du sprint 1.

## Reports décidés lors de sprints précédents

### [Sprint 0] Test de détection de violation d'architecture — À FAIRE quand il y a du vrai code

Au sprint 0, `AtlasApplicationModulesTest.verifiesModuleIsolation()` passe, mais
les modules sont **vides** : le test prouve que Modulith *tourne*, pas qu'il
*détecte réellement* une violation.

Quand le module **identity** aura du vrai code (aggregate `User`, etc.), ajouter
un test qui **prouve la détection** :

1. Créer (temporairement ou dans un module de test dédié) une **violation contrôlée** :
   par exemple, depuis un autre module, importer une classe du `domain/` ou de
   l'`infrastructure/` d'identity (au lieu de passer par son `api/`).
2. Vérifier que `modules.verify()` **échoue** bien avec cette violation
   (test attendant une `Violations`/exception Modulith).
3. Retirer la violation, confirmer que le build repasse au vert.

Objectif : transformer « le test passe » en « le test échoue quand il doit échouer »
— la seule preuve qui compte pour un garde-fou.

Référence : ADR-001 (isolation des modules), `AtlasApplicationModulesTest`.
