package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 问题-标签索引仓储。
 */
public interface SpringDataQuestionTagRepository
        extends JpaRepository<QuestionTagJpaEntity, QuestionTagJpaEntity.QuestionTagId> {

    void deleteByQuestionId(String questionId);
}
