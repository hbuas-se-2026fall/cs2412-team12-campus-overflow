package com.campusoverflow.reputation.interfaces.rest.dto;

import com.campusoverflow.reputation.application.view.ReputationView;

import java.time.Instant;
import java.util.List;

/**
 * 声誉响应。
 */
public record ReputationResponse(String userId, int score, List<Entry> entries) {

    /** 明细条目。 */
    public record Entry(String ruleCode, int delta, Instant occurredAt) {
    }

    public static ReputationResponse from(ReputationView view) {
        return new ReputationResponse(
                view.userId(),
                view.score(),
                view.entries().stream()
                        .map(e -> new Entry(e.ruleCode(), e.delta(), e.occurredAt()))
                        .toList());
    }
}
