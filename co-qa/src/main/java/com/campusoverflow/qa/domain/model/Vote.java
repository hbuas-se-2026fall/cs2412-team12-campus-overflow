package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 投票聚合（Lab 3 AGG-04，Sprint 3 从值对象升为聚合）。
 *
 * <p>为什么独立：票数如果直接在 Question / Answer 行上加减并加锁，热点内容会把所有投票串行化。
 * 现在每条投票是独立记录，"一人一票"由唯一键兜底，票数只是目标上的投影值。</p>
 *
 * <p>不变式：</p>
 * <ul>
 *   <li>{@code INV-V-01} 一人对同一目标最多一票 —— 数据库唯一键 + 仓储按 (target, voter) 查找</li>
 *   <li>{@code INV-V-02} 不能给自己的内容投票 —— {@link #cast}</li>
 *   <li>{@code INV-V-03} 改票 = 撤旧 + 投新，净变化 2 —— {@link #revise} 与 {@link #netDelta}</li>
 *   <li>{@code INV-V-04} 目标下架后不再接受投票 —— 由目标聚合的 {@code applyVoteProjection} 拒绝</li>
 * </ul>
 */
public record Vote(VoteId id, ContentTarget target, UserId voterId, VoteValue value, Instant castAt) {

    /** 被赞成时目标作者的声誉增量（US-11：被赞 +10）。 */
    public static final int UPVOTE_POINTS = 10;
    /** 被反对时目标作者的声誉增量（US-11：被踩 −2）。 */
    public static final int DOWNVOTE_POINTS = -2;

    public Vote {
        Objects.requireNonNull(id, "voteId 不能为空");
        Objects.requireNonNull(target, "投票目标不能为空");
        Objects.requireNonNull(voterId, "投票人不能为空");
        Objects.requireNonNull(value, "投票取值不能为空");
        Objects.requireNonNull(castAt, "投票时间不能为空");
    }

    /** 投出一票：内容作者给自己投票会被直接拒绝（{@code INV-V-02}）。 */
    public static Vote cast(ContentTarget target, UserId voterId, UserId targetAuthorId,
                            VoteValue value, Instant now) {
        if (Objects.equals(voterId, targetAuthorId)) {
            throw new InvariantViolationException(VoteInvariants.SELF_VOTE, "不能给自己的内容投票");
        }
        return new Vote(VoteId.newId(), target, voterId, value, now);
    }

    public static Vote rehydrate(VoteId id, ContentTarget target, UserId voterId,
                                 VoteValue value, Instant castAt) {
        return new Vote(id, target, voterId, value, castAt);
    }

    /** 改票：返回一张新票（旧票作废），净效果见 {@link #netDelta}。 */
    public Vote revise(VoteValue newValue, Instant now) {
        Objects.requireNonNull(newValue, "投票取值不能为空");
        if (newValue == this.value) {
            return this;
        }
        return new Vote(id, target, voterId, newValue, now);
    }

    /**
     * 改票造成的票数净变化：赞成→反对 = −2，反对→赞成 = +2，不变 = 0（{@code INV-V-03}）。
     */
    public static int netDelta(VoteValue previous, VoteValue next) {
        Objects.requireNonNull(previous, "previous 不能为空");
        Objects.requireNonNull(next, "next 不能为空");
        return next.score() - previous.score();
    }

    /** 某一张票对目标作者声誉的贡献（改票时用新值贡献减旧值贡献）。 */
    public static int reputationPoints(VoteValue value) {
        return value == VoteValue.UP ? UPVOTE_POINTS : DOWNVOTE_POINTS;
    }
}
