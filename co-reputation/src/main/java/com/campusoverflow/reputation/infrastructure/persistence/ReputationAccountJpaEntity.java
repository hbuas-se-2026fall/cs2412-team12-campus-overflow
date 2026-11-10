package com.campusoverflow.reputation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 声誉账户表（AGG-06）。
 */
@Entity
@Table(name = "reputation_account")
public class ReputationAccountJpaEntity {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReputationAccountJpaEntity() {
        // for JPA
    }

    public ReputationAccountJpaEntity(String userId) {
        this.userId = userId;
    }

    public void apply(int score, Instant updatedAt) {
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
