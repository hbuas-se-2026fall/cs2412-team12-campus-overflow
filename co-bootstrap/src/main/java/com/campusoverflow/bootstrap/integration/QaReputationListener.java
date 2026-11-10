package com.campusoverflow.bootstrap.integration;

import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.qa.domain.event.VoteCast;
import com.campusoverflow.reputation.application.ReputationApplicationService;
import com.campusoverflow.reputation.domain.model.RuleCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 问答上下文 → 声誉上下文的防腐层监听器（US-12）。
 *
 * <p>为什么放在装配层：co-qa 不认识 co-reputation，co-reputation 也不认识 co-qa；
 * 只有装配层同时依赖两者，负责把问答事件翻译成声誉加分命令。这样两个上下文可以独立演进。</p>
 *
 * <p>为什么用 {@code AFTER_COMMIT}：事件只有在源事务真正提交后才计分，
 * 避免"投票回滚了但声誉已经加分"的不一致。</p>
 */
@Component
public class QaReputationListener {

    private final ReputationApplicationService reputationApplicationService;

    public QaReputationListener(ReputationApplicationService reputationApplicationService) {
        this.reputationApplicationService = reputationApplicationService;
    }

    /** 提问 +2。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuestionPosted(QuestionPosted event) {
        reputationApplicationService.apply(
                event.askerId().asString(),
                "QP:" + event.questionId().asString(),
                RuleCode.QUESTION_POSTED,
                event.occurredAt());
    }

    /** 回答 +5。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerSubmitted(AnswerSubmitted event) {
        reputationApplicationService.apply(
                event.authorId().asString(),
                "AS:" + event.answerId().asString(),
                RuleCode.ANSWER_SUBMITTED,
                event.occurredAt());
    }

    /** 被采纳 +15；改判时先冲正上一位作者 −15（INV-R-03）。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerAccepted(AnswerAccepted event) {
        String key = event.questionId().asString() + ":" + event.acceptedAnswerId().asString();
        reputationApplicationService.apply(
                event.acceptedAnswerAuthorId().asString(),
                "AA:" + key,
                RuleCode.ANSWER_ACCEPTED,
                event.occurredAt());

        if (event.previousAnswerAuthorId() != null) {
            reputationApplicationService.apply(
                    event.previousAnswerAuthorId().asString(),
                    "AR:" + key,
                    RuleCode.ACCEPT_REVERSED,
                    event.occurredAt());
        }
    }

    /** 被赞 +10 / 被踩 −2；改票按差值为 ±12；撤票为反向。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoteCast(VoteCast event) {
        if (event.reputationDelta() == 0) {
            return;
        }
        RuleCode ruleCode = event.reputationDelta() > 0
                ? RuleCode.UPVOTE_RECEIVED
                : RuleCode.DOWNVOTE_RECEIVED;
        String eventId = "VC:" + event.target().type().code() + ":" + event.target().targetId()
                + ":" + event.voterId().asString()
                + ":" + event.occurredAt().toEpochMilli()
                + ":" + event.reputationDelta();
        reputationApplicationService.applyDelta(
                event.targetAuthorId().asString(),
                eventId,
                ruleCode,
                event.reputationDelta(),
                event.occurredAt());
    }
}
