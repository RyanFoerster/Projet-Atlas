# Pattern — États asynchrones (par composition)

> Doctrine : Atlas n'a **pas** de composant page-level « Loading/Empty/Error ». Les états async se
> font en **composant les primitives existantes** (Button `loading`, Input « État erreur », layout
> Focus, voix éditoriale). On ajoute un composant seulement si la composition ne suffit plus.

| État | Comment | Primitive(s) |
|---|---|---|
| **idle** | Le formulaire, prêt. | Input (§4.2) + Button (§4.1) |
| **submitting** | Bouton en cours, désactivé, largeur figée. | Button état `loading` (`aria-busy`, spinner) |
| **error (champ)** | Bordure + message `danger` sous le champ. | Input « État erreur » (§4.2) |
| **error (formulaire)** | Ligne `danger` + icône `alert-circle` au-dessus des actions. | Réutilise le langage visuel de l'Input error |
| **sent / succès** | Écran de confirmation en layout Focus (titre + texte), pas de formulaire. | Layout Focus (§5.2) + voix (§6) |

**Exemple — login** : saisie email *(idle)* → clic « Recevoir le lien », bouton en spinner *(submitting)*
→ si email invalide, message `danger` sous le champ *(error champ)* ; si l'appel échoue, ligne `danger`
+ `alert-circle` *(error form)* → sinon écran « Vérifie ta boîte mail » *(sent)*.

Règle : couleur jamais seule porteuse de sens (toujours + icône + texte). Respecter `prefers-reduced-motion`.
