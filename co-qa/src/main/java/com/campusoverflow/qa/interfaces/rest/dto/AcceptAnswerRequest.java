package com.campusoverflow.qa.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 采纳答案请求（US-08）。{@code operatorId} 后续由会话解析，Lab 4 先显式传入以便验证 {@code INV-Q-03}。
 */
public record AcceptAnswerRequest(
        @NotBlank(message = "answerId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "answerId 必须是 UUID")
        String answerId,

        @NotBlank(message = "operatorId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "operatorId 必须是 UUID")
        String operatorId
) {
}
