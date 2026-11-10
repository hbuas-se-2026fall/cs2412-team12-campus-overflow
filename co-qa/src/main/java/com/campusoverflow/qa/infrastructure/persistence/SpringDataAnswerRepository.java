package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 回答 Spring Data 仓储。
 */
public interface SpringDataAnswerRepository extends JpaRepository<AnswerJpaEntity, String> {

    List<AnswerJpaEntity> findByQuestionIdOrderByCreatedAtAsc(String questionId);
}
