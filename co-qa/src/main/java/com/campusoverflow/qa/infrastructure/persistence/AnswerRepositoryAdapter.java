package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.repository.AnswerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 回答仓储实现。
 */
@Repository
public class AnswerRepositoryAdapter implements AnswerRepository {

    private final SpringDataAnswerRepository jpa;

    public AnswerRepositoryAdapter(SpringDataAnswerRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Answer> findById(AnswerId id) {
        return jpa.findById(id.asString()).map(AnswerPersistenceMapper::toDomain);
    }

    @Override
    public List<Answer> findByQuestionId(QuestionId questionId) {
        return jpa.findByQuestionIdOrderByCreatedAtAsc(questionId.asString()).stream()
                .map(AnswerPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Answer save(Answer answer) {
        AnswerJpaEntity entity = jpa.findById(answer.id().asString())
                .orElseGet(() -> new AnswerJpaEntity(answer.id().asString()));
        AnswerPersistenceMapper.apply(answer, entity);
        return AnswerPersistenceMapper.toDomain(jpa.save(entity));
    }

    @Override
    public void delete(AnswerId id) {
        jpa.deleteById(id.asString());
    }
}
