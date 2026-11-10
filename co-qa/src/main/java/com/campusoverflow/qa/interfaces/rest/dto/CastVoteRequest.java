package com.campusoverflow.qa.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 投票请求（US-07）：targetType = question / answer，value = up / down。
 */
public record CastVoteRequest(
        @NotBlank(message = "targetType 不能为空")
        @Pattern(regexp = "(?i)question|answer", message = "targetType 只能是 question 或 answer")
        String targetType,

        @NotBlank(message = "targetId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "targetId 必须是 UUID")
        String targetId,

        @NotBlank(message = "voterId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "voterId 必须是 UUID")
        String voterId,

        @NotBlank(message = "value 不能为空")
        @Pattern(regexp = "(?i)up|down", message = "value 只能是 up 或 down")
        String value
) {
}
