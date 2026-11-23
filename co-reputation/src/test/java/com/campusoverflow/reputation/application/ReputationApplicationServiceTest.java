package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.application.view.ReputationView;
import com.campusoverflow.reputation.domain.model.ReputationAccount;
import com.campusoverflow.reputation.domain.model.ReputationEntry;
import com.campusoverflow.reputation.domain.model.ReputationScore;
import com.campusoverflow.reputation.domain.model.RuleCode;
import com.campusoverflow.reputation.domain.repository.ReputationAccountRepository;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 声誉应用服务单元测试：验证事件幂等、最小分保护与查询视图。
 */
@ExtendWith(MockitoExtension.class)
class ReputationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");
    private static final UserId USER = UserId.newId();

    @Mock
    private ReputationAccountRepository repository;

    private ReputationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ReputationApplicationService(repository);
    }

    @Test
    @DisplayName("首次计分：1 + 2 = 3，并用 ArgumentCaptor 验证保存的聚合")
    void applyIncrementsScore() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());
        when(repository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.apply(USER.asString(), "QP:1", RuleCode.QUESTION_POSTED, NOW);

        ArgumentCaptor<ReputationAccount> captor = ArgumentCaptor.forClass(ReputationAccount.class);
        verify(repository).save(captor.capture());
        ReputationAccount saved = captor.getValue();
        assertEquals(3, saved.score().value());
        assertTrue(saved.entries().containsKey("QP:1"));
    }

    @Test
    @DisplayName("INV-R-02：重复 eventId 不再保存（幂等）")
    void duplicateEventIsIgnored() {
        ReputationAccount account = ReputationAccount.open(USER);
        account.apply("QP:1", RuleCode.QUESTION_POSTED, NOW);
        when(repository.findByUserId(USER)).thenReturn(Optional.of(account));

        service.apply(USER.asString(), "QP:1", RuleCode.QUESTION_POSTED, NOW.plusSeconds(10));

        verify(repository, never()).save(any(ReputationAccount.class));
    }

    @Test
    @DisplayName("INV-R-01：负向增量不会把分数打到 1 以下")
    void negativeDeltaKeepsMinimum() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());
        when(repository.save(any(ReputationAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.applyDelta(USER.asString(), "VC:-5", RuleCode.DOWNVOTE_RECEIVED, -5, NOW);

        ArgumentCaptor<ReputationAccount> captor = ArgumentCaptor.forClass(ReputationAccount.class);
        verify(repository).save(captor.capture());
        assertEquals(1, captor.getValue().score().value());
    }

    @Test
    @DisplayName("查询：没有记录时返回初始 1 分与空明细")
    void viewReturnsInitialWhenAbsent() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());

        ReputationView view = service.view(USER.asString());

        assertEquals(1, view.score());
        assertTrue(view.entries().isEmpty());
    }

    @Test
    @DisplayName("查询：明细按时间倒序返回")
    void viewSortsEntriesDescending() {
        Map<String, ReputationEntry> entries = Map.of(
                "QP:1", new ReputationEntry("QP:1", RuleCode.QUESTION_POSTED, 2, NOW),
                "AA:1", new ReputationEntry("AA:1", RuleCode.ANSWER_ACCEPTED, 15, NOW.plusSeconds(60)));
        ReputationAccount account = ReputationAccount.rehydrate(USER,
                new ReputationScore(18), entries);
        when(repository.findByUserId(USER)).thenReturn(Optional.of(account));

        ReputationView view = service.view(USER.asString());

        assertEquals(18, view.score());
        assertEquals("ANSWER_ACCEPTED", view.entries().get(0).ruleCode());
        assertEquals(15, view.entries().get(0).delta());
    }

    @Test
    @DisplayName("scoreOf 返回聚合分值，缺省为 1")
    void scoreOfFallsBackToInitial() {
        when(repository.findByUserId(USER)).thenReturn(Optional.empty());
        assertEquals(1, service.scoreOf(USER.asString()).value());
    }
}
