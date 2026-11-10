package com.campusoverflow.qa.domain.model;

import java.util.UUID;

/**
 * 被采纳答案的弱引用（{@code INV-Q-04}）：只存 ID，不持有 Answer 对象，避免把 Answer 拉进 Question 聚合。
 */
public record AcceptedAnswerId(UUID value) {

    public AcceptedAnswerId {
        if (value == null) {
            throw new IllegalArgumentException("acceptedAnswerId 不能为空");
        }
    }

    public static AcceptedAnswerId of(AnswerId answerId) {
        return new AcceptedAnswerId(answerId.value());
    }

    public static AcceptedAnswerId of(String raw) {
        return new AcceptedAnswerId(AnswerId.of(raw).value());
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
