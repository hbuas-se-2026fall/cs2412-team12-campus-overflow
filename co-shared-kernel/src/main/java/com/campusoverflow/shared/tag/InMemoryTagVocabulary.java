package com.campusoverflow.shared.tag;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 内存版标签词表：只用于单元测试与本地演示。
 *
 * <p>生产实现是装配层的 {@code JdbcTagVocabulary}（词表落 {@code tag} 表）。</p>
 */
public final class InMemoryTagVocabulary implements TagVocabulary {

    private final Set<Tag> tags = new LinkedHashSet<>();

    public InMemoryTagVocabulary(Collection<String> rawTags) {
        if (rawTags != null) {
            rawTags.forEach(raw -> tags.add(Tag.of(raw)));
        }
    }

    @Override
    public boolean contains(Tag tag) {
        return tags.contains(tag);
    }

    @Override
    public Set<Tag> all() {
        return Set.copyOf(tags);
    }

    @Override
    public Tag registerIfAbsent(Tag tag) {
        tags.add(tag);
        return tag;
    }
}
