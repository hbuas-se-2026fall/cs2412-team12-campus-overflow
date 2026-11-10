package com.campusoverflow.qa.interfaces.rest.dto;

import com.campusoverflow.qa.application.view.QuestionPage;

import java.util.List;

/**
 * 问题列表响应（US-03 的最小实现）。
 *
 * <p>{@code hasNext} 让前端不必自己算"还有没有下一页"，分页参数的边界由服务端兜住。</p>
 */
public record QuestionPageResponse(
        List<QuestionResponse> items,
        int page,
        int size,
        long total,
        boolean hasNext
) {

    public static QuestionPageResponse from(QuestionPage page) {
        return new QuestionPageResponse(
                page.items().stream().map(QuestionResponse::from).toList(),
                page.page(),
                page.size(),
                page.total(),
                page.hasNext());
    }
}
