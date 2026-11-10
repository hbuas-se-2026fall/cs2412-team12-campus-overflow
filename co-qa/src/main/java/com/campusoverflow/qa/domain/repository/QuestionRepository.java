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
     * 按创建时间倒序分页查询（US-03 的最小实现）。
     *
     * <p>注意：这是直接查聚合，属于 Sprint 2 的临时读法；Sprint 3 的 US-13 搜索会改为
     * {@code co-discovery} 的读模型投影，届时本方法会被替换，领域模型不受影响。</p>
     *
     * @param page 从 0 开始的页码
     * @param size 每页条数（1–100）
     */
    List<Question> findPage(int page, int size);

    long count();
}
