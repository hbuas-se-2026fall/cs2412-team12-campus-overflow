package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.AnswerId;
import com.campusoverflow.qa.domain.model.AnswerStatus;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.shared.identity.UserId;

import java.util.Locale;

/**
 * 回答：领域对象 ↔ 持久化对象。
 */
final class AnswerPersistenceMapper {

    private AnswerPersistenceMapper() {
    }

    static void apply(Answer answer, AnswerJpaEntity entity) {
        entity.apply(
                answer.questionId().asString(),
                answer.authorId().asString(),
                answer.body().value(),
                answer.status().code(),
                answer.isAccepted(),
                answer.voteCount(),
                answer.createdAt(),
                answer.updatedAt());
    }

    static Answer toDomain(AnswerJpaEntity entity) {
        return Answer.rehydrate(
                AnswerId.of(entity.getId()),
                QuestionId.of(entity.getQuestionId()),
                UserId.of(entity.getAuthorId()),
                new Body(entity.getBody()),
                AnswerStatus.valueOf(entity.getStatus().toUpperCase(Locale.ROOT)),
                entity.isAccepted(),
                entity.getVoteCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
