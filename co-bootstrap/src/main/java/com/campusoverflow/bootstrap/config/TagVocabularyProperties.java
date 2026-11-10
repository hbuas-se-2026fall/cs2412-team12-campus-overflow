package com.campusoverflow.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 标签词表 SK-01 的配置项：{@code campus-overflow.tags.vocabulary}。
 *
 * <p>词表目前随配置发布（共享内核的内存实现）；Sprint 3 换成数据库实现时，配置项改为数据初始化脚本，
 * 领域代码不需要动。</p>
 */
@ConfigurationProperties(prefix = "campus-overflow.tags")
public record TagVocabularyProperties(List<String> vocabulary) {

    public TagVocabularyProperties {
        vocabulary = vocabulary == null ? List.of() : List.copyOf(vocabulary);
    }
}
