package com.campusoverflow.bootstrap.monitoring;

import com.campusoverflow.qa.domain.event.AnswerAccepted;
import com.campusoverflow.qa.domain.event.AnswerSubmitted;
import com.campusoverflow.qa.domain.event.CommentPosted;
import com.campusoverflow.qa.domain.event.QuestionPosted;
import com.campusoverflow.qa.domain.event.VoteCast;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lab 8：把业务事件翻译成 Prometheus 指标。
 *
 * <p>放在装配层（co-bootstrap）而不是领域层：领域模型不应该知道 Micrometer。
 * 监听器在事务提交后执行，失败只影响指标、不影响业务。</p>
 *
 * <p>暴露的指标（Prometheus 命名）：</p>
 * <ul>
 *   <li>{@code campusoverflow_questions_posted_total}</li>
 *   <li>{@code campusoverflow_answers_submitted_total}</li>
 *   <li>{@code campusoverflow_answers_accepted_total}</li>
 *   <li>{@code campusoverflow_votes_cast_total{value="up|down"}}</li>
 *   <li>{@code campusoverflow_comments_posted_total}</li>
 * </ul>
 * <p>再加上 Actuator 自带的 HTTP 延迟/错误率、JVM、连接池指标，共 10+ 个可观测指标。</p>
 */
@Component
public class QaMetricsListener {

    private final MeterRegistry registry;

    public QaMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuestionPosted(QuestionPosted event) {
        registry.counter("campusoverflow.questions.posted").increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerSubmitted(AnswerSubmitted event) {
        registry.counter("campusoverflow.answers.submitted").increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerAccepted(AnswerAccepted event) {
        registry.counter("campusoverflow.answers.accepted").increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVoteCast(VoteCast event) {
        registry.counter("campusoverflow.votes.cast", "value", event.value().code()).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentPosted(CommentPosted event) {
        registry.counter("campusoverflow.comments.posted").increment();
    }
}
