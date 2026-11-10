package com.campusoverflow.qa.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 提交回答请求（US-06）。
 */
public record CreateAnswerRequest(
        @NotBlank(message = "questionId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "questionId 必须是 UUID")
        String questionId,

        @NotBlank(message = "authorId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "authorId 必须是 UUID")
        String authorId,

        @NotBlank(message = "回答内容不能为空")
        @Size(max = 10000, message = "回答不能超过 10000 字")
        String body
) {
}
