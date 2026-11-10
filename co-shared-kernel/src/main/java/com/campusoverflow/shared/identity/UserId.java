package com.campusoverflow.shared.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * 用户标识：跨上下文的发布语言（Published Language）。
 *
 * <p>身份上下文拥有 User 聚合，但 Question/Answer/Vote/Reputation 都要引用"谁"，
 * 因此把 ID 放在共享内核，各上下文只传递标识、不传递 User 对象（聚合间只引用 ID）。</p>
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "userId 不能为空");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        try {
            return new UserId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("userId 不是合法的 UUID：" + raw, ex);
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
