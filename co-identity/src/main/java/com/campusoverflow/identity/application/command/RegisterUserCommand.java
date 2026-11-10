package com.campusoverflow.identity.application.command;

/**
 * 注册账号命令（{@code RegisterAccount}，对应 US-01）。
 */
public record RegisterUserCommand(String email, String nickname, String rawPassword) {
}
