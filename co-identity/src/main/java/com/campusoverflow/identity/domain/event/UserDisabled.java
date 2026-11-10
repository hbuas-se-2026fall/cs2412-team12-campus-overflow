package com.campusoverflow.identity.domain.event;

import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 用户已禁用（{@code UserDisabled}）。
 */
public record UserDisabled(UserId userId, String reason, Instant occurredAt) implements DomainEvent {
}
