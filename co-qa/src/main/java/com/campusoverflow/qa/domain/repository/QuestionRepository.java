package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;

import java.util.Optional;

/**
 * 问题仓储：接口在 domain 层，实现（JPA）在 infrastructure 层。
 */
public interface QuestionRepository {

    Optional<Question> findById(QuestionId id);

    Question save(Question question);

    boolean existsById(QuestionId id);
}
