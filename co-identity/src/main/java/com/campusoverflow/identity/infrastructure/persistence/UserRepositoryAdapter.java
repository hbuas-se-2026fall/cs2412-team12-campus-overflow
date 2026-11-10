package com.campusoverflow.identity.infrastructure.persistence;

import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.identity.domain.repository.UserRepository;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现：JPA 细节全部关在这个类里，领域层只看到 {@link UserRepository} 接口。
 */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    public UserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.asString()).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = jpa.findById(user.id().asString())
                .orElseGet(() -> new UserJpaEntity(user.id().asString()));
        UserPersistenceMapper.apply(user, entity);
        return UserPersistenceMapper.toDomain(jpa.save(entity));
    }
}
