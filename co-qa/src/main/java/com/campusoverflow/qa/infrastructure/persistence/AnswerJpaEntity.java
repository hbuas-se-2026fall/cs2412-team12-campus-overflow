package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 回答表 JPA 映射（持久化模型，与领域模型分离）。
 */
@Entity
@Table(name = "answer")
public class AnswerJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "question_id", length = 36, nullable = false)
    private String questionId;

    @Column(name = "author_id", length = 36, nullable = false)
    private String authorId;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AnswerJpaEntity() {
        // for JPA
    }

    public AnswerJpaEntity(String id) {
        this.id = id;
    }

    public void apply(String questionId, String authorId, String body, String status, boolean accepted,
                      Instant createdAt, Instant updatedAt) {
        this.questionId = questionId;
        this.authorId = authorId;
        this.body = body;
        this.status = status;
        this.accepted = accepted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public boolean isAccepted() {
        return accepted;
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
