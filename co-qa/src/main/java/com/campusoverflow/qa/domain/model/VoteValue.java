package com.campusoverflow.qa.domain.model;

/**
 * 投票取值：赞成票 +1 / 反对票 −1（统一语言术语 #17/#18）。
 */
public enum VoteValue {

    UP(1),
    DOWN(-1);

    private final int score;

    VoteValue(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }

    public String code() {
        return name().toLowerCase();
    }
}
