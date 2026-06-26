package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repository Spring Data de l'état dynamique des athlètes. */
public interface AthleteConditionJpaRepository extends JpaRepository<AthleteConditionJpaEntity, UUID> {
}
