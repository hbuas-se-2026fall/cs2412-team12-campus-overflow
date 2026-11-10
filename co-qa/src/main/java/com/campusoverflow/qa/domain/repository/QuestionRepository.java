package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;

import java.util.List;
import java.util.Optional;

/**
 * 问题仓储：接口在 domain 层，实现（JPA）在 infrastructure 层。
 */
public interface QuestionRepository {

    Optional<Question> findById(QuestionId id);

    Question save(Question question);

    boolean existsById(QuestionId id);

    /**
     * 按创建时间倒序分页查询（US-03）。
     *
     * @param page 从 0 开始的页码
     * @param size 每页条数（1–100）
     */
    List<Question> findPage(int page, int size);

    /** 按标签筛选（US-10），走 question_tag 派生索引。 */
    List<Question> findPageByTag(String tag, int page, int size);

    long count();

    long countByTag(String tag);
}
