package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.domain.DomainEvent;

import java.time.Instant;

/**
 * 问题已下架（{@code QuestionHidden}）。
 */
public record QuestionHidden(QuestionId questionId, String reason, Instant occurredAt) implements DomainEvent {
}
