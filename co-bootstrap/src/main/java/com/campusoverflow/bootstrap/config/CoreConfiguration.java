package com.campusoverflow.bootstrap.config;

import com.campusoverflow.bootstrap.infrastructure.event.SpringDomainEventPublisher;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.tag.InMemoryTagVocabulary;
import com.campusoverflow.shared.tag.TagVocabulary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 装配层配置：把共享内核需要的端口实现成 Bean。
 *
 * <p>领域模块不写 {@code @Configuration}，所有"选哪个实现"的决定集中在这里，方便按环境替换。</p>
 */
@Configuration
@EnableConfigurationProperties(TagVocabularyProperties.class)
public class CoreConfiguration {

    /** 统一时钟：测试里可以换成 Clock.fixed(...)，领域代码不直接调 Instant.now()。 */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TagVocabulary tagVocabulary(TagVocabularyProperties properties) {
        return new InMemoryTagVocabulary(properties.vocabulary());
    }

    @Bean
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher springPublisher) {
        return new SpringDomainEventPublisher(springPublisher);
    }
}
