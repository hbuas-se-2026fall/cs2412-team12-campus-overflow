package com.campusoverflow.qa.domain.model;

/**
 * 投票目标类型。
 */
public enum TargetType {
    QUESTION,
    ANSWER;

    public String code() {
        return name().toLowerCase();
    }
}
