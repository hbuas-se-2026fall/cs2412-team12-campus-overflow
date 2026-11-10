package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Vote 值对象测试：INV-V-02（不能自投）与 INV-V-03（改票净变化 2）。
 */
class VoteTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    private static final UserId AUTHOR = UserId.newId();
    private static final UserId VOTER = UserId.newId();
    private static final VoteTarget TARGET = VoteTarget.question(QuestionId.newId());

    @Test
    @DisplayName("INV-V-02：不能给自己的内容投票")
    void rejectsSelfVote() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> Vote.cast(TARGET, AUTHOR, AUTHOR, VoteValue.UP, NOW));
        assertEquals(VoteInvariants.SELF_VOTE, ex.code());
    }

    @Test
    @DisplayName("INV-V-03：赞成改反对，票数净变化 −2")
    void reviseUpToDownHasDeltaMinusTwo() {
        Vote up = Vote.cast(TARGET, VOTER, AUTHOR, VoteValue.UP, NOW);

        Vote down = up.revise(VoteValue.DOWN, NOW.plusSeconds(30));

        assertNotSame(up, down);
        assertEquals(VoteValue.DOWN, down.value());
        assertEquals(-2, Vote.netDelta(up.value(), down.value()));
    }

    @Test
    @DisplayName("INV-V-03：反对改赞成，票数净变化 +2")
    void reviseDownToUpHasDeltaPlusTwo() {
        Vote down = Vote.cast(TARGET, VOTER, AUTHOR, VoteValue.DOWN, NOW);

        Vote up = down.revise(VoteValue.UP, NOW.plusSeconds(30));

        assertEquals(2, Vote.netDelta(down.value(), up.value()));
    }

    @Test
    @DisplayName("重复投同一票是幂等的（不产生新对象）")
    void revisingToSameValueIsNoop() {
        Vote up = Vote.cast(TARGET, VOTER, AUTHOR, VoteValue.UP, NOW);

        assertSame(up, up.revise(VoteValue.UP, NOW.plusSeconds(30)));
        assertEquals(0, Vote.netDelta(up.value(), up.value()));
    }

    @Test
    @DisplayName("投票目标区分问题与回答，避免 ID 撞车")
    void targetCarriesType() {
        QuestionId questionId = QuestionId.newId();
        assertEquals(TargetType.QUESTION, VoteTarget.question(questionId).type());
        assertEquals(TargetType.ANSWER, VoteTarget.answer(AnswerId.newId()).type());
    }
}
