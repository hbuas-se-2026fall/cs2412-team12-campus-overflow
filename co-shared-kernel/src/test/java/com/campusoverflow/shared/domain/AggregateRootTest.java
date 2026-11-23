package com.campusoverflow.shared.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聚合根基类与领域异常基类的单元测试。
 */
class AggregateRootTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");

    @Test
    @DisplayName("登记事件后可取出一次，取完清空")
    void registersAndPullsEvents() {
        TestAggregate aggregate = new TestAggregate();

        aggregate.raise();

        assertTrue(aggregate.hasDomainEvents());
        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertEquals(1, events.size());
        assertEquals("TestHappened", events.get(0).eventName());
        assertEquals(NOW, events.get(0).occurredAt());
        assertFalse(aggregate.hasDomainEvents());
    }

    @Test
    @DisplayName("重复 pull 返回空列表，不会重复投递")
    void secondPullIsEmpty() {
        TestAggregate aggregate = new TestAggregate();
        aggregate.raise();
        aggregate.pullDomainEvents();

        assertTrue(aggregate.pullDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("登记 null 事件被忽略")
    void ignoresNullEvent() {
        TestAggregate aggregate = new TestAggregate();
        aggregate.raiseNull();

        assertFalse(aggregate.hasDomainEvents());
    }

    @Test
    @DisplayName("领域异常保留错误码")
    void domainExceptionKeepsCode() {
        InvariantViolationException ex = new InvariantViolationException("INV-T-01", "测试");
        assertEquals("INV-T-01", ex.code());
        assertEquals("INV-T-01", ex.invariantCode());
        assertEquals("测试", ex.getMessage());

        DomainException plain = new DomainException("BUSINESS", "业务失败", new IllegalStateException("cause"));
        assertEquals("BUSINESS", plain.code());
        assertSame(IllegalStateException.class, plain.getCause().getClass());
    }

    @Test
    @DisplayName("EntityNotFoundException 带聚合名与 ID")
    void entityNotFoundMessage() {
        EntityNotFoundException ex = EntityNotFoundException.of("问题", "abc");
        assertEquals(EntityNotFoundException.CODE, ex.code());
        assertTrue(ex.getMessage().contains("问题"));
        assertTrue(ex.getMessage().contains("abc"));
    }

    private static final class TestHappened implements DomainEvent {
        @Override
        public Instant occurredAt() {
            return NOW;
        }
    }

    private static final class TestAggregate extends AggregateRoot {
        void raise() {
            registerEvent(new TestHappened());
        }

        void raiseNull() {
            registerEvent(null);
        }
    }
}
