package com.campusoverflow.qa.domain.model;

import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.QuestionEdited;
import com.campusoverflow.qa.domain.event.QuestionHidden;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 问题聚合根（Lab 3 AGG-02，所属上下文：问答核心）。
 *
 * <p>为什么 Answer 不在这个聚合里：Lab 3 的结论是"回答无上限 + 并发写"，放进来会让聚合无限膨胀并锁住问题行；
 * Question 只持有 {@code acceptedAnswerId} 这一个弱引用，采纳的一致性由应用服务校验"答案确实属于本问题"完成。</p>
 *
 * <p>不变式落地位置：</p>
 * <table border="1">
 *   <tr><th>编号</th><th>规则</th><th>落地方法</th></tr>
 *   <tr><td>INV-Q-01</td><td>标题 5–150 / 正文 ≤ 20000</td><td>{@link Title}、{@link Body} 构造</td></tr>
 *   <tr><td>INV-Q-02</td><td>标签 ≤ 5 且在词表内</td><td>{@link TagSet#of}</td></tr>
 *   <tr><td>INV-Q-03</td><td>只有提问者可采纳</td><td>{@link #acceptAnswer}</td></tr>
 *   <tr><td>INV-Q-04</td><td>最多一个采纳，改判留痕</td><td>{@link #acceptAnswer} + {@code AnswerAccepted.previousAcceptedAnswerId}</td></tr>
 *   <tr><td>INV-Q-05</td><td>hidden 后不可编辑/采纳/投票</td><td>{@link #assertVisible}</td></tr>
 * </table>
 */
public final class Question extends AggregateRoot {

    private final QuestionId id;
    private final UserId askerId;
    private final Instant createdAt;

    private Title title;
    private Body body;
    private TagSet tags;
    private AcceptedAnswerId acceptedAnswerId;
    private QuestionStatus status;
    private int voteCount;
    private Instant updatedAt;

    private Question(QuestionId id, UserId askerId, Title title, Body body, TagSet tags,
                     AcceptedAnswerId acceptedAnswerId, QuestionStatus status, int voteCount,
                     Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "questionId 不能为空");
        this.askerId = Objects.requireNonNull(askerId, "askerId 不能为空");
        this.title = Objects.requireNonNull(title, "title 不能为空");
        this.body = Objects.requireNonNull(body, "body 不能为空");
        this.tags = Objects.requireNonNull(tags, "tags 不能为空");
        this.acceptedAnswerId = acceptedAnswerId;
        this.status = Objects.requireNonNull(status, "status 不能为空");
        this.voteCount = voteCount;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /** 发布问题（{@code PostQuestion}）：新问题一定是 open，票数为 0。 */
    public static Question post(QuestionId id, UserId askerId, Title title, Body body, TagSet tags, Instant now) {
        Question question = new Question(id, askerId, title, body, tags, null, QuestionStatus.OPEN, 0, now, now);
        question.registerEvent(new QuestionPosted(id, askerId, title.value(), tags.asCsv(), now));
        return question;
    }

    /** 从持久化状态重建：不校验、不发事件（历史数据已合法）。 */
    public static Question rehydrate(QuestionId id, UserId askerId, Title title, Body body, TagSet tags,
                                     AcceptedAnswerId acceptedAnswerId, QuestionStatus status, int voteCount,
                                     Instant createdAt, Instant updatedAt) {
        return new Question(id, askerId, title, body, tags, acceptedAnswerId, status, voteCount, createdAt, updatedAt);
    }

    /** 编辑（{@code EditQuestion}）：只有提问者本人，且问题未被下架。 */
    public void edit(Title newTitle, Body newBody, TagSet newTags, UserId operatorId, Instant now) {
        assertVisible("编辑");
        if (!askerId.equals(operatorId)) {
            throw new InvariantViolationException(QuestionInvariants.EDIT_ONLY_ASKER, "只有提问者本人可以编辑问题");
        }
        this.title = Objects.requireNonNull(newTitle, "title 不能为空");
        this.body = Objects.requireNonNull(newBody, "body 不能为空");
        this.tags = Objects.requireNonNull(newTags, "tags 不能为空");
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
        registerEvent(new QuestionEdited(id, askerId, now));
    }

    /**
     * 采纳答案（{@code AcceptAnswer}）：仅提问者（{@code INV-Q-03}），全问题最多一个（{@code INV-Q-04}）。
     *
     * <p>"答案是否属于本问题""答案是否被下架"属于跨聚合校验，由应用服务在调用本方法前完成；
     * {@code acceptedAnswerAuthorId} 用于给新作者 +15，{@code previousAnswerAuthorId} 是改判时
     * 上一位被采纳者的 ID（首次采纳为 null），声誉上下文据此冲正。</p>
     *
     * @return 是否真的发生了改判（重复采纳同一条答案返回 false，不产生事件）
     */
    public boolean acceptAnswer(AnswerId answerId, UserId acceptedAnswerAuthorId,
                                UserId previousAnswerAuthorId, UserId operatorId, Instant now) {
        assertVisible("采纳答案");
        if (!askerId.equals(operatorId)) {
            throw new InvariantViolationException(QuestionInvariants.ACCEPT_ONLY_ASKER, "只有提问者本人可以采纳答案");
        }
        Objects.requireNonNull(answerId, "answerId 不能为空");
        Objects.requireNonNull(acceptedAnswerAuthorId, "acceptedAnswerAuthorId 不能为空");
        AcceptedAnswerId next = AcceptedAnswerId.of(answerId);
        if (next.equals(acceptedAnswerId)) {
            return false;
        }
        AcceptedAnswerId previous = acceptedAnswerId;
        this.acceptedAnswerId = next;
        this.status = QuestionStatus.SOLVED;
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
        registerEvent(new AnswerAccepted(id, answerId, previous, askerId,
                acceptedAnswerAuthorId, previousAnswerAuthorId, now));
        return true;
    }

    /** 下架（{@code HideQuestion}）：幂等；下架后不可再编辑、采纳或投票。 */
    public void hide(String reason, Instant now) {
        if (status == QuestionStatus.HIDDEN) {
            return;
        }
        this.status = QuestionStatus.HIDDEN;
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
        registerEvent(new QuestionHidden(id, reason, now));
    }

    /**
     * 应用票数投影变化（{@code INV-V-04}：下架内容不再接受投票）。
     *
     * <p>票数是投影值而不是 Votes 集合——这是 Lab 3 为了热点问题写放大做的取舍，见聚合清单 AGG-04 说明。</p>
     */
    public void applyVoteProjection(int delta, Instant now) {
        if (delta != 1 && delta != -1 && delta != 2 && delta != -2) {
            throw new IllegalArgumentException("票数投影只允许 ±1 或 ±2，收到：" + delta);
        }
        assertVisible("被投票");
        this.voteCount = Math.addExact(voteCount, delta);
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    private void assertVisible(String action) {
        if (!status.isVisible()) {
            throw new InvariantViolationException(QuestionInvariants.HIDDEN_IMMUTABLE,
                    "问题已下架，不能再" + action);
        }
    }

    public QuestionId id() {
        return id;
    }

    public UserId askerId() {
        return askerId;
    }

    public Title title() {
        return title;
    }

    public Body body() {
        return body;
    }

    public TagSet tags() {
        return tags;
    }

    public Optional<AcceptedAnswerId> acceptedAnswerId() {
        return Optional.ofNullable(acceptedAnswerId);
    }

    public QuestionStatus status() {
        return status;
    }

    public int voteCount() {
        return voteCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
