package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.identity.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 回答仓储。
 */
public interface AnswerRepository {

    Optional<Answer> findById(AnswerId id);

    List<Answer> findByQuestionId(QuestionId questionId);

    /** 某作者对某问题的最近一条回答，用于 {@code INV-A-03} 的 60 秒限流。 */
    Optional<Answer> findLatestByQuestionAndAuthor(QuestionId questionId, UserId authorId);

    Answer save(Answer answer);

    void delete(AnswerId id);
}
