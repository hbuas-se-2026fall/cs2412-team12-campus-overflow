package com.campusoverflow.identity.domain.event;

import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 账号已注册（{@code AccountRegistered}）。
 */
public record AccountRegistered(UserId userId, String email, Instant occurredAt) implements DomainEvent {
}
