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
 * <p>{@code previousAcceptedAnswerId} 是 {@code INV-Q-04} 的留痕：声誉上下文靠它做"改判冲正"
 * （先扣回上一位作者 +15，再给新作者 +15），否则无法回溯是谁被撤销了采纳。</p>
 */
public record AnswerAccepted(QuestionId questionId,
                             AnswerId acceptedAnswerId,
                             AcceptedAnswerId previousAcceptedAnswerId,
                             UserId askerId,
                             Instant occurredAt) implements DomainEvent {
}
