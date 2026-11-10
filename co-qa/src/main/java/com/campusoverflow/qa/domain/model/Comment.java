package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 评论聚合（Lab 3 AGG-05）：对问题或回答的短补充，≤500 字，不盖楼。
 *
 * <p>不变式：</p>
 * <ul>
 *   <li>{@code INV-C-01} 内容非空且 ≤500 字</li>
 *   <li>{@code INV-C-02} 目标存在（应用服务校验）</li>
 *   <li>{@code INV-C-03} 仅作者可删除自己的评论</li>
 * </ul>
 */
public final class Comment extends AggregateRoot {

    public static final int MAX_LENGTH = 500;

    private final CommentId id;
    private final ContentTarget target;
    private final UserId authorId;
    private final String body;
    private final Instant createdAt;

    private Comment(CommentId id, ContentTarget target, UserId authorId, String body, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "commentId 不能为空");
        this.target = Objects.requireNonNull(target, "评论目标不能为空");
        this.authorId = Objects.requireNonNull(authorId, "评论人不能为空");
        this.body = Objects.requireNonNull(body, "评论内容不能为空");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
    }

    /** 发表评论（{@code PostComment}）。 */
    public static Comment post(CommentId id, ContentTarget target, UserId authorId, String rawBody, Instant now) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new InvariantViolationException("INV-C-01", "评论内容不能为空");
        }
        String trimmed = rawBody.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new InvariantViolationException("INV-C-01", "评论不能超过 " + MAX_LENGTH + " 字");
        }
        return new Comment(id, target, authorId, trimmed, now);
    }

    public static Comment rehydrate(CommentId id, ContentTarget target, UserId authorId,
                                    String body, Instant createdAt) {
        return new Comment(id, target, authorId, body, createdAt);
    }

    /** 删除前守卫：仅作者可删自己的评论（{@code INV-C-03}）。 */
    public void assertDeletable(UserId operatorId) {
        if (!authorId.equals(operatorId)) {
            throw new InvariantViolationException("INV-C-03", "只能删除自己的评论");
        }
    }

    public CommentId id() {
        return id;
    }

    public ContentTarget target() {
        return target;
    }

    public UserId authorId() {
        return authorId;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
