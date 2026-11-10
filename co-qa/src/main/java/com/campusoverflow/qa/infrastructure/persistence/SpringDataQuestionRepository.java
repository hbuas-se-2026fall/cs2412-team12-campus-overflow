package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 问题 Spring Data 仓储：只被适配器使用。
 */
public interface SpringDataQuestionRepository extends JpaRepository<QuestionJpaEntity, String> {

    /** 按创建时间倒序分页（由适配器传入 Pageable）。 */
    Page<QuestionJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
