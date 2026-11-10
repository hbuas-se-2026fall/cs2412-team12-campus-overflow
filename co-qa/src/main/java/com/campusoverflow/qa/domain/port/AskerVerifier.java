package com.campusoverflow.qa.domain.port;

import com.campusoverflow.shared.identity.UserId;

/**
 * 提问人校验端口：问答上下文不认识 User 聚合，只问身份上下文"这个人能不能发布内容"。
 *
 * <p>这是上下文映射里的防腐层（ACL，统一语言术语 #47）：
 * 实现放在装配层，把 co-identity 的模型翻译成"能/不能"这一个布尔结论，
 * 于是 co-qa 在编译期不依赖 co-identity。</p>
 */
public interface AskerVerifier {

    /**
     * @throws com.campusoverflow.shared.domain.DomainException 用户不存在（NOT_FOUND）或状态不允许发布（INV-U-02）
     */
    void ensureCanPublish(UserId askerId);
}
