package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import com.campusoverflow.shared.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 问答核心的小值对象与枚举测试：补齐 JaCoCo 报告里 domain.model 的盲点。
 */
class QaValueObjectTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");

    @Test
    @DisplayName("标题：5–150 字，空白与超长被拒绝")
    void titleRules() {
        assertEquals(5, Title.of("五个字符啊").value().length());
        assertThrows(InvariantViolationException.class, () -> Title.of("四个字啊"));
        assertThrows(InvariantViolationException.class, () -> Title.of(" "));
        assertThrows(InvariantViolationException.class, () -> Title.of("长".repeat(151)));
    }

    @Test
    @DisplayName("正文：问题 20000 上限、回答 10000 上限")
    void bodyLimits() {
        assertEquals(3, Body.ofQuestion("abc").value().length());
        assertThrows(InvariantViolationException.class, () -> Body.ofQuestion(" "));
        assertThrows(InvariantViolationException.class,
                () -> Body.ofQuestion("x".repeat(Body.QUESTION_MAX_LENGTH + 1)));
        assertThrows(InvariantViolationException.class,
                () -> Body.ofAnswer("x".repeat(Body.ANSWER_MAX_LENGTH + 1)));
    }

    @Test
    @DisplayName("标识：UUID 解析与非法输入")
    void idRules() {
        QuestionId qid = QuestionId.newId();
        AnswerId aid = AnswerId.newId();
        CommentId cid = CommentId.newId();

        assertEquals(qid, QuestionId.of(qid.asString()));
        assertEquals(aid, AnswerId.of(aid.asString()));
        assertEquals(cid, CommentId.of(cid.asString()));
        assertThrows(IllegalArgumentException.class, () -> QuestionId.of("bad"));
        assertThrows(IllegalArgumentException.class, () -> AnswerId.of("bad"));
        assertThrows(IllegalArgumentException.class, () -> CommentId.of("bad"));
    }

    @Test
    @DisplayName("ContentTarget：区分问题与回答，空目标被拒绝")
    void contentTargetRules() {
        QuestionId qid = QuestionId.newId();
        ContentTarget question = ContentTarget.question(qid);
        ContentTarget answer = ContentTarget.answer(AnswerId.newId());

        assertTrue(question.isQuestion());
        assertFalse(answer.isQuestion());
        assertEquals(qid.asString(), question.targetId());
        assertEquals(TargetType.ANSWER, answer.type());
        assertEquals(TargetType.QUESTION, ContentTarget.of(TargetType.QUESTION, qid.asString()).type());
        assertThrows(IllegalArgumentException.class, () -> ContentTarget.of(TargetType.QUESTION, " "));
        assertThrows(NullPointerException.class, () -> ContentTarget.of(null, qid.asString()));
    }

    @Test
    @DisplayName("AcceptedAnswerId：从 AnswerId 或字符串构造")
    void acceptedAnswerIdRules() {
        AnswerId answerId = AnswerId.newId();

        assertEquals(answerId.asString(), AcceptedAnswerId.of(answerId).asString());
        assertEquals(answerId.asString(), AcceptedAnswerId.of(answerId.asString()).asString());
        assertThrows(IllegalArgumentException.class, () -> AcceptedAnswerId.of("bad"));
    }

    @Test
    @DisplayName("状态与取值：code / isVisible / score")
    void enumRules() {
        assertTrue(QuestionStatus.OPEN.isVisible());
        assertFalse(QuestionStatus.HIDDEN.isVisible());
        assertEquals("solved", QuestionStatus.SOLVED.code());

        assertTrue(AnswerStatus.NORMAL.isVisible());
        assertFalse(AnswerStatus.HIDDEN.isVisible());
        assertEquals("hidden", AnswerStatus.HIDDEN.code());

        assertEquals("question", TargetType.QUESTION.code());
        assertEquals("answer", TargetType.ANSWER.code());

        assertEquals(1, VoteValue.UP.score());
        assertEquals(-1, VoteValue.DOWN.score());
        assertEquals("up", VoteValue.UP.code());
    }

    @Test
    @DisplayName("Vote：rehydrate 保留标识，reputationPoints 对应 +10 / −2")
    void voteHelpers() {
        ContentTarget target = ContentTarget.question(QuestionId.newId());
        VoteId id = VoteId.newId();
        UserId voter = UserId.newId();

        Vote vote = Vote.rehydrate(id, target, voter, VoteValue.DOWN, NOW);

        assertEquals(id, vote.id());
        assertEquals(VoteValue.DOWN, vote.value());
        assertEquals(Vote.DOWNVOTE_POINTS, Vote.reputationPoints(VoteValue.DOWN));
        assertEquals(Vote.UPVOTE_POINTS, Vote.reputationPoints(VoteValue.UP));
    }

    @Test
    @DisplayName("标识与目标：toString / newId / of 的边界")
    void idEdgeCases() {
        assertTrue(QuestionId.newId().toString().length() == 36);
        assertTrue(AnswerId.newId().toString().length() == 36);
        assertTrue(CommentId.newId().toString().length() == 36);
        VoteId voteId = VoteId.newId();
        assertEquals(voteId, VoteId.of(voteId.asString()));
        assertThrows(IllegalArgumentException.class, () -> VoteId.of("bad"));
        assertThrows(IllegalArgumentException.class, () -> AcceptedAnswerId.of((String) null));
    }

    @Test
    @DisplayName("问题：下架幂等、编辑成功、票数投影参数校验")
    void questionBehaviourEdges() {
        Question question = Question.post(QuestionId.newId(), UserId.newId(),
                Title.of("边界测试的问题标题"), Body.ofQuestion("边界测试正文。"), TagSet.empty(), NOW);

        question.hide("第一次", NOW);
        question.hide("第二次", NOW);
        assertEquals(QuestionStatus.HIDDEN, question.status());

        Question visible = Question.post(QuestionId.newId(), UserId.newId(),
                Title.of("可以编辑的问题标题"), Body.ofQuestion("可以编辑的正文。"), TagSet.empty(), NOW);
        visible.edit(Title.of("编辑之后的新标题"), Body.ofQuestion("编辑之后的正文。"),
                TagSet.empty(), visible.askerId(), NOW.plusSeconds(60));
        assertEquals("编辑之后的新标题", visible.title().value());

        assertThrows(IllegalArgumentException.class, () -> visible.applyVoteProjection(3, NOW));
        assertThrows(InvariantViolationException.class, () -> Title.of((String) null));
        assertThrows(InvariantViolationException.class, () -> Body.ofQuestion(null));
    }

    @Test
    @DisplayName("回答：编辑、下架、rehydrate 与删除守卫")
    void answerBehaviourEdges() {
        QuestionId questionId = QuestionId.newId();
        Answer answer = Answer.rehydrate(AnswerId.newId(), questionId, UserId.newId(),
                Body.ofAnswer("回答正文内容。"), AnswerStatus.NORMAL, false, 2, NOW, NOW);

        assertEquals(2, answer.voteCount());
        answer.edit(Body.ofAnswer("编辑后的回答正文。"), answer.authorId(), NOW.plusSeconds(60));
        answer.hide("下架理由", NOW.plusSeconds(120));
        assertEquals(AnswerStatus.HIDDEN, answer.status());
        assertThrows(InvariantViolationException.class,
                () -> answer.applyVoteProjection(1, NOW.plusSeconds(180)));
    }

    @Test
    @DisplayName("评论：rehydrate 与作者删除守卫")
    void commentBehaviourEdges() {
        UserId author = UserId.newId();
        Comment comment = Comment.rehydrate(CommentId.newId(),
                ContentTarget.question(QuestionId.newId()), author, "评论内容。", NOW);

        comment.assertDeletable(author);
        assertEquals("评论内容。", comment.body());
        assertThrows(InvariantViolationException.class,
                () -> Comment.post(CommentId.newId(), ContentTarget.question(QuestionId.newId()),
                        author, "x".repeat(501), NOW));
    }

    @Test
    @DisplayName("TagSet：空集合、CSV 往返与最大数量校验")
    void tagSetHelpers() {
        assertTrue(TagSet.empty().isEmpty());
        assertTrue(TagSet.fromCsv(null).isEmpty());
        assertTrue(TagSet.fromCsv(" , ").isEmpty());
        assertThrows(InvariantViolationException.class,
                () -> new TagSet(List.of(
                        Tag.of("java"), Tag.of("mysql"), Tag.of("maven"),
                        Tag.of("junit"), Tag.of("docker"), Tag.of("git"))));

        TagSet set = TagSet.fromCsv("java,mysql");
        assertEquals("java,mysql", set.asCsv());
    }
}
