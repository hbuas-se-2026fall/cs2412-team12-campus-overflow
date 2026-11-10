package com.campusoverflow.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 用户表 JPA 映射（持久化模型，与领域模型分离，见 ADR-002）。
 *
 * <p>只有基础设施层知道"表长什么样"：领域对象没有 JPA 注解，可以纯内存单元测试。</p>
 *
 * <p>注意 {@code apply(...)} 的用法：更新时必须先取到受管的实体再改字段，
 * 而不是 new 一个同 ID 的对象去 merge——否则 {@code @Version} 会被当成 0，直接触发乐观锁异常。</p>
 */
@Entity
@Table(name = "user")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @Column(name = "nickname", length = 32, nullable = false)
    private String nickname;

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 乐观锁（统一语言术语 #52）：并发修改由数据库版本号兜底。 */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected UserJpaEntity() {
        // for JPA
    }

    public UserJpaEntity(String id) {
        this.id = id;
    }

    public void apply(String email, String nickname, String passwordHash, String status,
                      Instant createdAt, Instant updatedAt) {
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
