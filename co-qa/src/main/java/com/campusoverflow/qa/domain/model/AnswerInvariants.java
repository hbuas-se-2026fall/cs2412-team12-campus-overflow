package com.campusoverflow.qa.domain.model;

/**
 * 回答的不变式编号（Lab 3 AGG-03）。
 */
public final class AnswerInvariants {

    /** 内容非空，≤ 10000 字。 */
    public static final String BODY_LENGTH = "INV-A-01";

    /** 引用的 questionId 必须存在且不是 hidden（由应用服务校验，见 QuestionApplicationService）。 */
    public static final String QUESTION_MUST_BE_VISIBLE = "INV-A-02";

    /** 同一作者对同一问题 60 秒内不得重复提交（应用层限流）。 */
    public static final String SUBMIT_TOO_FREQUENT = "INV-A-03";

    /** 被采纳的回答不可删除，只能编辑。 */
    public static final String ACCEPTED_CANNOT_DELETE = "INV-A-04";

    /** 仅回答者本人可编辑/删除。 */
    public static final String ONLY_AUTHOR = "ANSWER_ONLY_AUTHOR";

    private AnswerInvariants() {
    }
}
