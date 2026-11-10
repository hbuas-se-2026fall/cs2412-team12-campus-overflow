package com.campusoverflow.shared.domain;

import java.util.List;

/**
 * 领域事件投递端口：应用服务在事务提交前把聚合里的事件交出去，
 * 具体是进程内 Spring 事件、消息队列还是日志，由装配层决定。
 */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
