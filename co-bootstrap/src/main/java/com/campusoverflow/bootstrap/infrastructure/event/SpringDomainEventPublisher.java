package com.campusoverflow.bootstrap.infrastructure.event;

import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * 领域事件投递实现：先写结构化日志（可被采集），再交给 Spring 事件总线。
 *
 * <p>Sprint 3 接入消息队列时，只替换这个类，应用服务与聚合不动。</p>
 */
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher springPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publish(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (DomainEvent event : events) {
            log.info("domain event published: name={}, occurredAt={}", event.eventName(), event.occurredAt());
            springPublisher.publishEvent(event);
        }
    }
}
