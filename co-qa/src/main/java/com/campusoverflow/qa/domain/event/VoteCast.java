package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 投票已发生 / 已变更（{@code VoteCast}）。
 *
 * <p>带 {@code targetAuthorId} 与 {@code reputationDelta}：声誉上下文据此给作者加减分，
 * 无需回查问答上下文的聚合；改票时 delta 是"新贡献 − 旧贡献"，撤票时是"− 旧贡献"。</p>
 */
public record VoteCast(ContentTarget target,
                       UserId voterId,
                       UserId targetAuthorId,
                       VoteValue value,
                       int reputationDelta,
                       Instant occurredAt) implements DomainEvent {
}
