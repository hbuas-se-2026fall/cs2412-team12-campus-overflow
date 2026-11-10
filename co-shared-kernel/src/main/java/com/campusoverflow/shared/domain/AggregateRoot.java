package com.campusoverflow.shared.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根基类：统一收集聚合内部产生的领域事件。
 *
 * <p>聚合只在内部状态真正改变时登记事件；应用服务在事务提交前调用 {@link #pullDomainEvents()}
 * 取出并投递，避免"命令失败却发出事件"。</p>
 *
 * <p>注意：这里刻意不引入 JPA / Spring 注解，共享内核保持框架无关，方便单元测试。</p>
 */
public abstract class AggregateRoot {

    /** transient：即使将来被序列化也不会把事件清单写出去。 */
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /** 取出并清空事件（投递方负责保证只投递一次）。 */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> snapshot = List.copyOf(domainEvents);
        domainEvents.clear();
        return snapshot;
    }

    public boolean hasDomainEvents() {
        return !domainEvents.isEmpty();
    }
}
