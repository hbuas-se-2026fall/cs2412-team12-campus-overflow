package com.campusoverflow.qa.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 评论请求（US-09）。
 */
public record CreateCommentRequest(
        @NotBlank(message = "targetType 不能为空")
        @Pattern(regexp = "(?i)question|answer", message = "targetType 只能是 question 或 answer")
        String targetType,

        @NotBlank(message = "targetId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "targetId 必须是 UUID")
        String targetId,

        @NotBlank(message = "authorId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "authorId 必须是 UUID")
        String authorId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论不能超过 500 字")
        String body
) {
}
