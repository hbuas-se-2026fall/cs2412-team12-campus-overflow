package com.campusoverflow.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 装配层启动入口（Lab 4 · Step 1 的"co-bootstrap 声明启动入口"）。
 *
 * <p>这里只做三件事：声明扫描范围、启动 Spring、把各上下文的 Bean 装到一起；
 * 任何业务规则都不允许写在这个模块里。</p>
 */
@SpringBootApplication(scanBasePackages = "com.campusoverflow")
@EntityScan(basePackages = "com.campusoverflow")
@EnableJpaRepositories(basePackages = "com.campusoverflow")
public class CampusOverflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusOverflowApplication.class, args);
    }
}
