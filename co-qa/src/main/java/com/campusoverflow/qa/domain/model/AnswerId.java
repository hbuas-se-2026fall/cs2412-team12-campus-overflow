package com.campusoverflow.qa.domain.model;

import java.util.UUID;

/**
 * 回答标识（{@code Answer} 聚合根的唯一标识）。
 */
public record AnswerId(UUID value) {

    public AnswerId {
        if (value == null) {
            throw new IllegalArgumentException("answerId 不能为空");
        }
    }

    public static AnswerId newId() {
        return new AnswerId(UUID.randomUUID());
    }

    public static AnswerId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("answerId 不能为空");
        }
        try {
            return new AnswerId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("answerId 不是合法的 UUID：" + raw);
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
