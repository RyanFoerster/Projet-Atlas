package dev.ryanfoerster.atlas.identity.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Mapper manuel entre l'aggregate {@link User} (domaine riche) et {@link UserJpaEntity}
 * (stockage anémique). Écrit à la main par choix de doctrine (ADR-015) : MapStruct est
 * inadapté aux aggregates riches (constructeurs privés, accesseurs record-style, value objects).
 *
 * <p>Conversions de value objects faites ici, explicitement : {@code Locale ↔ BCP-47},
 * {@code ZoneId ↔ id texte}, déballage/remballage des VO. La reconstruction du domaine passe
 * par {@link User#reconstitute} (réhydratation, pas création).
 */
@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.id().value());
        entity.setEmail(user.email().value());
        entity.setDisplayName(user.displayName().value());
        entity.setLocale(user.locale().toLanguageTag());
        entity.setTimezone(user.timezone().getId());
        entity.setCreatedAt(user.createdAt());
        entity.setLastLoginAt(user.lastLoginAt().orElse(null));
        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                new UserId(entity.getId()),
                Email.of(entity.getEmail()),
                DisplayName.of(entity.getDisplayName()),
                Locale.forLanguageTag(entity.getLocale()),
                ZoneId.of(entity.getTimezone()),
                entity.getCreatedAt(),
                entity.getLastLoginAt());
    }
}
