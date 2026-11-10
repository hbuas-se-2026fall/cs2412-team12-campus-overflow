package com.campusoverflow.identity.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（US-01）：只做"格式级"校验，业务规则仍在聚合里。
 */
public record RegisterUserRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不合法")
        @Size(max = 254, message = "邮箱不能超过 254 个字符")
        String email,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称不能超过 32 个字符")
        String nickname,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需在 8–64 之间")
        String password
) {
}
