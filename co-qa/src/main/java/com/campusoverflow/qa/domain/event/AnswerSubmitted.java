package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 回答已提交（{@code AnswerSubmitted}）。
 */
public record AnswerSubmitted(AnswerId answerId, QuestionId questionId, UserId authorId,
                              Instant occurredAt) implements DomainEvent {
}
