package com.campusoverflow.qa.application;

import com.campusoverflow.qa.application.command.PostCommentCommand;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.TargetType;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import com.campusoverflow.qa.domain.repository.CommentRepository;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * 评论应用服务（US-09）。
 */
@Service
public class CommentApplicationService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final Clock clock;

    public CommentApplicationService(QuestionRepository questionRepository,
                                     AnswerRepository answerRepository,
                                     CommentRepository commentRepository,
                                     Clock clock) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.commentRepository = commentRepository;
        this.clock = clock;
    }

    /** 发表评论：目标必须存在（INV-C-02），内容长度由聚合守卫（INV-C-01）。 */
    @Transactional
    public Comment post(PostCommentCommand command) {
        ContentTarget target = toTarget(command.targetType(), command.targetId());
        ensureTargetExists(target);
        Comment comment = Comment.post(CommentId.newId(), target,
                UserId.of(command.authorId()), command.body(), now());
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> list(String rawType, String targetId) {
        return commentRepository.findByTarget(toTarget(rawType, targetId));
    }

    /** 删除评论：仅作者本人（INV-C-03）。 */
    @Transactional
    public void delete(String commentId, String operatorId) {
        Comment comment = commentRepository.findById(CommentId.of(commentId))
                .orElseThrow(() -> EntityNotFoundException.of("评论", commentId));
        comment.assertDeletable(UserId.of(operatorId));
        commentRepository.delete(comment.id());
    }

    private void ensureTargetExists(ContentTarget target) {
        if (target.isQuestion()) {
            questionRepository.findById(QuestionId.of(target.targetId()))
                    .orElseThrow(() -> EntityNotFoundException.of("问题", target.targetId()));
            return;
        }
        answerRepository.findById(AnswerId.of(target.targetId()))
                .orElseThrow(() -> EntityNotFoundException.of("回答", target.targetId()));
    }

    private ContentTarget toTarget(String rawType, String targetId) {
        return new ContentTarget(TargetType.valueOf(rawType.toUpperCase(Locale.ROOT)), targetId);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
