package com.campusoverflow.qa.domain.model;

import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.QuestionEdited;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import com.campusoverflow.shared.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Question 聚合行为测试：逐条覆盖 Lab 3 AGG-02 的不变式 INV-Q-01 ~ INV-Q-05。
 */
class QuestionTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    private static final UserId ASKER = UserId.newId();
    private static final UserId OTHER = UserId.newId();

    @Test
    @DisplayName("发布问题：初始 open、票数 0、无采纳，并产生 QuestionPosted 事件")
    void postCreatesOpenQuestion() {
        Question question = sampleQuestion();

        assertEquals(QuestionStatus.OPEN, question.status());
        assertEquals(0, question.voteCount());
        assertTrue(question.acceptedAnswerId().isEmpty());

        List<DomainEvent> events = question.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(QuestionPosted.class, events.get(0));
    }

    @Test
    @DisplayName("INV-Q-01：标题不足 5 字被拒绝")
    void rejectsShortTitle() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> Title.of("短标题"));
        assertEquals(QuestionInvariants.TITLE_LENGTH, ex.code());
    }

    @Test
    @DisplayName("INV-Q-02：超过 5 个标签被拒绝")
    void rejectsTooManyTags() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> new TagSet(List.of(
                        Tag.of("java"), Tag.of("mysql"), Tag.of("maven"),
                        Tag.of("junit"), Tag.of("docker"), Tag.of("git"))));
        assertEquals(QuestionInvariants.TAG_SET, ex.code());
    }

    @Test
    @DisplayName("INV-Q-03：非提问者采纳答案被拒绝")
    void rejectsAcceptanceByNonAsker() {
        Question question = sampleQuestion();
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> question.acceptAnswer(AnswerId.newId(), OTHER, null, OTHER, NOW.plusSeconds(60)));
        assertEquals(QuestionInvariants.ACCEPT_ONLY_ASKER, ex.code());
    }

    @Test
    @DisplayName("INV-Q-04：改判记录上一条被采纳的答案，供声誉冲正")
    void recordsPreviousAcceptedAnswerOnChange() {
        Question question = sampleQuestion();
        AnswerId first = AnswerId.newId();
        AnswerId second = AnswerId.newId();

        question.acceptAnswer(first, ASKER, null, ASKER, NOW.plusSeconds(60));
        question.pullDomainEvents();
        question.acceptAnswer(second, ASKER, ASKER, ASKER, NOW.plusSeconds(120));

        List<DomainEvent> events = question.pullDomainEvents();
        assertEquals(1, events.size());
        AnswerAccepted event = assertInstanceOf(AnswerAccepted.class, events.get(0));
        assertEquals(second, event.acceptedAnswerId());
        assertEquals(first.value(), event.previousAcceptedAnswerId().value());
        assertEquals(ASKER, event.previousAnswerAuthorId(), "改判要带出上一位作者，信誉上下文才能冲正");
        assertEquals(QuestionStatus.SOLVED, question.status());
    }

    @Test
    @DisplayName("INV-Q-04：重复采纳同一条答案幂等，不产生事件")
    void acceptingSameAnswerIsIdempotent() {
        Question question = sampleQuestion();
        AnswerId answerId = AnswerId.newId();
        question.acceptAnswer(answerId, ASKER, null, ASKER, NOW.plusSeconds(60));
        question.pullDomainEvents();

        boolean changed = question.acceptAnswer(answerId, ASKER, null, ASKER, NOW.plusSeconds(120));

        assertFalse(changed);
        assertTrue(question.pullDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("INV-Q-05：已下架的问题不能再被采纳、投票或编辑")
    void hiddenQuestionRejectsWrites() {
        Question question = sampleQuestion();
        question.hide("包含攻击性内容", NOW.plusSeconds(60));

        assertEquals(QuestionInvariants.HIDDEN_IMMUTABLE,
                assertThrows(InvariantViolationException.class,
                        () -> question.acceptAnswer(AnswerId.newId(), ASKER, null, ASKER, NOW.plusSeconds(120))).code());
        assertEquals(QuestionInvariants.HIDDEN_IMMUTABLE,
                assertThrows(InvariantViolationException.class,
                        () -> question.applyVoteProjection(1, NOW.plusSeconds(120))).code());
        assertEquals(QuestionInvariants.HIDDEN_IMMUTABLE,
                assertThrows(InvariantViolationException.class,
                        () -> question.edit(Title.of("新的标题内容"), Body.ofQuestion("新正文"),
                                TagSet.empty(), ASKER, NOW.plusSeconds(120))).code());
    }

    @Test
    @DisplayName("编辑：非提问者被拒绝，提问者编辑产生 QuestionEdited 事件")
    void onlyAskerCanEdit() {
        Question question = sampleQuestion();

        assertEquals(QuestionInvariants.EDIT_ONLY_ASKER,
                assertThrows(InvariantViolationException.class,
                        () -> question.edit(Title.of("别人改的标题"), Body.ofQuestion("别人改的正文"),
                                TagSet.empty(), OTHER, NOW.plusSeconds(60))).code());

        question.pullDomainEvents();
        question.edit(Title.of("提问者自己修改后的标题"), Body.ofQuestion("修改后的正文"),
                TagSet.empty(), ASKER, NOW.plusSeconds(120));

        List<DomainEvent> events = question.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(QuestionEdited.class, events.get(0));
    }

    @Test
    @DisplayName("票数投影：赞成 +1、反对 −1 可以累加，并允许为负")
    void voteProjectionAccumulates() {
        Question question = sampleQuestion();

        question.applyVoteProjection(1, NOW.plusSeconds(10));
        question.applyVoteProjection(2, NOW.plusSeconds(20));
        question.applyVoteProjection(-1, NOW.plusSeconds(30));
        question.applyVoteProjection(-2, NOW.plusSeconds(40));

        assertEquals(0, question.voteCount());
    }

    @Test
    @DisplayName("发布事件里带出提问者、标题与标签，下游投影无需回查聚合")
    void postedEventCarriesPayload() {
        Question question = Question.post(QuestionId.newId(), ASKER,
                Title.of("Flyway 和 JPA 谁先执行？"),
                Body.ofQuestion("启动时表还没建，Hibernate 先跑了"),
                new TagSet(List.of(Tag.of("flyway"), Tag.of("spring-data-jpa"))),
                NOW);

        QuestionPosted event = (QuestionPosted) question.pullDomainEvents().get(0);
        assertEquals(ASKER, event.askerId());
        assertEquals("flyway,spring-data-jpa", event.tagsCsv());
        assertSame(question.id(), event.questionId());
    }

    private static Question sampleQuestion() {
        return Question.post(QuestionId.newId(), ASKER,
                Title.of("如何在 Spring Boot 3 中配置多数据源？"),
                Body.ofQuestion("我想把读写库分开，配置文件该怎么写？"),
                TagSet.empty(), NOW);
    }
}
