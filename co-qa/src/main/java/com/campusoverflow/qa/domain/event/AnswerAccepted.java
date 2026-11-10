package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.AcceptedAnswerId;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 答案已被采纳（{@code AnswerAccepted}）。
 *
 * <p>同时带上"新作者"与"被撤销的旧作者"：声誉上下文要在改判时先冲正旧作者 +15，
 * 再给新作者 +15（{@code INV-R-03}）。只带 {@code previousAcceptedAnswerId} 不够，
 * 因为声誉上下文不应反向查询问答上下文的数据。</p>
 *
 * <p>{@code previousAcceptedAnswerId} / {@code previousAnswerAuthorId} 在首次采纳时为 null。</p>
 */
public record AnswerAccepted(QuestionId questionId,
                             AnswerId acceptedAnswerId,
                             AcceptedAnswerId previousAcceptedAnswerId,
                             UserId askerId,
                             UserId acceptedAnswerAuthorId,
                             UserId previousAnswerAuthorId,
                             Instant occurredAt) implements DomainEvent {
}
