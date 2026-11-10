package com.campusoverflow.reputation.domain.model;

import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.domain.InvariantViolationException;
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
 *   <li>{@code INV-R-02} 同一 {@code eventId} 只计一次 —— 明细表按 eventId 去重</li>
 * </ul>
 *
 * <p>Lab 4 只交付领域骨架与单元测试；持久化与事件订阅随 Sprint 3 的 US-11 落地（见 ADR-002）。</p>
 */
public final class ReputationAccount extends AggregateRoot {

    private final UserId userId;
    private final Map<Long, ReputationEntry> entries;
    private ReputationScore score;

    private ReputationAccount(UserId userId, ReputationScore score, Map<Long, ReputationEntry> entries) {
        this.userId = Objects.requireNonNull(userId, "userId 不能为空");
        this.score = Objects.requireNonNull(score, "score 不能为空");
        this.entries = new LinkedHashMap<>(entries);
    }

    public static ReputationAccount open(UserId userId) {
        return new ReputationAccount(userId, ReputationScore.initial(), Map.of());
    }

    public static ReputationAccount rehydrate(UserId userId, ReputationScore score, Map<Long, ReputationEntry> entries) {
        return new ReputationAccount(userId, score, entries);
    }

    /**
     * 应用一次声誉事件。
     *
     * @return 是否真的计分（重复 eventId 返回 false，保证"至少一次投递"下的幂等）
     */
    public boolean apply(long eventId, RuleCode ruleCode, Instant now) {
        Objects.requireNonNull(ruleCode, "ruleCode 不能为空");
        if (entries.containsKey(eventId)) {
            return false;
        }
        int delta = ruleCode.delta();
        this.score = score.plus(delta);
        entries.put(eventId, new ReputationEntry(eventId, ruleCode, delta, Objects.requireNonNull(now, "now 不能为空")));
        return true;
    }

    /** 改判冲正（{@code INV-R-03}）：先扣回上一位作者，再给新作者加，调用方保证成对执行。 */
    public void reverse(long previousEventId, Instant now) {
        ReputationEntry entry = entries.get(previousEventId);
        if (entry == null) {
            throw new InvariantViolationException("INV-R-03", "找不到可冲正的声誉明细：" + previousEventId);
        }
        this.score = score.plus(-entry.delta());
        entries.remove(previousEventId);
    }

    public UserId userId() {
        return userId;
    }

    public ReputationScore score() {
        return score;
    }

    public Map<Long, ReputationEntry> entries() {
        return Map.copyOf(entries);
    }
}
