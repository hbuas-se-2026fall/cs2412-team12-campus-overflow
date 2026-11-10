package com.campusoverflow.identity.domain.service;

/**
 * 密码编码端口：领域层只知道"能编码、能校验"，具体算法（bcrypt）在基础设施层实现。
 *
 * <p>放在 domain 层是刻意的：{@code User.register()} 需要在领域内校验密码策略（{@code INV-U-03}），
 * 但又不能依赖 Spring Security。</p>
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
