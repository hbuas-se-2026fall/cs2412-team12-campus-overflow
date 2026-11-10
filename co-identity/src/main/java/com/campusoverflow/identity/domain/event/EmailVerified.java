package com.campusoverflow.identity.domain.event;

import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 邮箱已验证（{@code EmailVerified}）。
 */
public record EmailVerified(UserId userId, String email, Instant occurredAt) implements DomainEvent {
}
