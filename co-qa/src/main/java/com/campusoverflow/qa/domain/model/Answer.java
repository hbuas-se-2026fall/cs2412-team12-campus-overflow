package com.campusoverflow.qa.domain.model;

import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 回答实体 / 聚合根（Lab 3 AGG-03）：与 Question 同一上下文，但独立成聚合。
 *
 * <p>不变式：</p>
 * <ul>
 *   <li>{@code INV-A-01} 内容非空且 ≤ 10000 字 —— {@link Body#ofAnswer}</li>
 *   <li>{@code INV-A-02} 问题必须存在且未下架 —— 应用服务校验（跨聚合）</li>
 *   <li>{@code INV-A-03} 60 秒内防重复提交 —— 应用服务限流</li>
 *   <li>{@code INV-A-04} 被采纳的回答不可删除，只能编辑</li>
 * </ul>
 */
public final class Answer extends AggregateRoot {

    private final AnswerId id;
    private final QuestionId questionId;
    private final UserId authorId;
    private final Instant createdAt;

    private Body body;
    private AnswerStatus status;
    private boolean accepted;
    private Instant updatedAt;

    private Answer(AnswerId id, QuestionId questionId, UserId authorId, Body body,
                   AnswerStatus status, boolean accepted, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "answerId 不能为空");
        this.questionId = Objects.requireNonNull(questionId, "questionId 不能为空");
        this.authorId = Objects.requireNonNull(authorId, "authorId 不能为空");
        this.body = Objects.requireNonNull(body, "body 不能为空");
        this.status = Objects.requireNonNull(status, "status 不能为空");
        this.accepted = accepted;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /** 提交回答（{@code SubmitAnswer}）。 */
    public static Answer submit(AnswerId id, QuestionId questionId, UserId authorId, Body body, Instant now) {
        Answer answer = new Answer(id, questionId, authorId, body, AnswerStatus.NORMAL, false, now, now);
        answer.registerEvent(new AnswerSubmitted(id, questionId, authorId, now));
        return answer;
    }

    public static Answer rehydrate(AnswerId id, QuestionId questionId, UserId authorId, Body body,
                                   AnswerStatus status, boolean accepted, Instant createdAt, Instant updatedAt) {
        return new Answer(id, questionId, authorId, body, status, accepted, createdAt, updatedAt);
    }

    /** 编辑：仅作者本人，且未被下架。 */
    public void edit(Body newBody, UserId operatorId, Instant now) {
        assertAuthor(operatorId);
        assertVisible("编辑");
        this.body = Objects.requireNonNull(newBody, "body 不能为空");
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    /** 被采纳；同一回答重复采纳保持幂等。 */
    public void markAccepted(Instant now) {
        assertVisible("被采纳");
        this.accepted = true;
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    /** 删除前的守卫：{@code INV-A-04} 被采纳的回答不可删除。 */
    public void assertDeletable(UserId operatorId) {
        assertAuthor(operatorId);
        if (accepted) {
            throw new InvariantViolationException(AnswerInvariants.ACCEPTED_CANNOT_DELETE,
                    "被采纳的回答不能删除，只能编辑");
        }
    }

    /** 下架：治理动作，保留内容与审计。 */
    public void hide(String reason, Instant now) {
        if (status == AnswerStatus.HIDDEN) {
            return;
        }
        this.status = AnswerStatus.HIDDEN;
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    private void assertAuthor(UserId operatorId) {
        if (!authorId.equals(operatorId)) {
            throw new InvariantViolationException(AnswerInvariants.ONLY_AUTHOR, "只有回答者本人可以操作该回答");
        }
    }

    private void assertVisible(String action) {
        if (!status.isVisible()) {
            throw new InvariantViolationException(QuestionInvariants.HIDDEN_IMMUTABLE, "回答已下架，不能再" + action);
        }
    }

    public AnswerId id() {
        return id;
    }

    public QuestionId questionId() {
        return questionId;
    }

    public UserId authorId() {
        return authorId;
    }

    public Body body() {
        return body;
    }

    public AnswerStatus status() {
        return status;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
