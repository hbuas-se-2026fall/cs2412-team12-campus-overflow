package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * 问题表 JPA 映射（持久化模型，与领域模型分离）。
 *
 * <p>标签在库里以逗号分隔字符串存储：标签语法本身禁止逗号，Sprint 3 的 US-10 要按标签检索时会拆出
 * {@code question_tag} 表，届时只改这一层，领域模型不动。</p>
 */
@Entity
@Table(name = "question")
public class QuestionJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "asker_id", length = 36, nullable = false)
    private String askerId;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "tags", length = 512, nullable = false)
    private String tags;

    @Column(name = "accepted_answer_id", length = 36)
    private String acceptedAnswerId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected QuestionJpaEntity() {
        // for JPA
    }

    public QuestionJpaEntity(String id) {
        this.id = id;
    }

    public void apply(String askerId, String title, String body, String tags, String acceptedAnswerId,
                      String status, int voteCount, Instant createdAt, Instant updatedAt) {
        this.askerId = askerId;
        this.title = title;
        this.body = body;
        this.tags = tags;
        this.acceptedAnswerId = acceptedAnswerId;
        this.status = status;
        this.voteCount = voteCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getAskerId() {
        return askerId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getTags() {
        return tags;
    }

    public String getAcceptedAnswerId() {
        return acceptedAnswerId;
    }

    public String getStatus() {
        return status;
    }

    public int getVoteCount() {
        return voteCount;
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
