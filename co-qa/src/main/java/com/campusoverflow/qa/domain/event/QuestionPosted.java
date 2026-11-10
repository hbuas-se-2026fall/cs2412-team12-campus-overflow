package com.campusoverflow.qa.domain.event;

import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;

/**
 * 问题已发布（{@code QuestionPosted}）。
 *
 * <p>{@code tagsCsv} 用逗号分隔传给下游（发现上下文建索引、声誉上下文 +2），避免事件里再带一个上下文的值对象。</p>
 */
public record QuestionPosted(QuestionId questionId, UserId askerId, String title, String tagsCsv,
                             Instant occurredAt) implements DomainEvent {
}
