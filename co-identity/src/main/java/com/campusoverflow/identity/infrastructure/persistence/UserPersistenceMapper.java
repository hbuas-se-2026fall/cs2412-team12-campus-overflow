package com.campusoverflow.identity.infrastructure.persistence;

import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.Nickname;
import com.campusoverflow.identity.domain.model.PasswordHash;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.identity.domain.model.UserStatus;
import com.campusoverflow.shared.identity.UserId;

import java.util.Locale;

/**
 * 领域对象 ↔ 持久化对象的翻译（只在基础设施层出现）。
 */
final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    /** 把领域状态写进受管实体（新建或更新都走这里）。 */
    static void apply(User user, UserJpaEntity entity) {
        entity.apply(
                user.email().value(),
                user.nickname().value(),
                user.passwordHash().value(),
                // 库里存小写（与 DDL 注释、接口契约一致），枚举名只用于代码内部
                user.status().code(),
                user.createdAt(),
                user.updatedAt());
    }

    /**
     * 重建领域对象：{@code version} 是持久化关注点，领域层不感知（见 ADR-002）。
     */
    static User toDomain(UserJpaEntity entity) {
        return User.rehydrate(
                UserId.of(entity.getId()),
                new Email(entity.getEmail()),
                new Nickname(entity.getNickname()),
                PasswordHash.of(entity.getPasswordHash()),
                UserStatus.valueOf(entity.getStatus().toUpperCase(Locale.ROOT)),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
