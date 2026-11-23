package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 评论已发表（US-09）。
 *
 * <p>Lab 8 起用于驱动监控指标；后续也可用于"被评论"通知（US-14）。</p>
 */
public record CommentPosted(CommentId commentId, ContentTarget target, UserId authorId, Instant occurredAt)
        implements DomainEvent {

    @Override
    public String eventName() {
        return "CommentPosted";
    }
}
