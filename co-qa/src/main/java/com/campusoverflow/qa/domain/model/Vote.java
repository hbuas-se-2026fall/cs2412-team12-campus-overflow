package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 投票值对象（统一语言术语 #16）：一次"谁对哪个目标投了什么票"的不可变记录。
 *
 * <p>Lab 4 先把它实现为值对象（Step 2 的要求）；Lab 3 的结论是 Vote 最终要独立成聚合，
 * 因为票数需要从 Question 行锁里解耦——见 ADR-002 的"后续演进"一节。</p>
 *
 * <p>不变式：</p>
 * <ul>
 *   <li>{@code INV-V-02} 不能给自己的内容投票 —— 构造时校验；</li>
 *   <li>{@code INV-V-03} 改票 = 撤销旧票 + 投新票，净变化 2 —— 由 {@link #revise} 与 {@link #netDelta} 表达；</li>
 *   <li>{@code INV-V-01} 一人一票由仓储唯一键兜底；{@code INV-V-04} 目标下架由 Question 聚合拒绝。</li>
 * </ul>
 */
public record Vote(VoteTarget target, UserId voterId, VoteValue value, Instant castAt) {

    public Vote {
        Objects.requireNonNull(target, "投票目标不能为空");
        Objects.requireNonNull(voterId, "投票人不能为空");
        Objects.requireNonNull(value, "投票取值不能为空");
        Objects.requireNonNull(castAt, "投票时间不能为空");
    }

    /** 投出一票：内容作者给自己投票会被直接拒绝（{@code INV-V-02}）。 */
    public static Vote cast(VoteTarget target, UserId voterId, UserId targetAuthorId,
                            VoteValue value, Instant now) {
        if (Objects.equals(voterId, targetAuthorId)) {
            throw new InvariantViolationException(VoteInvariants.SELF_VOTE, "不能给自己的内容投票");
        }
        return new Vote(target, voterId, value, now);
    }

    /** 改票：返回一张新票（旧票作废），净效果见 {@link #netDelta}。 */
    public Vote revise(VoteValue newValue, Instant now) {
        Objects.requireNonNull(newValue, "投票取值不能为空");
        if (newValue == this.value) {
            return this;
        }
        return new Vote(target, voterId, newValue, now);
    }

    /**
     * 改票造成的票数净变化：赞成→反对 = −2，反对→赞成 = +2，不变 = 0（{@code INV-V-03}）。
     */
    public static int netDelta(VoteValue previous, VoteValue next) {
        Objects.requireNonNull(previous, "previous 不能为空");
        Objects.requireNonNull(next, "next 不能为空");
        return next.score() - previous.score();
    }
}
