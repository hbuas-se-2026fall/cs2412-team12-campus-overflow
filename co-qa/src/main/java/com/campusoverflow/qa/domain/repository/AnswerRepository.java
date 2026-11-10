package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.QuestionId;

import java.util.List;
import java.util.Optional;

/**
 * 回答仓储。
 */
public interface AnswerRepository {

    Optional<Answer> findById(AnswerId id);

    List<Answer> findByQuestionId(QuestionId questionId);

    Answer save(Answer answer);

    void delete(AnswerId id);
}
