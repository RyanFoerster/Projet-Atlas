package dev.ryanfoerster.atlas.shared.domain.exceptions;

/**
 * Classe de base abstraite de toutes les exceptions métier d'Atlas.
 *
 * <p>Pattern DDD : le domaine signale une violation de règle métier (invariant non
 * respecté, value object invalide, transition d'aggregate interdite) en levant une
 * {@code DomainException} — jamais une exception technique du framework. Cela garde le
 * domaine pur (ADR-003) et donne aux couches supérieures un type stable à intercepter
 * pour traduire en réponse HTTP, en message d'erreur, etc.
 *
 * <p>C'est une exception <strong>non checked</strong> (hérite de {@link RuntimeException}) :
 * le projet bannit les exceptions checked dans le domaine (cf. CLAUDE.md §5). Une règle
 * métier violée n'est pas une condition que l'appelant doit obligatoirement gérer via la
 * signature ; c'est un échec de validation qui remonte naturellement.
 *
 * <p><strong>Réservée aux violations de règles métier — jamais aux erreurs techniques.</strong>
 * La distinction est fondamentale et structure tout le traitement d'erreur de l'application :
 * <ul>
 *   <li><b>Violation métier</b> (input invalide d'un humain : email mal formé, nom trop
 *       court, transition d'aggregate interdite) → {@code DomainException} → réponse
 *       <b>400</b> avec un message clair pour l'utilisateur, pas d'alerte.</li>
 *   <li><b>Erreur technique</b> (bug de l'appelant : argument {@code null} non autorisé,
 *       UUID malformé parsé depuis une source interne, état incohérent du programme) →
 *       {@link IllegalArgumentException} / {@link IllegalStateException} → réponse
 *       <b>500</b> + alerte. Ce n'est pas une {@code DomainException}.</li>
 * </ul>
 * Les confondre, c'est confondre les responsabilités du gestionnaire d'exceptions global
 * (400 vs 500, message utilisateur vs alerte ops). On ne mélange jamais les deux.
 *
 * <p><strong>Convention pour les modules</strong> : chaque exception métier d'un module
 * (ex. {@code InvalidEmailException} dans identity) étend cette classe. On obtient ainsi
 * une hiérarchie commune ({@code catch (DomainException e)} attrape toutes les erreurs
 * métier) sans réinventer la base dans chaque bounded context. Cette classe vit dans le
 * kernel partagé {@code shared/} car elle est, par nature, transverse à tous les modules.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
