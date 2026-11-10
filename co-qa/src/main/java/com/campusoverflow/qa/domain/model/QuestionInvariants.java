package com.campusoverflow.qa.domain.model;

/**
 * 问答核心的不变式编号（与 Lab 3《聚合识别清单》AGG-02 一一对应）。
 *
 * <p>接口错误响应、日志、测试断言统一引用这里的常量，避免"文档写 INV-Q-04、代码写 4004"。</p>
 */
public final class QuestionInvariants {

    /** 标题 5–150 字，正文 ≤ 20000 字。 */
    public static final String TITLE_LENGTH = "INV-Q-01";
    public static final String BODY_LENGTH = "INV-Q-01";

    /** 标签最多 5 个，且必须存在于 SK-01 词表。 */
    public static final String TAG_SET = "INV-Q-02";

    /** 只有提问者本人可以采纳。 */
    public static final String ACCEPT_ONLY_ASKER = "INV-Q-03";

    /** 最多一个被采纳答案；改判必须记录 previousAcceptedAnswerId。 */
    public static final String SINGLE_ACCEPTED = "INV-Q-04";

    /** hidden 状态的问题不可再被回答、不可被投票、不可编辑。 */
    public static final String HIDDEN_IMMUTABLE = "INV-Q-05";

    /** 仅提问者可编辑问题（Lab 3 未单列编号，沿用 INV-Q-03 的"本人"语义，代码用独立码便于前端区分）。 */
    public static final String EDIT_ONLY_ASKER = "QUESTION_EDIT_FORBIDDEN";

    private QuestionInvariants() {
    }
}
