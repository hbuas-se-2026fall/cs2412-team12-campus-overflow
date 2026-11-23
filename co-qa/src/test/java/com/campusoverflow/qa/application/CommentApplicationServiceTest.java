package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.PostCommentCommand;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.TagSet;
import com.campusoverflow.qa.domain.model.Title;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.CommentRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.shared.domain.EntityNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评论应用服务单元测试（US-09）：目标存在性、作者删除守卫。
 */
@ExtendWith(MockitoExtension.class)
class CommentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final UserId AUTHOR = UserId.newId();
    private static final UserId OTHER = UserId.newId();

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private CommentRepository commentRepository;

    private CommentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CommentApplicationService(questionRepository, answerRepository, commentRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("对问题发表评论：保存并返回评论内容")
    void postCommentOnQuestion() {
        Question question = sampleQuestion();
        when(questionRepository.findById(question.id())).thenReturn(Optional.of(question));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = service.post(new PostCommentCommand("question", question.id().asString(),
                AUTHOR.asString(), "  补充一条说明。  "));

        assertEquals("补充一条说明。", comment.body());
        assertEquals("question", comment.target().type().code());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals(AUTHOR, captor.getValue().authorId());
    }

    @Test
    @DisplayName("对回答发表评论：目标换成回答")
    void postCommentOnAnswer() {
        Answer answer = Answer.submit(AnswerId.newId(), QuestionId.newId(), OTHER,
                Body.ofAnswer("回答正文内容。"), NOW.minusSeconds(600));
        when(answerRepository.findById(answer.id())).thenReturn(Optional.of(answer));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = service.post(new PostCommentCommand("answer", answer.id().asString(),
                AUTHOR.asString(), "对回答的评论。"));

        assertEquals("answer", comment.target().type().code());
        assertEquals(answer.id().asString(), comment.target().targetId());
    }

    @Test
    @DisplayName("目标不存在时抛 EntityNotFoundException")
    void postCommentOnMissingTarget() {
        QuestionId id = QuestionId.newId();
        when(questionRepository.findById(id)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.post(
                new PostCommentCommand("question", id.asString(), AUTHOR.asString(), "评论不存在的目标。")));

        assertEquals("NOT_FOUND", ex.code());
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("列表：转发到仓储查询")
    void listComments() {
        ContentTarget target = ContentTarget.question(QuestionId.newId());
        when(commentRepository.findByTarget(any(ContentTarget.class))).thenReturn(List.of());

        assertTrue(service.list("question", target.targetId()).isEmpty());
    }

    @Test
    @DisplayName("INV-C-03：非作者删除评论被拒绝")
    void deleteByNonAuthorRejected() {
        Comment comment = Comment.post(CommentId.newId(), ContentTarget.question(QuestionId.newId()),
                AUTHOR, "作者写的评论。", NOW);
        when(commentRepository.findById(comment.id())).thenReturn(Optional.of(comment));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> service.delete(comment.id().asString(), OTHER.asString()));

        assertEquals("INV-C-03", ex.code());
        verify(commentRepository, never()).delete(any(CommentId.class));
    }

    @Test
    @DisplayName("作者删除评论成功")
    void deleteByAuthor() {
        Comment comment = Comment.post(CommentId.newId(), ContentTarget.question(QuestionId.newId()),
                AUTHOR, "作者写的评论。", NOW);
        when(commentRepository.findById(comment.id())).thenReturn(Optional.of(comment));

        service.delete(comment.id().asString(), AUTHOR.asString());

        verify(commentRepository).delete(comment.id());
    }

    private static Question sampleQuestion() {
        return Question.post(QuestionId.newId(), OTHER,
                Title.of("评论服务测试用的问题标题"), Body.ofQuestion("评论服务测试正文。"),
                TagSet.empty(), NOW.minusSeconds(3600));
    }
}
