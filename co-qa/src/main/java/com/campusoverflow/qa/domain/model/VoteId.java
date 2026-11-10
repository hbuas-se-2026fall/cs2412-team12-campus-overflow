package com.campusoverflow.qa.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 投票记录标识。
 *
 * <p>Lab 3 把 Vote 的"业务标识"定义为 (targetType, targetId, voterId)；
 * 数据库用唯一键保证这一约束，同时保留一个代理主键方便更新与审计。</p>
 */
public record VoteId(UUID value) {

    public VoteId {
        Objects.requireNonNull(value, "voteId 不能为空");
    }

    public static VoteId newId() {
        return new VoteId(UUID.randomUUID());
    }

    public static VoteId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("voteId 不能为空");
        }
        try {
            return new VoteId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("voteId 不是合法的 UUID：" + raw, ex);
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
