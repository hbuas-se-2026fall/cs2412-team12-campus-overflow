package com.campusoverflow.qa.domain.model;

import java.util.Objects;

/**
 * 内容目标：一次投票 / 评论指向"哪个问题或哪条回答"。
 *
 * <p>用 {@code String targetId} 而不是具体聚合 ID 类型，让投票与评论各用同一套目标表达，
 * 又不必为每种内容各写一个值对象。</p>
 */
public record ContentTarget(TargetType type, String targetId) {

    public ContentTarget {
        Objects.requireNonNull(type, "目标类型不能为空");
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("目标标识不能为空");
        }
    }

    public static ContentTarget question(QuestionId id) {
        return new ContentTarget(TargetType.QUESTION, id.asString());
    }

    public static ContentTarget answer(AnswerId id) {
        return new ContentTarget(TargetType.ANSWER, id.asString());
    }

    public static ContentTarget of(TargetType type, String targetId) {
        return new ContentTarget(type, targetId);
    }

    public boolean isQuestion() {
        return type == TargetType.QUESTION;
    }
}
