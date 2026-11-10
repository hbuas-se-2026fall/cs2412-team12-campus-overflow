package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.AcceptedAnswerId;
import com.campusoverflow.qa.domain.model.Body;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.model.QuestionStatus;
import com.campusoverflow.qa.domain.model.TagSet;
import com.campusoverflow.qa.domain.model.Title;
import com.campusoverflow.shared.identity.UserId;

import java.util.Locale;

/**
 * 问题：领域对象 ↔ 持久化对象。
 */
final class QuestionPersistenceMapper {

    private QuestionPersistenceMapper() {
    }

    static void apply(Question question, QuestionJpaEntity entity) {
        entity.apply(
                question.askerId().asString(),
                question.title().value(),
                question.body().value(),
                question.tags().asCsv(),
                question.acceptedAnswerId().map(AcceptedAnswerId::asString).orElse(null),
                // 库里存小写（与 DDL 注释一致），枚举名只用于代码内部
                question.status().code(),
                question.voteCount(),
                question.createdAt(),
                question.updatedAt());
    }

    static Question toDomain(QuestionJpaEntity entity) {
        return Question.rehydrate(
                QuestionId.of(entity.getId()),
                UserId.of(entity.getAskerId()),
                new Title(entity.getTitle()),
                new Body(entity.getBody()),
                TagSet.fromCsv(entity.getTags()),
                entity.getAcceptedAnswerId() == null ? null : AcceptedAnswerId.of(entity.getAcceptedAnswerId()),
                QuestionStatus.valueOf(entity.getStatus().toUpperCase(Locale.ROOT)),
                entity.getVoteCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
