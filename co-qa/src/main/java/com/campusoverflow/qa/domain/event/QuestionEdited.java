package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 问题已编辑（{@code QuestionEdited}）。
 */
public record QuestionEdited(QuestionId questionId, UserId editorId, Instant occurredAt) implements DomainEvent {
}
