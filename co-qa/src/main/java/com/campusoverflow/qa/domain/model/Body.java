package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;

import java.util.Objects;

/**
 * 正文值对象（统一语言术语 #11）：Markdown 原文，不能为空白。
 *
 * <p>问题与回答的长度上限不同，所以由两个工厂方法显式表达，避免"一个类里塞一个 if 判断是谁"：</p>
 * <ul>
 *   <li>问题正文 ≤ 20000 字（{@code INV-Q-01}）</li>
 *   <li>回答正文 ≤ 10000 字（{@code INV-A-01}）</li>
 * </ul>
 */
public record Body(String value) {

    public static final int QUESTION_MAX_LENGTH = 20_000;
    public static final int ANSWER_MAX_LENGTH = 10_000;

    public Body {
        Objects.requireNonNull(value, "正文不能为空");
    }

    public static Body ofQuestion(String raw) {
        return of(raw, QUESTION_MAX_LENGTH, QuestionInvariants.BODY_LENGTH);
    }

    public static Body ofAnswer(String raw) {
        return of(raw, ANSWER_MAX_LENGTH, AnswerInvariants.BODY_LENGTH);
    }

    private static Body of(String raw, int maxLength, String invariantCode) {
        if (raw == null || raw.isBlank()) {
            throw new InvariantViolationException(invariantCode, "正文不能为空白");
        }
        if (raw.length() > maxLength) {
            throw new InvariantViolationException(invariantCode,
                    "正文长度不能超过 " + maxLength + " 字，当前 " + raw.length() + " 字");
        }
        return new Body(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
