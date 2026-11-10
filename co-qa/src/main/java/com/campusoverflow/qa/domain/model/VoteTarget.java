package com.campusoverflow.qa.domain.model;

import java.util.Objects;

/**
 * 投票目标：类型 + 目标标识。
 *
 * <p>用 {@code String targetId} 而不是 {@code QuestionId}，是为了让同一个值对象同时服务问题与回答，
 * 避免 Vote 依赖两个聚合的具体类型。</p>
 */
public record VoteTarget(TargetType type, String targetId) {

    public VoteTarget {
        Objects.requireNonNull(type, "投票目标类型不能为空");
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("投票目标标识不能为空");
        }
    }

    public static VoteTarget question(QuestionId id) {
        return new VoteTarget(TargetType.QUESTION, id.asString());
    }

    public static VoteTarget answer(AnswerId id) {
        return new VoteTarget(TargetType.ANSWER, id.asString());
    }
}
