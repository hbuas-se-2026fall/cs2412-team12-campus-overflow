package com.campusoverflow.reputation.domain.model;

import java.time.Instant;

/**
 * 声誉明细（统一语言术语 #27）：一次加减分流水。
 *
 * <p>{@code eventId} 是幂等键（{@code INV-R-02}）：同一业务事件重复投递时只记一次。
 * 用字符串而不是自增 ID，是因为它来自事件本身（如 {@code QP:<questionId>}）。</p>
 */
public record ReputationEntry(String eventId, RuleCode ruleCode, int delta, Instant occurredAt) {
}
