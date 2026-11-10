package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.campusoverflow.shared.tag.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 问题仓储实现：领域层只看到 {@link QuestionRepository}。
 *
 * <p>保存时同步 {@code question_tag} 派生索引（US-10 按标签筛选用）。
 * 索引是投影，重建只需把 question 表全量扫一遍重写。</p>
 */
@Repository
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final SpringDataQuestionRepository jpa;
    private final SpringDataQuestionTagRepository tagJpa;

    public QuestionRepositoryAdapter(SpringDataQuestionRepository jpa,
                                     SpringDataQuestionTagRepository tagJpa) {
        this.jpa = jpa;
        this.tagJpa = tagJpa;
    }

    @Override
    public Optional<Question> findById(QuestionId id) {
        return jpa.findById(id.asString()).map(QuestionPersistenceMapper::toDomain);
    }

    @Override
    public Question save(Question question) {
        QuestionJpaEntity entity = jpa.findById(question.id().asString())
                .orElseGet(() -> new QuestionJpaEntity(question.id().asString()));
        QuestionPersistenceMapper.apply(question, entity);
        QuestionJpaEntity saved = jpa.save(entity);
        syncTagIndex(question);
        return QuestionPersistenceMapper.toDomain(saved);
    }

    private void syncTagIndex(Question question) {
        tagJpa.deleteByQuestionId(question.id().asString());
        for (Tag tag : question.tags().values()) {
            tagJpa.save(new QuestionTagJpaEntity(question.id().asString(), tag.value()));
        }
    }

    @Override
    public boolean existsById(QuestionId id) {
        return jpa.existsById(id.asString());
    }

    @Override
    public List<Question> findPage(int page, int size) {
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(QuestionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Question> findPageByTag(String tag, int page, int size) {
        return jpa.findByTagOrderByCreatedAtDesc(tag, page * size, size).stream()
                .map(QuestionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public long countByTag(String tag) {
        return jpa.countByTag(tag);
    }
}
