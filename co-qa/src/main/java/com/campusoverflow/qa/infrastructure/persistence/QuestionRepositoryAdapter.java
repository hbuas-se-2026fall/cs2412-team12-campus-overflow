package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 问题仓储实现：领域层只看到 {@link QuestionRepository}。
 */
@Repository
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final SpringDataQuestionRepository jpa;

    public QuestionRepositoryAdapter(SpringDataQuestionRepository jpa) {
        this.jpa = jpa;
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
        return QuestionPersistenceMapper.toDomain(jpa.save(entity));
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
    public long count() {
        return jpa.count();
    }
}
