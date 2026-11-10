package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 投票表 JPA 映射（AGG-04）。
 *
 * <p>唯一键 {@code uk_vote_target_voter} 是 {@code INV-V-01} 的最后一道防线：
 * 并发双击时应用层"先查后写"可能同时通过，数据库唯一键会拒绝第二条。</p>
 */
@Entity
@Table(name = "vote")
public class VoteJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "target_type", length = 16, nullable = false)
    private String targetType;

    @Column(name = "target_id", length = 36, nullable = false)
    private String targetId;

    @Column(name = "voter_id", length = 36, nullable = false)
    private String voterId;

    @Column(name = "vote_value", length = 8, nullable = false)
    private String value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected VoteJpaEntity() {
        // for JPA
    }

    public VoteJpaEntity(String id) {
        this.id = id;
    }

    public void apply(String targetType, String targetId, String voterId, String value,
                      Instant createdAt, Instant updatedAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.voterId = voterId;
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getVoterId() {
        return voterId;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
