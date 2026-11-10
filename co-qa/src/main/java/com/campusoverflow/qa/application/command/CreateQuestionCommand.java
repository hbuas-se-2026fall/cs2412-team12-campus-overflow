package com.campusoverflow.qa.application.command;

import java.util.List;

/**
 * 发布问题命令（{@code PostQuestion}，对应 US-04）。
 *
 * <p>{@code askerId} 目前由请求体显式传入：Lab 4 尚未接入登录鉴权（US-02 在 Sprint 2 末），
 * 接入后它会由网关/会话解析，命令对象不需要改。</p>
 */
public record CreateQuestionCommand(String askerId, String title, String body, List<String> tags) {

    public CreateQuestionCommand {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
