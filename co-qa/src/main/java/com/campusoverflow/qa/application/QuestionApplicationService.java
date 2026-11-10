package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.AcceptAnswerCommand;
import com.campusoverflow.qa.application.command.CreateQuestionCommand;
import com.campusoverflow.qa.application.command.SubmitAnswerCommand;
import com.campusoverflow.qa.application.view.QuestionPage;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.AnswerInvariants;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.TagSet;
import com.campusoverflow.qa.domain.model.Title;
import com.campusoverflow.qa.domain.port.AskerVerifier;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import com.campusoverflow.shared.tag.Tag;
import com.campusoverflow.shared.tag.TagVocabulary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 问答应用服务：把一次用例编排成"取聚合 → 守护卫 → 调聚合方法 → 保存 → 发事件"。
 *
 * <p>业务判断全部在聚合里；这里只处理跨聚合/跨上下文的编排（如"答案确实属于这个问题"）。</p>
 */
@Service
public class QuestionApplicationService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AskerVerifier askerVerifier;
    private final TagVocabulary tagVocabulary;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public QuestionApplicationService(QuestionRepository questionRepository,
                                      AnswerRepository answerRepository,
                                      AskerVerifier askerVerifier,
                                      TagVocabulary tagVocabulary,
                                      DomainEventPublisher eventPublisher,
                                      Clock clock) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.askerVerifier = askerVerifier;
        this.tagVocabulary = tagVocabulary;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * 发布问题：校验提问人 → 解析/登记标签 → 聚合内校验标题/正文 → 落库 → 发事件。
     */
    @Transactional
    public Question create(CreateQuestionCommand command) {
        UserId askerId = UserId.of(command.askerId());
        // 跨上下文：身份上下文说"这个人能发布"，问答上下文不认 User 类型
        askerVerifier.ensureCanPublish(askerId);

        Title title = Title.of(command.title());
        Body body = Body.ofQuestion(command.body());
        TagSet tags = resolveTags(command.tags());

        Question question = Question.post(QuestionId.newId(), askerId, title, body, tags, now());
        Question saved = questionRepository.save(question);
        // 事件只存在于"刚计算的聚合"上；save() 返回的是重建对象，没有事件
        eventPublisher.publish(question.pullDomainEvents());
        return saved;
    }

    /**
     * 提交回答（US-06）：问题必须可见（INV-A-02），同一作者对同一问题 60 秒限流（INV-A-03）。
     */
    @Transactional
    public Answer submitAnswer(SubmitAnswerCommand command) {
        QuestionId questionId = QuestionId.of(command.questionId());
        UserId authorId = UserId.of(command.authorId());
        askerVerifier.ensureCanPublish(authorId);

        Question question = get(questionId);
        if (!question.status().isVisible()) {
            throw new InvariantViolationException(AnswerInvariants.QUESTION_MUST_BE_VISIBLE,
                    "问题已下架，不能再回答");
        }

        Instant now = now();
        answerRepository.findLatestByQuestionAndAuthor(questionId, authorId).ifPresent(last -> {
            long seconds = Duration.between(last.createdAt(), now).getSeconds();
            if (seconds >= 0 && seconds < 60) {
                throw new InvariantViolationException(AnswerInvariants.SUBMIT_TOO_FREQUENT,
                        "同一问题 60 秒内只能提交一次回答，请 " + (60 - seconds) + " 秒后再试");
            }
        });

        Answer answer = Answer.submit(AnswerId.newId(), questionId, authorId, Body.ofAnswer(command.body()), now);
        Answer saved = answerRepository.save(answer);
        eventPublisher.publish(answer.pullDomainEvents());
        return saved;
    }

    /** 采纳答案（US-08）：跨聚合校验"答案属于本问题且未下架"，其余规则由聚合守卫。 */
    @Transactional
    public Question acceptAnswer(AcceptAnswerCommand command) {
        QuestionId questionId = QuestionId.of(command.questionId());
        AnswerId answerId = AnswerId.of(command.answerId());
        UserId operatorId = UserId.of(command.operatorId());

        Question question = get(questionId);
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> EntityNotFoundException.of("回答", answerId));

        if (!answer.questionId().equals(questionId)) {
            throw new InvariantViolationException(AnswerInvariants.QUESTION_MUST_BE_VISIBLE,
                    "回答 " + answerId + " 不属于问题 " + questionId);
        }
        if (!answer.status().isVisible()) {
            throw new InvariantViolationException(AnswerInvariants.QUESTION_MUST_BE_VISIBLE, "该回答已被下架，不能被采纳");
        }

        // 改判时要知道上一位被采纳者的 ID，声誉上下文才能冲正（INV-R-03）
        UserId previousAnswerAuthorId = question.acceptedAnswerId()
                .flatMap(previous -> answerRepository.findById(AnswerId.of(previous.asString())))
                .map(Answer::authorId)
                .orElse(null);

        Instant now = now();
        question.acceptAnswer(answerId, answer.authorId(), previousAnswerAuthorId, operatorId, now);
        answer.markAccepted(now);

        Question saved = questionRepository.save(question);
        answerRepository.save(answer);
        eventPublisher.publish(question.pullDomainEvents());
        return saved;
    }

    /** 下架问题（治理动作，US-24 会由管理端调用）。 */
    @Transactional
    public Question hide(String questionId, String reason) {
        Question question = get(QuestionId.of(questionId));
        question.hide(reason, now());
        Question saved = questionRepository.save(question);
        eventPublisher.publish(question.pullDomainEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public Question get(QuestionId id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of("问题", id));
    }

    /**
     * 问题列表（US-03）：可选按标签筛选（US-10），分页参数在这里夹紧。
     */
    @Transactional(readOnly = true)
    public QuestionPage list(int page, int size, String tag) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        if (tag != null && !tag.isBlank()) {
            String normalized = Tag.of(tag).value();
            return new QuestionPage(
                    questionRepository.findPageByTag(normalized, safePage, safeSize),
                    safePage,
                    safeSize,
                    questionRepository.countByTag(normalized));
        }
        return new QuestionPage(
                questionRepository.findPage(safePage, safeSize),
                safePage,
                safeSize,
                questionRepository.count());
    }

    @Transactional(readOnly = true)
    public List<Answer> answersOf(QuestionId questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    /**
     * 标签解析（US-10）：词表可扩展——未知标签自动登记，不再直接拒绝。
     *
     * <p>这是 Sprint 3 对 INV-Q-02 的修订：词表从"封闭白名单"变成"可扩展词表"，
     * 原因见 {@code docs/adr/ADR-003-sprint3-core-features.md}。</p>
     */
    private TagSet resolveTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return TagSet.empty();
        }
        List<Tag> tags = new ArrayList<>();
        Set<Tag> seen = new HashSet<>();
        for (String raw : rawTags) {
            Tag tag = Tag.of(raw);
            if (seen.add(tag)) {
                tags.add(tagVocabulary.registerIfAbsent(tag));
            }
        }
        return new TagSet(tags);
    }

    /** 与数据库 DATETIME(6) 对齐：先截断到微秒，避免写回后精度"变小"。 */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
