package com.campusoverflow.bootstrap.infrastructure.tag;

import com.campusoverflow.shared.tag.Tag;
import com.campusoverflow.shared.tag.TagVocabulary;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库版标签词表（SK-01）：词表落 {@code tag} 表，US-10 起可扩展。
 *
 * <p>用 JPA 而不是 JdbcTemplate：实体由 Hibernate 统一建表，
 * 测试环境（H2 + ddl-auto）不需要额外维护一份 DDL。</p>
 */
public class JpaTagVocabulary implements TagVocabulary {

    private final SpringDataTagRepository repository;
    private final Clock clock;

    public JpaTagVocabulary(SpringDataTagRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public boolean contains(Tag tag) {
        return repository.existsById(tag.value());
    }

    @Override
    public Set<Tag> all() {
        List<Tag> tags = repository.findAll().stream()
                .map(entity -> Tag.of(entity.getCode()))
                .toList();
        return new LinkedHashSet<>(tags);
    }

    @Override
    public Tag registerIfAbsent(Tag tag) {
        if (!repository.existsById(tag.value())) {
            repository.save(new TagJpaEntity(tag.value(), tag.value(), clock.instant()));
        }
        return tag;
    }
}
