/**
 * Kernel partagé d'Atlas — déclaré comme module Spring Modulith <strong>OPEN</strong>.
 *
 * <p>Par défaut, un module Modulith est <em>fermé</em> (CLOSED) : seuls les types de son
 * package de base sont exposés, ses sous-packages sont internes et inaccessibles aux autres
 * modules. C'est exactement ce qu'on veut pour un bounded context métier (identity, athletics…)
 * — l'encapsulation est la règle.
 *
 * <p>Mais {@code shared/} n'est pas un bounded context : c'est le <strong>kernel partagé</strong>
 * (cf. CLAUDE.md §3), conçu pour être importé librement par <em>tous</em> les modules
 * (value objects fondamentaux comme {@code Weight}/{@code RPE}, base d'exceptions
 * {@code DomainException}, base d'events…). Le déclarer {@code Type.OPEN} signifie : « ce
 * module n'encapsule pas, ses sous-packages sont volontairement accessibles partout ». Sans
 * ça, la moindre référence inter-module à {@code shared.domain.exceptions.DomainException}
 * ferait échouer la vérification d'isolation.
 *
 * <p>Contrepartie assumée : le kernel doit rester <strong>minimal</strong>. Tout ce qui peut
 * vivre dans un module spécifique doit y vivre — un kernel qui grossit devient un couplage
 * global déguisé. C'est la discipline qui accompagne le statut OPEN.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package dev.ryanfoerster.atlas.shared;
