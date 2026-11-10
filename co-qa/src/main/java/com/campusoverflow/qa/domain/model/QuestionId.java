package com.campusoverflow.qa.domain.model;

import java.util.UUID;

/**
 * 问题标识（{@code Question} 聚合根的唯一标识）。
 */
public record QuestionId(UUID value) {

    public QuestionId {
        if (value == null) {
            throw new IllegalArgumentException("questionId 不能为空");
        }
    }

    public static QuestionId newId() {
        return new QuestionId(UUID.randomUUID());
    }

    public static QuestionId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("questionId 不能为空");
        }
        try {
            return new QuestionId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("questionId 不是合法的 UUID：" + raw, ex);
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
