package com.campusoverflow.qa.application.view;

import com.campusoverflow.qa.domain.model.Question;

import java.util.List;

/**
 * 问题分页结果（应用层视图）。
 *
 * <p>刻意不用 Spring Data 的 {@code Page}：应用层不认识分页框架，接口层再把它翻译成 HTTP 契约。
 * 这样将来换成 {@code co-discovery} 的读模型查询时，本类型不用变。</p>
 */
public record QuestionPage(List<Question> items, int page, int size, long total) {

    public QuestionPage {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < total;
    }
}
