package com.campusoverflow.reputation.domain.model;

import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 声誉账户聚合根（Lab 3 AGG-06）：以 {@code userId} 作为聚合标识。
 *
 * <p>不变式：</p>
 * <ul>
 *   <li>{@code INV-R-01} 分数永不低于 1 —— {@link ReputationScore#plus}</li>
 *   <li>{@code INV-R-02} 同一 {@code eventId} 只计一次 —— 明细按 eventId 去重，数据库再加唯一键</li>
 *   <li>{@code INV-R-03} 改判冲正 —— 由事件层发出一条 {@code ACCEPT_REVERSED(-15)} 明细实现</li>
 * </ul>
 */
public final class ReputationAccount extends AggregateRoot {

    private final UserId userId;
    private final Map<String, ReputationEntry> entries;
    private ReputationScore score;

    private ReputationAccount(UserId userId, ReputationScore score, Map<String, ReputationEntry> entries) {
        this.userId = Objects.requireNonNull(userId, "userId 不能为空");
        this.score = Objects.requireNonNull(score, "score 不能为空");
        this.entries = new LinkedHashMap<>(entries);
    }

    public static ReputationAccount open(UserId userId) {
        return new ReputationAccount(userId, ReputationScore.initial(), Map.of());
    }

    public static ReputationAccount rehydrate(UserId userId, ReputationScore score,
                                              Map<String, ReputationEntry> entries) {
        return new ReputationAccount(userId, score, entries);
    }

    /**
     * 按规则码的固定分值计分。
     *
     * @return 是否真的计分（重复 eventId 返回 false，保证"至少一次投递"下的幂等）
     */
    public boolean apply(String eventId, RuleCode ruleCode, Instant now) {
        Objects.requireNonNull(ruleCode, "ruleCode 不能为空");
        return applyDelta(eventId, ruleCode, ruleCode.delta(), now);
    }

    /**
     * 按显式分值计分：投票改判时"新贡献 − 旧贡献"可能是 ±12，不能直接用规则码固定值。
     */
    public boolean applyDelta(String eventId, RuleCode ruleCode, int delta, Instant now) {
        Objects.requireNonNull(eventId, "eventId 不能为空");
        Objects.requireNonNull(ruleCode, "ruleCode 不能为空");
        if (entries.containsKey(eventId)) {
            return false;
        }
        this.score = score.plus(delta);
        entries.put(eventId, new ReputationEntry(eventId, ruleCode, delta,
                Objects.requireNonNull(now, "now 不能为空")));
        return true;
    }

    public UserId userId() {
        return userId;
    }

    public ReputationScore score() {
        return score;
    }

    public Map<String, ReputationEntry> entries() {
        return Map.copyOf(entries);
    }
}
