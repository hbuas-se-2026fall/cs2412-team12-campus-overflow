package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 评论 Spring Data 仓储。
 */
public interface SpringDataCommentRepository extends JpaRepository<CommentJpaEntity, String> {

    List<CommentJpaEntity> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, String targetId);
}
