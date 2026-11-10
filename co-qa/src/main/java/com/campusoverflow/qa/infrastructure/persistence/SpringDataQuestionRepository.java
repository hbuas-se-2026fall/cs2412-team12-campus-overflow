package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 问题 Spring Data 仓储：只被适配器使用。
 */
public interface SpringDataQuestionRepository extends JpaRepository<QuestionJpaEntity, String> {

    /** 按创建时间倒序分页（由适配器传入 Pageable）。 */
    Page<QuestionJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 按标签分页（US-10）：走 question_tag 派生索引，避免对 tags CSV 做 LIKE 全表扫描。
     */
    @Query(value = """
            SELECT q.*
            FROM question q
                     JOIN question_tag qt ON qt.question_id = q.id
            WHERE qt.tag = :tag
            ORDER BY q.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<QuestionJpaEntity> findByTagOrderByCreatedAtDesc(@Param("tag") String tag,
                                                          @Param("offset") int offset,
                                                          @Param("size") int size);

    @Query(value = "SELECT COUNT(*) FROM question_tag WHERE tag = :tag", nativeQuery = true)
    long countByTag(@Param("tag") String tag);
}
