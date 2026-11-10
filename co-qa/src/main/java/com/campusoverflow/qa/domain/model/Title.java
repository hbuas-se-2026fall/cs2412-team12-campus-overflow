package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Objects;

/**
 * 问题标题值对象（{@code INV-Q-01}）：5–150 字。
 *
 * <p>长度按 Unicode 码点计算，避免中文、emoji 在 {@code String.length()} 下被算成两个字符。</p>
 */
public record Title(String value) {

    public static final int MIN_LENGTH = 5;
    public static final int MAX_LENGTH = 150;

    public Title {
        Objects.requireNonNull(value, "标题不能为空");
        int length = value.codePointCount(0, value.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new InvariantViolationException(QuestionInvariants.TITLE_LENGTH,
                    "标题长度必须在 " + MIN_LENGTH + "–" + MAX_LENGTH + " 字之间，当前 " + length + " 字");
        }
    }

    public static Title of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvariantViolationException(QuestionInvariants.TITLE_LENGTH, "标题不能为空白");
        }
        return new Title(raw.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
