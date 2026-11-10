package com.campusoverflow.reputation.domain.model;

/**
 * 规则码（统一语言术语 #28）：触发加减分的业务规则，分值集中在这里，便于运营调整。
 *
 * <p>数值来自 Lab 2 US-11 的验收要点：提问 +2、回答 +5、被采纳 +15、被赞 +10、被踩 −2。</p>
 */
public enum RuleCode {

    QUESTION_POSTED(2),
    ANSWER_SUBMITTED(5),
    ANSWER_ACCEPTED(15),
    UPVOTE_RECEIVED(10),
    DOWNVOTE_RECEIVED(-2),
    /** 改判冲正：撤销上一位被采纳者的 +15（INV-R-03）。 */
    ACCEPT_REVERSED(-15);

    private final int delta;

    RuleCode(int delta) {
        this.delta = delta;
    }

    public int delta() {
        return delta;
    }

    public String code() {
        return name();
    }
}
