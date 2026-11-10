package com.campusoverflow.identity.domain.repository;

import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.shared.identity.UserId;

import java.util.Optional;

/**
 * 用户仓储（统一语言术语 #53）：接口定义在 domain 层，实现放在 infrastructure 层。
 *
 * <p>领域层只表达"我要按 ID / 邮箱找一个用户、我要保存一个用户"，
 * 至于用 JPA 还是别的，领域层不关心。</p>
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    /** {@code INV-U-01} 的存在性检查（最终仍由数据库唯一索引兜底）。 */
    boolean existsByEmail(Email email);

    User save(User user);
}
