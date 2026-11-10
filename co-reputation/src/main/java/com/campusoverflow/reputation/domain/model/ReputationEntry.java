package com.campusoverflow.reputation.domain.model;

import java.time.Instant;

/**
 * 声誉明细（统一语言术语 #27）：一次加减分流水，{@code eventId} 用于幂等（{@code INV-R-02}）。
 */
public record ReputationEntry(long eventId, RuleCode ruleCode, int delta, Instant occurredAt) {
}
