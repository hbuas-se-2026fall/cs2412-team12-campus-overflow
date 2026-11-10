package com.campusoverflow.qa.domain.model;

import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Answer 聚合行为测试：覆盖 INV-A-01 / INV-A-04 与"仅作者可操作"。
 */
class AnswerTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    private static final UserId AUTHOR = UserId.newId();
    private static final UserId OTHER = UserId.newId();
    private static final QuestionId QUESTION_ID = QuestionId.newId();

    @Test
    @DisplayName("提交回答：状态 normal、未采纳，并产生 AnswerSubmitted 事件")
    void submitCreatesAnswer() {
        Answer answer = sampleAnswer();

        assertEquals(AnswerStatus.NORMAL, answer.status());
        assertFalse(answer.isAccepted());
        assertInstanceOf(AnswerSubmitted.class, answer.pullDomainEvents().get(0));
    }

    @Test
    @DisplayName("INV-A-01：空白正文被拒绝")
    void rejectsBlankBody() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> Body.ofAnswer("   "));
        assertEquals(AnswerInvariants.BODY_LENGTH, ex.code());
    }

    @Test
    @DisplayName("INV-A-01：正文超过 10000 字被拒绝")
    void rejectsTooLongBody() {
        String tooLong = "x".repeat(Body.ANSWER_MAX_LENGTH + 1);
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> Body.ofAnswer(tooLong));
        assertEquals(AnswerInvariants.BODY_LENGTH, ex.code());
    }

    @Test
    @DisplayName("非作者不能编辑回答")
    void rejectsEditByNonAuthor() {
        Answer answer = sampleAnswer();
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> answer.edit(Body.ofAnswer("我帮你改一下"), OTHER, NOW.plusSeconds(60)));
        assertEquals(AnswerInvariants.ONLY_AUTHOR, ex.code());
    }

    @Test
    @DisplayName("INV-A-04：被采纳的回答不能删除，但还能编辑")
    void acceptedAnswerCannotBeDeleted() {
        Answer answer = sampleAnswer();
        answer.markAccepted(NOW.plusSeconds(60));

        assertThrows(InvariantViolationException.class, () -> answer.assertDeletable(AUTHOR));

        answer.edit(Body.ofAnswer("补充一个可复现的最小示例"), AUTHOR, NOW.plusSeconds(120));
        assertEquals("补充一个可复现的最小示例", answer.body().value());
    }

    @Test
    @DisplayName("未被采纳的回答可以删除")
    void normalAnswerCanBeDeleted() {
        Answer answer = sampleAnswer();
        answer.assertDeletable(AUTHOR); // 不抛异常即通过
        assertTrue(answer.status().isVisible());
    }

    @Test
    @DisplayName("已下架的回答不能再编辑")
    void hiddenAnswerCannotBeEdited() {
        Answer answer = sampleAnswer();
        answer.hide("广告", NOW.plusSeconds(60));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> answer.edit(Body.ofAnswer("再改一次"), AUTHOR, NOW.plusSeconds(120)));
        assertEquals(QuestionInvariants.HIDDEN_IMMUTABLE, ex.code());
    }

    private static Answer sampleAnswer() {
        return Answer.submit(AnswerId.newId(), QUESTION_ID, AUTHOR,
                Body.ofAnswer("检查一下 application.yml 里 flyway 是否在 jpa 之前初始化。"), NOW);
    }
}
