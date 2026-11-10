package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 问题 Spring Data 仓储：只被适配器使用。
 */
public interface SpringDataQuestionRepository extends JpaRepository<QuestionJpaEntity, String> {
}
