package com.campusoverflow.bootstrap.monitoring;

import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.qa.domain.event.CommentPosted;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.qa.domain.event.VoteCast;
import com.campusoverflow.qa.domain.model.AcceptedAnswerId;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.shared.identity.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 指标监听器单元测试：用 SimpleMeterRegistry 验证计数与标签。
 */
class QaMetricsListenerTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");

    private SimpleMeterRegistry registry;
    private QaMetricsListener listener;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        listener = new QaMetricsListener(registry);
    }

    @Test
    @DisplayName("提问/回答/采纳/评论各计一次")
    void countsBusinessEvents() {
        UserId user = UserId.newId();
        QuestionId questionId = QuestionId.newId();
        AnswerId answerId = AnswerId.newId();

        listener.onQuestionPosted(new QuestionPosted(questionId, user, "标题", "java", NOW));
        listener.onAnswerSubmitted(new AnswerSubmitted(answerId, questionId, user, NOW));
        listener.onAnswerAccepted(new AnswerAccepted(questionId, answerId,
                AcceptedAnswerId.of(answerId), user, user, null, NOW));
        listener.onCommentPosted(new CommentPosted(CommentId.newId(),
                ContentTarget.question(questionId), user, NOW));

        assertEquals(1.0, registry.get("campusoverflow.questions.posted").counter().count());
        assertEquals(1.0, registry.get("campusoverflow.answers.submitted").counter().count());
        assertEquals(1.0, registry.get("campusoverflow.answers.accepted").counter().count());
        assertEquals(1.0, registry.get("campusoverflow.comments.posted").counter().count());
    }

    @Test
    @DisplayName("投票按 up/down 分标签计数")
    void countsVotesByValue() {
        UserId voter = UserId.newId();
        UserId author = UserId.newId();
        ContentTarget target = ContentTarget.question(QuestionId.newId());

        listener.onVoteCast(new VoteCast(target, voter, author, VoteValue.UP, 10, NOW));
        listener.onVoteCast(new VoteCast(target, voter, author, VoteValue.UP, 10, NOW));
        listener.onVoteCast(new VoteCast(target, voter, author, VoteValue.DOWN, -2, NOW));

        assertEquals(2.0, registry.get("campusoverflow.votes.cast").tag("value", "up").counter().count());
        assertEquals(1.0, registry.get("campusoverflow.votes.cast").tag("value", "down").counter().count());
    }
}
