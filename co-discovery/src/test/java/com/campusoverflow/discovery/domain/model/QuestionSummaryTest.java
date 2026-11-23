package com.campusoverflow.discovery.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 读模型记录的最小测试：保证骨架类也被实例化，避免覆盖率门禁出现"0/1 行"的假空白。
 */
class QuestionSummaryTest {

    @Test
    @DisplayName("QuestionSummary 保存列表展示所需字段")
    void holdsFields() {
        Instant publishedAt = Instant.parse("2026-11-23T00:00:00Z");
        QuestionSummary summary = new QuestionSummary("q-1", "标题", "摘要", "u-1",
                List.of("java"), 3, 2, publishedAt);

        assertEquals("q-1", summary.questionId());
        assertEquals(3, summary.voteCount());
        assertEquals(2, summary.answerCount());
        assertEquals(publishedAt, summary.publishedAt());
    }
}
