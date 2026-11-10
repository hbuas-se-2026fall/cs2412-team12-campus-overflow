package com.campusoverflow.qa.domain.model;

import java.util.UUID;

/**
 * 评论标识。
 */
public record CommentId(UUID value) {

    public CommentId {
        if (value == null) {
            throw new IllegalArgumentException("commentId 不能为空");
        }
    }

    public static CommentId newId() {
        return new CommentId(UUID.randomUUID());
    }

    public static CommentId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("commentId 不能为空");
        }
        try {
            return new CommentId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("commentId 不是合法的 UUID：" + raw, ex);
        }
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
