package com.campusoverflow.bootstrap.integration;

import com.campusoverflow.identity.application.UserApplicationService;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.qa.domain.port.AskerVerifier;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Component;

/**
 * 问答上下文 ↔ 身份上下文的防腐层适配器（ACL）。
 *
 * <p>co-qa 只知道自己定义的 {@link AskerVerifier} 端口，编译期不依赖 co-identity；
 * 由装配层负责把 User 模型翻译成"能不能发布"这一个结论。</p>
 */
@Component
public class IdentityAskerVerifier implements AskerVerifier {

    private final UserApplicationService userApplicationService;

    public IdentityAskerVerifier(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @Override
    public void ensureCanPublish(UserId askerId) {
        User user = userApplicationService.find(askerId)
                .orElseThrow(() -> EntityNotFoundException.of("用户", askerId));
        // INV-U-02：未验证/已禁用的账号不能发布内容
        user.assertCanPublish();
    }
}
