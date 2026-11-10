package com.campusoverflow.identity.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Objects;

/**
 * 昵称值对象：1–32 个字符，去掉首尾空白后不能为空。
 */
public record Nickname(String value) {

    public static final int MAX_LENGTH = 32;

    public Nickname {
        Objects.requireNonNull(value, "昵称不能为空");
        if (value.isBlank()) {
            throw new InvariantViolationException(UserInvariants.NICKNAME_FORMAT, "昵称不能为空白");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvariantViolationException(UserInvariants.NICKNAME_FORMAT, "昵称长度不能超过 " + MAX_LENGTH + " 个字符");
        }
    }

    public static Nickname of(String raw) {
        if (raw == null) {
            throw new InvariantViolationException(UserInvariants.NICKNAME_FORMAT, "昵称不能为空");
        }
        return new Nickname(raw.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
