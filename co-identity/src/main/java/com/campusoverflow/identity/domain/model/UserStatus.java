package com.campusoverflow.identity.domain.model;

/**
 * 账号状态（统一语言术语 #1）：单向流转 PENDING → ACTIVE → DISABLED（{@code INV-U-04}）。
 */
public enum UserStatus {

    /** 已注册、邮箱待验证。 */
    PENDING,

    /** 邮箱已验证，可发布内容。 */
    ACTIVE,

    /** 已禁用，不可逆。 */
    DISABLED;

    public boolean canTransitTo(UserStatus next) {
        return switch (this) {
            case PENDING -> next == ACTIVE;
            case ACTIVE -> next == DISABLED;
            case DISABLED -> false;
        };
    }

    public String code() {
        return name().toLowerCase();
    }
}
