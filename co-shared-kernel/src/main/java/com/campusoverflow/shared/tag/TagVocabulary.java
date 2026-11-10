package com.campusoverflow.shared.tag;

import java.util.Set;

/**
 * 标签词表 SK-01（统一语言术语 #13）：问答核心与发现上下文共同拥有的只读词表。
 *
 * <p>共享内核定义契约，具体实现由装配层注入（当前是内存实现 {@link InMemoryTagVocabulary}，
 * Sprint 3 的 US-10 会换成数据库实现，接口不变）。</p>
 */
public interface TagVocabulary {

    boolean contains(Tag tag);

    Set<Tag> all();
}
