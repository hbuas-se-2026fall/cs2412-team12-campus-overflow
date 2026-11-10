package com.campusoverflow.reputation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 声誉明细表：{@code event_id} 上有唯一键，是 {@code INV-R-02} 的数据库兜底。
 */
@Entity
@Table(name = "reputation_entry")
public class ReputationEntryJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "event_id", length = 160, nullable = false)
    private String eventId;

    @Column(name = "rule_code", length = 32, nullable = false)
    private String ruleCode;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReputationEntryJpaEntity() {
        // for JPA
    }

    public ReputationEntryJpaEntity(String id, String userId, String eventId, String ruleCode,
                                    int delta, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.ruleCode = ruleCode;
        this.delta = delta;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getDelta() {
        return delta;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
