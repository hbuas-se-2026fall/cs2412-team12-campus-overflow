package com.campusoverflow.discovery.domain.repository;

import com.campusoverflow.discovery.domain.model.QuestionSummary;

import java.util.List;
import java.util.Optional;

/**
 * 问题摘要仓储（读模型）。
 *
 * <p>Lab 4 只立接口；Sprint 2 末的 US-03（问题列表）会用 JPA 实现，
 * Sprint 3 的 US-13（搜索）再换成带分词索引的实现。</p>
 */
public interface QuestionSummaryRepository {

    Optional<QuestionSummary> findByQuestionId(String questionId);

    List<QuestionSummary> findRecent(int limit);

    void save(QuestionSummary summary);

    void deleteByQuestionId(String questionId);
}
