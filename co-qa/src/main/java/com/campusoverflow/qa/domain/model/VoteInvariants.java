package com.campusoverflow.qa.domain.model;

/**
 * 投票的不变式编号（Lab 3 AGG-04）。
 */
public final class VoteInvariants {

    /** 一人对同一目标最多一票。 */
    public static final String ONE_VOTE_PER_TARGET = "INV-V-01";

    /** 不能给自己的内容投票。 */
    public static final String SELF_VOTE = "INV-V-02";

    /** 改票 = 先撤销旧票再投新票，票数净变化为 2。 */
    public static final String REVISE_DELTA = "INV-V-03";

    /** 目标被下架后不再接受投票。 */
    public static final String TARGET_HIDDEN = "INV-V-04";

    private VoteInvariants() {
    }
}
