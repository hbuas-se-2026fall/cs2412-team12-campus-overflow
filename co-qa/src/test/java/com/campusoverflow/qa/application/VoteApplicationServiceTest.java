package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.CastVoteCommand;
import com.campusoverflow.qa.application.view.VoteResult;
import com.campusoverflow.qa.domain.event.VoteCast;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.TagSet;
import com.campusoverflow.qa.domain.model.Title;
import com.campusoverflow.qa.domain.model.Vote;
import com.campusoverflow.qa.domain.model.VoteInvariants;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.qa.domain.repository.VoteRepository;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投票应用服务单元测试（US-07）。
 *
 * <p>重点：用 {@link ArgumentCaptor} 验证"发出 VoteCast 事件"这一副作用，
 * 以及票数投影（question.voteCount）与事件里的声誉增量是否一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class VoteApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final UserId ASKER = UserId.newId();
    private static final UserId VOTER = UserId.newId();

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private VoteApplicationService service;
    private Question question;

    @BeforeEach
    void setUp() {
        service = new VoteApplicationService(questionRepository, answerRepository, voteRepository,
                eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
        question = Question.post(QuestionId.newId(), ASKER,
                Title.of("投票服务测试用的问题标题"), Body.ofQuestion("这是投票测试用的正文。"),
                TagSet.empty(), NOW.minusSeconds(3600));
    }

    @Test
    @DisplayName("首次赞成：票数 0→1，事件声誉增量 +10，目标作者是提问者")
    void castUpVote() {
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(voteRepository.find(any(ContentTarget.class), any(UserId.class))).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoteResult result = service.cast(
                new CastVoteCommand("question", question.id().asString(), VOTER.asString(), "up"));

        assertEquals(1, result.targetVoteCount());
        assertEquals(1, question.voteCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        VoteCast event = (VoteCast) captor.getValue().get(0);
        assertEquals(10, event.reputationDelta());
        assertEquals(ASKER, event.targetAuthorId());
        assertEquals(VOTER, event.voterId());
    }

    @Test
    @DisplayName("改票：赞成→反对，票数净变化 −2，声誉增量 −12")
    void reviseVote() {
        Vote existing = Vote.cast(ContentTarget.question(question.id()), VOTER, ASKER,
                VoteValue.UP, NOW.minusSeconds(60));
        question.applyVoteProjection(1, NOW.minusSeconds(60));
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(voteRepository.find(any(ContentTarget.class), any(UserId.class))).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoteResult result = service.cast(
                new CastVoteCommand("question", question.id().asString(), VOTER.asString(), "down"));

        assertEquals(-1, result.targetVoteCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals(-12, ((VoteCast) captor.getValue().get(0)).reputationDelta());
    }

    @Test
    @DisplayName("撤票：票数回到 0，删除投票记录，声誉增量 −10")
    void retractVote() {
        Vote existing = Vote.cast(ContentTarget.question(question.id()), VOTER, ASKER,
                VoteValue.UP, NOW.minusSeconds(60));
        question.applyVoteProjection(1, NOW.minusSeconds(60));
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(voteRepository.find(any(ContentTarget.class), any(UserId.class))).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.retract(new CastVoteCommand("question", question.id().asString(), VOTER.asString(), null));

        assertEquals(0, question.voteCount());
        verify(voteRepository).delete(existing);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals(-10, ((VoteCast) captor.getValue().get(0)).reputationDelta());
    }

    @Test
    @DisplayName("INV-V-02：不能给自己的问题投票")
    void rejectsSelfVote() {
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(voteRepository.find(any(ContentTarget.class), any(UserId.class))).thenReturn(Optional.empty());

        InvariantViolationException ex = assertThrows(InvariantViolationException.class, () -> service.cast(
                new CastVoteCommand("question", question.id().asString(), ASKER.asString(), "up")));

        assertEquals(VoteInvariants.SELF_VOTE, ex.code());
    }

    @Test
    @DisplayName("对回答投票：走 AnswerRepository 并更新回答的票数投影")
    void castVoteOnAnswer() {
        Answer answer = Answer.submit(AnswerId.newId(), question.id(), ASKER,
                Body.ofAnswer("这是被投票的回答正文。"), NOW.minusSeconds(600));
        when(answerRepository.findById(answer.id())).thenReturn(Optional.of(answer));
        when(voteRepository.find(any(ContentTarget.class), any(UserId.class))).thenReturn(Optional.empty());
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoteResult result = service.cast(
                new CastVoteCommand("answer", answer.id().asString(), VOTER.asString(), "up"));

        assertEquals(1, result.targetVoteCount());
        assertEquals(1, answer.voteCount());
        verify(answerRepository).save(answer);
    }
}
