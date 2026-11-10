package com.campusoverflow.identity.interfaces.rest.dto;

import com.campusoverflow.identity.domain.model.User;

import java.time.Instant;

/**
 * 注册响应：绝不返回密码哈希。
 */
public record UserResponse(
        String id,
        String email,
        String nickname,
        String status,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().asString(),
                user.email().value(),
                user.nickname().value(),
                user.status().code(),
                user.createdAt());
    }
}
