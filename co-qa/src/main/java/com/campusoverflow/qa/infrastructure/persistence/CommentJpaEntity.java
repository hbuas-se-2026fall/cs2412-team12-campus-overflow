package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 评论表 JPA 映射（AGG-05）。
 */
@Entity
@Table(name = "comment")
public class CommentJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "target_type", length = 16, nullable = false)
    private String targetType;

    @Column(name = "target_id", length = 36, nullable = false)
    private String targetId;

    @Column(name = "author_id", length = 36, nullable = false)
    private String authorId;

    @Column(name = "body", length = 500, nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CommentJpaEntity() {
        // for JPA
    }

    public CommentJpaEntity(String id) {
        this.id = id;
    }

    public void apply(String targetType, String targetId, String authorId, String body, Instant createdAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
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

    public String getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
