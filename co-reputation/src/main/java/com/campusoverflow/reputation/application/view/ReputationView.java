package com.campusoverflow.reputation.application.view;

import com.campusoverflow.reputation.domain.model.ReputationAccount;
import com.campusoverflow.reputation.domain.model.ReputationEntry;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * 声誉视图：账户分值 + 最近明细（按时间倒序）。
 */
public record ReputationView(String userId, int score, List<Entry> entries) {

    /** 一条明细。 */
    public record Entry(String ruleCode, int delta, Instant occurredAt) {
    }

    public static ReputationView of(ReputationAccount account) {
        List<Entry> entries = account.entries().values().stream()
                .sorted(Comparator.comparing(ReputationEntry::occurredAt).reversed())
                .map(entry -> new Entry(entry.ruleCode().code(), entry.delta(), entry.occurredAt()))
                .toList();
        return new ReputationView(account.userId().asString(), account.score().value(), entries);
    }

    /** 还没有任何声誉事件：按 INV-R-01，初始 1 分。 */
    public static ReputationView initial(String userId) {
        return new ReputationView(userId, 1, List.of());
    }
}
