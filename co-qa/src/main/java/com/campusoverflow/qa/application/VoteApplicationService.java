package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.CastVoteCommand;
import com.campusoverflow.qa.application.view.VoteResult;
import com.campusoverflow.qa.domain.event.VoteCast;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.TargetType;
import com.campusoverflow.qa.domain.model.Vote;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.qa.domain.repository.VoteRepository;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 投票应用服务（US-07）。
 *
 * <p>编排要点：</p>
 * <ol>
 *   <li>投票记录本身是 {@link Vote} 聚合，独立于被投票内容；</li>
 *   <li>目标上的 {@code voteCount} 只是投影，由目标聚合的 {@code applyVoteProjection} 维护；</li>
 *   <li>改票只写一次记录（唯一键保证），净变化交给 {@link Vote#netDelta}；</li>
 *   <li>每次变化都发 {@code VoteCast}，由声誉上下文按 {@code reputationDelta} 加减分。</li>
 * </ol>
 */
@Service
public class VoteApplicationService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final VoteRepository voteRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public VoteApplicationService(QuestionRepository questionRepository,
                                  AnswerRepository answerRepository,
                                  VoteRepository voteRepository,
                                  DomainEventPublisher eventPublisher,
                                  Clock clock) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.voteRepository = voteRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** 投票 / 改票（{@code CastVote}）。 */
    @Transactional
    public VoteResult cast(CastVoteCommand command) {
        ContentTarget target = toTarget(command.targetType(), command.targetId());
        UserId voterId = UserId.of(command.voterId());
        VoteValue value = VoteValue.valueOf(command.value().toUpperCase(Locale.ROOT));
        Instant now = now();

        if (target.isQuestion()) {
            Question question = loadQuestion(target);
            return voteOn(target, voterId, value, question.askerId(), question.voteCount(),
                    (delta, at) -> {
                        question.applyVoteProjection(delta, at);
                        questionRepository.save(question);
                        return question.voteCount();
                    }, now);
        }
        Answer answer = loadAnswer(target);
        return voteOn(target, voterId, value, answer.authorId(), answer.voteCount(),
                (delta, at) -> {
                    answer.applyVoteProjection(delta, at);
                    answerRepository.save(answer);
                    return answer.voteCount();
                }, now);
    }

    /** 撤票（幂等：没投过票时直接返回）。 */
    @Transactional
    public void retract(CastVoteCommand command) {
        ContentTarget target = toTarget(command.targetType(), command.targetId());
        UserId voterId = UserId.of(command.voterId());
        Optional<Vote> existing = voteRepository.find(target, voterId);
        if (existing.isEmpty()) {
            return;
        }
        Vote vote = existing.get();
        Instant now = now();
        int delta = -vote.value().score();
        int reputationDelta = -Vote.reputationPoints(vote.value());
        UserId authorId = authorIdOf(target);

        applyProjection(target, delta, now);
        voteRepository.delete(vote);
        eventPublisher.publish(List.of(
                new VoteCast(target, voterId, authorId, vote.value(), reputationDelta, now)));
    }

    private VoteResult voteOn(ContentTarget target, UserId voterId, VoteValue value,
                              UserId authorId, int currentCount, Projection projection, Instant now) {
        Optional<Vote> existing = voteRepository.find(target, voterId);

        Vote toSave;
        int delta;
        int reputationDelta;
        if (existing.isEmpty()) {
            toSave = Vote.cast(target, voterId, authorId, value, now);
            delta = value.score();
            reputationDelta = Vote.reputationPoints(value);
        } else {
            Vote revised = existing.get().revise(value, now);
            if (revised == existing.get()) {
                // 重复投同一票：幂等，不改投影也不发事件
                return new VoteResult(target, voterId, value, currentCount);
            }
            toSave = revised;
            delta = Vote.netDelta(existing.get().value(), value);
            reputationDelta = Vote.reputationPoints(value) - Vote.reputationPoints(existing.get().value());
        }

        int newCount = projection.apply(delta, now);
        voteRepository.save(toSave);
        eventPublisher.publish(List.of(
                new VoteCast(target, voterId, authorId, value, reputationDelta, now)));
        return new VoteResult(target, voterId, value, newCount);
    }

    private void applyProjection(ContentTarget target, int delta, Instant now) {
        if (target.isQuestion()) {
            Question question = loadQuestion(target);
            question.applyVoteProjection(delta, now);
            questionRepository.save(question);
            return;
        }
        Answer answer = loadAnswer(target);
        answer.applyVoteProjection(delta, now);
        answerRepository.save(answer);
    }

    private UserId authorIdOf(ContentTarget target) {
        return target.isQuestion() ? loadQuestion(target).askerId() : loadAnswer(target).authorId();
    }

    private Question loadQuestion(ContentTarget target) {
        return questionRepository.findById(QuestionId.of(target.targetId()))
                .orElseThrow(() -> EntityNotFoundException.of("问题", target.targetId()));
    }

    private Answer loadAnswer(ContentTarget target) {
        return answerRepository.findById(AnswerId.of(target.targetId()))
                .orElseThrow(() -> EntityNotFoundException.of("回答", target.targetId()));
    }

    private ContentTarget toTarget(String rawType, String targetId) {
        TargetType type = TargetType.valueOf(rawType.toUpperCase(Locale.ROOT));
        return new ContentTarget(type, targetId);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** 把"目标投影的更新"抽象成一个返回值，避免 question / answer 两套重复分支。 */
    @FunctionalInterface
    private interface Projection {
        int apply(int delta, Instant now);
    }
}
