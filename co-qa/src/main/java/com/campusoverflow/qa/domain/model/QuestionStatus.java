package com.campusoverflow.qa.domain.model;

/**
 * 问题状态：open / solved / hidden。
 *
 * <p>hidden 是终态；hidden 后不可再编辑、不可再被回答、不可被投票（{@code INV-Q-05}）。</p>
 */
public enum QuestionStatus {

    OPEN,
    SOLVED,
    HIDDEN;

    public boolean isVisible() {
        return this != HIDDEN;
    }

    public String code() {
        return name().toLowerCase();
    }
}
