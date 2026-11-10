package com.campusoverflow.shared.tag;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 标签（统一语言术语 #12）：来自共享内核 SK-01 词表的 2–20 字分类词。
 *
 * <p>值对象不可变，构造即校验：长度 2–20、仅允许小写字母/数字/汉字/连字符，统一小写归一化。
 * 术语格式校验用 {@code INV-SK-01}，"必须存在于词表"的校验在 {@link TagVocabulary} 使用处（{@code INV-Q-02}）。</p>
 */
public record Tag(String value) {

    public static final String INVARIANT_CODE = "INV-SK-01";
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 20;

    private static final Pattern GRAMMAR = Pattern.compile("^[a-z0-9\\u4e00-\\u9fa5-]{2,20}$");

    public Tag {
        Objects.requireNonNull(value, "标签不能为空");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvariantViolationException(INVARIANT_CODE,
                    "标签长度必须在 " + MIN_LENGTH + "–" + MAX_LENGTH + " 字之间：" + value);
        }
        if (!GRAMMAR.matcher(value).matches()) {
            throw new InvariantViolationException(INVARIANT_CODE,
                    "标签只能包含小写字母、数字、汉字或连字符：" + value);
        }
    }

    /** 归一化：去首尾空白 + 统一小写。 */
    public static Tag of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvariantViolationException(INVARIANT_CODE, "标签不能为空白");
        }
        return new Tag(raw.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
