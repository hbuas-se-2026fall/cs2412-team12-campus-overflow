package com.campusoverflow.qa.interfaces.rest.dto;

import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.shared.tag.Tag;

import java.time.Instant;
import java.util.List;

/**
 * 问题响应：对外只暴露契约字段，不直接暴露领域对象（避免将来重构聚合时破坏 API）。
 */
public record QuestionResponse(
        String id,
        String askerId,
        String title,
        String body,
        List<String> tags,
        String status,
        String acceptedAnswerId,
        int voteCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.id().asString(),
                question.askerId().asString(),
                question.title().value(),
                question.body().value(),
                question.tags().values().stream().map(Tag::value).toList(),
                question.status().code(),
                question.acceptedAnswerId().map(a -> a.asString()).orElse(null),
                question.voteCount(),
                question.createdAt(),
                question.updatedAt());
    }
}
