package com.campusoverflow.qa.interfaces.rest.dto;

import com.campusoverflow.qa.domain.model.Comment;

import java.time.Instant;

/**
 * 评论响应。
 */
public record CommentResponse(
        String id,
        String targetType,
        String targetId,
        String authorId,
        String body,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.id().asString(),
                comment.target().type().code(),
                comment.target().targetId(),
                comment.authorId().asString(),
                comment.body(),
                comment.createdAt());
    }
}
