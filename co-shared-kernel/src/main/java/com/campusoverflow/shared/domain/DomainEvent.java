package com.campusoverflow.shared.domain;

import java.time.Instant;

/**
 * 领域事件：过去时命名、不可变的业务事实（统一语言术语 #40）。
 *
 * <p>事件只描述"已经发生了什么"，不包含"应该做什么"；投递与幂等由应用层/基础设施负责。</p>
 */
public interface DomainEvent {

    /** 事件发生时刻（业务时间，不是投递时间）。 */
    Instant occurredAt();

    /** 事件名，默认取类名，如 {@code QuestionPosted}。 */
    default String eventName() {
        return getClass().getSimpleName();
    }
}
