package com.campusoverflow.qa.domain.model;

/**
 * 回答状态：normal / hidden。
 */
public enum AnswerStatus {

    NORMAL,
    HIDDEN;

    public boolean isVisible() {
        return this == NORMAL;
    }

    public String code() {
        return name().toLowerCase();
    }
}
