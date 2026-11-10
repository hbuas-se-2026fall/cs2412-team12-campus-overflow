package com.campusoverflow.shared.tag;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 共享内核的参考实现：内存版标签词表。
 *
 * <p>由装配层用 {@code campus-overflow.tags.vocabulary} 配置项初始化，不依赖任何框架，
 * 因此单元测试可以直接 {@code new InMemoryTagVocabulary(Set.of(...))}。</p>
 */
public final class InMemoryTagVocabulary implements TagVocabulary {

    private final Set<Tag> tags;

    public InMemoryTagVocabulary(Collection<String> rawTags) {
        Set<Tag> parsed = new LinkedHashSet<>();
        if (rawTags != null) {
            rawTags.forEach(raw -> parsed.add(Tag.of(raw)));
        }
        this.tags = Set.copyOf(parsed);
    }

    @Override
    public boolean contains(Tag tag) {
        return tags.contains(tag);
    }

    @Override
    public Set<Tag> all() {
        return tags;
    }
}
