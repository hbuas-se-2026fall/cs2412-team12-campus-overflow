package com.campusoverflow.discovery.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * 问题摘要读模型（统一语言术语 #38）：为列表/搜索优化的投影，不承载业务规则。
 *
 * <p>字段是"复制"而非引用——读模型允许冗余，重建时全量重放即可（见 Lab 3 一致性策略）。</p>
 */
public record QuestionSummary(
        String questionId,
        String title,
        String excerpt,
        String askerId,
        List<String> tags,
        int voteCount,
        int answerCount,
        Instant publishedAt
) {
}
