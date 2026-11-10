package com.campusoverflow.qa.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发布问题请求（US-04）。
 *
 * <p>这里只做"格式级"校验（和前端保持一致的第一道防线）；长度语义、标签词表、权限等业务规则
 * 仍然由 {@code Question} 聚合与 {@code TagSet} 守卫——避免把规则写两遍、改一处漏一处。</p>
 */
public record CreateQuestionRequest(
        @NotBlank(message = "askerId 不能为空")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                message = "askerId 必须是 UUID")
        String askerId,

        @NotBlank(message = "标题不能为空")
        @Size(min = 5, max = 150, message = "标题长度必须在 5–150 字之间")
        String title,

        @NotBlank(message = "正文不能为空")
        @Size(max = 20000, message = "正文不能超过 20000 字")
        String body,

        @Size(max = 5, message = "最多只能打 5 个标签")
        List<@NotNull @Size(min = 2, max = 20, message = "标签长度必须在 2–20 字之间") String> tags
) {
}
