package com.campusoverflow.shared.tag;

import java.util.Set;

/**
 * 标签词表 SK-01（统一语言术语 #13）：问答核心与发现上下文共同拥有的词表。
 *
 * <p>Sprint 3（US-10）起词表从"只读白名单"演进为"可扩展词表"：
 * {@link #registerIfAbsent} 允许发帖时自动登记新标签，落库实现由装配层注入。
 * 默认实现 {@link InMemoryTagVocabulary} 仅供单元测试与本地演示。</p>
 */
public interface TagVocabulary {

    boolean contains(Tag tag);

    Set<Tag> all();

    /**
     * 登记一个标签（已存在则原样返回），用于 US-10 的"不存在的新标签自动创建"。
     */
    Tag registerIfAbsent(Tag tag);
}
