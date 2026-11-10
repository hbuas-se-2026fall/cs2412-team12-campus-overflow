package com.campusoverflow.identity.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Objects;

/**
 * 密码哈希值对象：只允许存放哈希，绝不允许出现明文（{@code INV-U-03}）。
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "密码哈希不能为空");
        if (value.isBlank()) {
            throw new InvariantViolationException(UserInvariants.PASSWORD_POLICY, "密码哈希不能为空白");
        }
    }

    public static PasswordHash of(String encoded) {
        return new PasswordHash(encoded);
    }

    /** 防止把明文误当哈希塞进来（bcrypt 以 $2a/$2b/$2y 开头）。 */
    public boolean looksEncoded() {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    @Override
    public String toString() {
        return "PasswordHash{***}";
    }
}
