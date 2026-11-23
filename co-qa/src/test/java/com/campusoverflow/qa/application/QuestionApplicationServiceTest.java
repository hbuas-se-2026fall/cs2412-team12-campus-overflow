package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.AcceptAnswerCommand;
import com.campusoverflow.qa.application.command.CreateQuestionCommand;
import com.campusoverflow.qa.application.command.SubmitAnswerCommand;
import com.campusoverflow.qa.application.view.QuestionPage;
import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.AnswerInvariants;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.QuestionInvariants;
import com.campusoverflow.qa.domain.model.TagSet;
import com.campusoverflow.qa.domain.model.Title;
import com.campusoverflow.qa.domain.port.AskerVerifier;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import com.campusoverflow.shared.tag.Tag;
import com.campusoverflow.shared.tag.TagVocabulary;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 问答应用服务单元测试：覆盖发布、列表（含标签与分页夹紧）、回答、采纳（含改判留痕）。
 */
@ExtendWith(MockitoExtension.class)
class QuestionApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final UserId ASKER = UserId.newId();
    private static final UserId AUTHOR = UserId.newId();
    private static final UserId OTHER = UserId.newId();

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AskerVerifier askerVerifier;

    @Mock
    private TagVocabulary tagVocabulary;

    @Mock
    private DomainEventPublisher eventPublisher;

    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new QuestionApplicationService(questionRepository, answerRepository, askerVerifier,
                tagVocabulary, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("发布问题：登记标签、保存聚合，并发布 QuestionPosted")
    void createQuestion() {
        when(tagVocabulary.registerIfAbsent(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Question question = service.create(new CreateQuestionCommand(ASKER.asString(),
                "发布问题服务测试标题", "这是发布问题测试的正文内容。", List.of("Java")));

        assertEquals(ASKER, question.askerId());
        assertEquals("java", question.tags().values().get(0).value());
        verify(askerVerifier).ensureCanPublish(ASKER);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        QuestionPosted event = (QuestionPosted) captor.getValue().get(0);
        assertEquals("java", event.tagsCsv());
    }

    @Test
    @DisplayName("列表：按标签查询并返回总数")
    void listByTag() {
        Question question = sampleQuestion();
        when(questionRepository.findPageByTag("java", 0, 20)).thenReturn(List.of(question));
        when(questionRepository.countByTag("java")).thenReturn(1L);

        QuestionPage page = service.list(0, 20, "java");

        assertEquals(1, page.items().size());
        assertEquals(1L, page.total());
        assertFalse(page.hasNext());
    }

    @Test
    @DisplayName("列表：分页参数被夹紧（负数页码→0，超大 size→100）")
    void listClampsPaging() {
        when(questionRepository.findPage(0, 100)).thenReturn(List.of());

        QuestionPage page = service.list(-5, 1000, null);

        assertEquals(0, page.page());
        assertEquals(100, page.size());
        assertEquals(0L, page.total());
        verify(questionRepository).findPage(0, 100);
    }

    @Test
    @DisplayName("提交回答：保存并发布 AnswerSubmitted")
    void submitAnswer() {
        Question question = sampleQuestion();
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(answerRepository.findLatestByQuestionAndAuthor(question.id(), AUTHOR)).thenReturn(Optional.empty());
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Answer answer = service.submitAnswer(new SubmitAnswerCommand(
                question.id().asString(), AUTHOR.asString(), "这是提交回答测试的正文。"));

        assertEquals(AUTHOR, answer.authorId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        AnswerSubmitted event = (AnswerSubmitted) captor.getValue().get(0);
        assertEquals(answer.id(), event.answerId());
    }

    @Test
    @DisplayName("INV-A-03：60 秒内重复回答被拒绝")
    void submitAnswerRateLimited() {
        Question question = sampleQuestion();
        Answer recent = Answer.submit(AnswerId.newId(), question.id(), AUTHOR,
                Body.ofAnswer("刚刚提交过的回答。"), NOW.minusSeconds(5));
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(answerRepository.findLatestByQuestionAndAuthor(question.id(), AUTHOR)).thenReturn(Optional.of(recent));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class, () -> service.submitAnswer(
                new SubmitAnswerCommand(question.id().asString(), AUTHOR.asString(), "再来一条回答。")));

        assertEquals(AnswerInvariants.SUBMIT_TOO_FREQUENT, ex.code());
    }

    @Test
    @DisplayName("INV-A-02：已下架的问题不能再回答")
    void submitAnswerOnHiddenQuestion() {
        Question question = sampleQuestion();
        question.hide("测试下架", NOW);
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class, () -> service.submitAnswer(
                new SubmitAnswerCommand(question.id().asString(), AUTHOR.asString(), "回答已下架的问题。")));

        assertEquals(AnswerInvariants.QUESTION_MUST_BE_VISIBLE, ex.code());
    }

    @Test
    @DisplayName("采纳：事件带新作者，首次采纳无上一位作者")
    void acceptAnswer() {
        Question question = sampleQuestion();
        question.pullDomainEvents(); // 清掉 QuestionPosted，只验证采纳事件
        Answer answer = Answer.submit(AnswerId.newId(), question.id(), AUTHOR,
                Body.ofAnswer("会被采纳的回答正文。"), NOW.minusSeconds(300));
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(answerRepository.findById(answer.id())).thenReturn(Optional.of(answer));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.acceptAnswer(new AcceptAnswerCommand(question.id().asString(), answer.id().asString(),
                ASKER.asString()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        AnswerAccepted event = (AnswerAccepted) captor.getValue().get(0);
        assertEquals(AUTHOR, event.acceptedAnswerAuthorId());
        assertNull(event.previousAnswerAuthorId());
    }

    @Test
    @DisplayName("改判：事件带上一位被采纳者的作者 ID，供声誉冲正")
    void acceptAnswerRecordsPreviousAuthor() {
        Question question = sampleQuestion();
        AnswerId previousId = AnswerId.newId();
        Answer previous = Answer.submit(previousId, question.id(), OTHER,
                Body.ofAnswer("上一条被采纳的回答。"), NOW.minusSeconds(900));
        Answer current = Answer.submit(AnswerId.newId(), question.id(), AUTHOR,
                Body.ofAnswer("新提交的回答正文。"), NOW.minusSeconds(600));
        question.acceptAnswer(previousId, OTHER, null, ASKER, NOW.minusSeconds(500));
        question.pullDomainEvents();

        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(answerRepository.findById(previousId)).thenReturn(Optional.of(previous));
        when(answerRepository.findById(current.id())).thenReturn(Optional.of(current));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(Answer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.acceptAnswer(new AcceptAnswerCommand(question.id().asString(), current.id().asString(),
                ASKER.asString()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        AnswerAccepted event = (AnswerAccepted) captor.getValue().get(0);
        assertEquals(AUTHOR, event.acceptedAnswerAuthorId());
        assertEquals(OTHER, event.previousAnswerAuthorId());
    }

    @Test
    @DisplayName("采纳别人的问题里的回答会被拒绝（INV-Q-03）")
    void acceptByNonAskerRejected() {
        Question question = sampleQuestion();
        Answer answer = Answer.submit(AnswerId.newId(), question.id(), AUTHOR,
                Body.ofAnswer("回答正文内容。"), NOW.minusSeconds(300));
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(answerRepository.findById(answer.id())).thenReturn(Optional.of(answer));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class, () -> service.acceptAnswer(
                new AcceptAnswerCommand(question.id().asString(), answer.id().asString(), OTHER.asString())));

        assertEquals(QuestionInvariants.ACCEPT_ONLY_ASKER, ex.code());
    }

    private static Question sampleQuestion() {
        return Question.post(QuestionId.newId(), ASKER,
                Title.of("问答服务测试用的问题标题"), Body.ofQuestion("这是问答服务测试的正文内容。"),
                TagSet.empty(), NOW.minusSeconds(3600));
    }
}
