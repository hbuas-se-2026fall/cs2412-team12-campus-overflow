package com.campusoverflow.identity.interfaces.rest;

import com.campusoverflow.identity.application.UserApplicationService;
import com.campusoverflow.identity.application.command.RegisterUserCommand;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.identity.interfaces.rest.dto.RegisterUserRequest;
import com.campusoverflow.identity.interfaces.rest.dto.UserResponse;
import com.campusoverflow.shared.identity.UserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 身份接口层：只做 HTTP 协议适配（DTO 转换、状态码、Location），不做业务判断。
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /** US-01：注册账号（初始状态 pending）。 */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        User user = userApplicationService.register(
                new RegisterUserCommand(request.email(), request.nickname(), request.password()));
        var location = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(user.id().asString()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    /**
     * 模拟"点击邮件里的验证链接"：pending → active（{@code INV-U-04}）。
     *
     * <p>真实实现里 token 会随注册邮件发出、在这里校验；Lab 4 先把状态机跑通，
     * 令牌表属于 US-01 的后续任务。</p>
     */
    @PostMapping("/{id}/email-verification")
    public UserResponse verifyEmail(@PathVariable("id") String id) {
        return UserResponse.from(userApplicationService.verifyEmail(UserId.of(id)));
    }

    /** 便于 Lab 4 验证与后续 US-02 登录使用。 */
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable("id") String id) {
        return UserResponse.from(userApplicationService.get(UserId.of(id)));
    }
}
