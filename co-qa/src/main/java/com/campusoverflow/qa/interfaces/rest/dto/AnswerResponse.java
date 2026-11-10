package com.campusoverflow.qa.interfaces.rest.dto;

import com.campusoverflow.qa.domain.model.Answer;

import java.time.Instant;

/**
 * 回答响应。
 */
public record AnswerResponse(
        String id,
        String questionId,
        String authorId,
        String body,
        String status,
        boolean accepted,
        int voteCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
                answer.id().asString(),
                answer.questionId().asString(),
                answer.authorId().asString(),
                answer.body().value(),
                answer.status().code(),
                answer.isAccepted(),
                answer.voteCount(),
                answer.createdAt(),
                answer.updatedAt());
    }
}
