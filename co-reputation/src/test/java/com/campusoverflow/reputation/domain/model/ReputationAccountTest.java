package com.campusoverflow.reputation.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReputationAccount 骨架测试：INV-R-01（不低于 1 分）与 INV-R-02（eventId 幂等）。
 */
class ReputationAccountTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    private static final UserId USER = UserId.newId();

    @Test
    @DisplayName("新账户初始 1 分（INV-R-01）")
    void initialScoreIsOne() {
        ReputationAccount account = ReputationAccount.open(USER);
        assertEquals(1, account.score().value());
    }

    @Test
    @DisplayName("被采纳 +15，明细可追溯")
    void acceptedAnswerAddsFifteen() {
        ReputationAccount account = ReputationAccount.open(USER);

        assertTrue(account.apply(1001L, RuleCode.ANSWER_ACCEPTED, NOW));

        assertEquals(16, account.score().value());
        assertEquals(RuleCode.ANSWER_ACCEPTED, account.entries().get(1001L).ruleCode());
    }

    @Test
    @DisplayName("同一 eventId 重复投递只计一次（INV-R-02）")
    void duplicateEventIsIgnored() {
        ReputationAccount account = ReputationAccount.open(USER);
        account.apply(2001L, RuleCode.QUESTION_POSTED, NOW);

        assertFalse(account.apply(2001L, RuleCode.QUESTION_POSTED, NOW.plusSeconds(10)));

        assertEquals(3, account.score().value(), "1 + 2，只加一次");
        assertEquals(1, account.entries().size());
    }

    @Test
    @DisplayName("扣分不会把分数打到 1 以下（INV-R-01）")
    void scoreNeverDropsBelowOne() {
        ReputationAccount account = ReputationAccount.open(USER);

        for (int i = 0; i < 5; i++) {
            account.apply(3000L + i, RuleCode.DOWNVOTE_RECEIVED, NOW);
        }

        assertEquals(1, account.score().value());
    }

    @Test
    @DisplayName("直接构造小于 1 的分数会被拒绝")
    void rejectsOutOfRangeScore() {
        assertThrows(InvariantViolationException.class, () -> new ReputationScore(0));
    }
}
