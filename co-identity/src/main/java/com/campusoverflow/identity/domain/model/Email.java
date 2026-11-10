package com.campusoverflow.identity.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 邮箱值对象：统一小写归一化，保证"忽略大小写比较"这件事只在构造时做一次。
 *
 * <p>唯一性（{@code INV-U-01}）由仓储 + 数据库唯一索引保证，值对象只负责格式与归一化。</p>
 */
public record Email(String value) {

    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "邮箱不能为空");
        if (value.length() > MAX_LENGTH) {
            throw new InvariantViolationException(UserInvariants.EMAIL_FORMAT, "邮箱长度不能超过 " + MAX_LENGTH + " 个字符");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvariantViolationException(UserInvariants.EMAIL_FORMAT, "邮箱格式不合法：" + value);
        }
    }

    /** 去空白 + 转小写（RFC 5321 的域部分本就大小写不敏感，此处整个地址统一小写）。 */
    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvariantViolationException(UserInvariants.EMAIL_FORMAT, "邮箱不能为空白");
        }
        return new Email(raw.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
