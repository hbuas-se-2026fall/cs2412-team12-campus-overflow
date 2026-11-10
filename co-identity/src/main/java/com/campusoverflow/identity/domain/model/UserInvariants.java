package com.campusoverflow.identity.domain.model;

/**
 * 身份上下文的不变式编号（与 Lab 3《聚合识别清单》AGG-01 一一对应）。
 */
public final class UserInvariants {

    /** 邮箱全局唯一，比较时忽略大小写。 */
    public static final String EMAIL_UNIQUE = "INV-U-01";

    /** 邮箱格式合法（唯一性的前置条件）。 */
    public static final String EMAIL_FORMAT = "INV-U-01.FMT";

    /** 未验证邮箱（pending）不得发布问题或回答。 */
    public static final String CANNOT_PUBLISH = "INV-U-02";

    /** 密码 ≥ 8 位且同时含字母与数字；只存哈希。 */
    public static final String PASSWORD_POLICY = "INV-U-03";

    /** 状态单向流转：pending→active→disabled，不可回退。 */
    public static final String STATUS_TRANSITION = "INV-U-04";

    /** 昵称格式。 */
    public static final String NICKNAME_FORMAT = "NICKNAME_INVALID";

    private UserInvariants() {
    }
}
