package com.campusoverflow.bootstrap.infrastructure.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 标签词表实体（SK-01，US-10）。
 *
 * <p>放在装配层：它是共享内核契约 {@code TagVocabulary} 的持久化实现，
 * 不属于问答或发现任一上下文。</p>
 */
@Entity
@Table(name = "tag")
public class TagJpaEntity {

    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "label", length = 20, nullable = false)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TagJpaEntity() {
        // for JPA
    }

    public TagJpaEntity(String code, String label, Instant createdAt) {
        this.code = code;
        this.label = label;
        this.createdAt = createdAt;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
