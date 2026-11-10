package com.campusoverflow.identity.domain.model;

import com.campusoverflow.identity.domain.service.PasswordEncoder;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User 聚合行为测试：只测聚合，不启动 Spring、不连数据库。
 *
 * <p>这正是"领域模型不依赖框架"的收益——每条不变式一个断言，跑一次不到 1 秒。</p>
 */
class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-09T02:00:00Z");
    private static final PasswordEncoder ENCODER = new FakePasswordEncoder();

    @Test
    @DisplayName("注册：邮箱归一化为小写，初始状态 pending，未验证不能发布（INV-U-02）")
    void registerCreatesPendingUser() {
        User user = newUser("Student@HBUAS.edu.cn", "abc12345");

        assertEquals("student@hbuas.edu.cn", user.email().value());
        assertEquals(UserStatus.PENDING, user.status());
        assertFalse(user.canPublish());

        InvariantViolationException ex = assertThrows(InvariantViolationException.class, user::assertCanPublish);
        assertEquals(UserInvariants.CANNOT_PUBLISH, ex.code());
        assertEquals(1, user.pullDomainEvents().size(), "注册应产生 AccountRegistered 事件");
    }

    @Test
    @DisplayName("注册：密码不足 8 位被拒绝（INV-U-03）")
    void rejectsShortPassword() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> newUser("a@b.com", "abc1234"));
        assertEquals(UserInvariants.PASSWORD_POLICY, ex.code());
    }

    @Test
    @DisplayName("注册：密码只有字母没有数字被拒绝（INV-U-03）")
    void rejectsPasswordWithoutDigit() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> newUser("a@b.com", "abcdefgh"));
        assertEquals(UserInvariants.PASSWORD_POLICY, ex.code());
    }

    @Test
    @DisplayName("邮箱验证后状态变 active，可以发布内容")
    void verifyEmailActivatesUser() {
        User user = newUser("a@b.com", "abc12345");
        user.pullDomainEvents();

        user.verifyEmail(NOW.plusSeconds(60));

        assertEquals(UserStatus.ACTIVE, user.status());
        assertTrue(user.canPublish());
        assertEquals(1, user.pullDomainEvents().size(), "验证应产生 EmailVerified 事件");
    }

    @Test
    @DisplayName("状态不可回退：已验证的账号不能再验证一次（INV-U-04）")
    void rejectsIllegalTransition() {
        User user = newUser("a@b.com", "abc12345");
        user.verifyEmail(NOW.plusSeconds(60));

        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> user.verifyEmail(NOW.plusSeconds(120)));
        assertEquals(UserInvariants.STATUS_TRANSITION, ex.code());
    }

    @Test
    @DisplayName("已禁用账号不能再验证或改昵称（INV-U-04）")
    void disabledUserIsTerminal() {
        User user = newUser("a@b.com", "abc12345");
        user.verifyEmail(NOW.plusSeconds(60));
        user.disable(NOW.plusSeconds(120), "灌水");

        assertFalse(user.canPublish());
        assertThrows(InvariantViolationException.class, () -> user.verifyEmail(NOW.plusSeconds(180)));
        assertThrows(InvariantViolationException.class,
                () -> user.changeNickname(Nickname.of("新名字"), NOW.plusSeconds(180)));
    }

    private static User newUser(String email, String password) {
        return User.register(UserId.newId(), Email.of(email), Nickname.of("测试同学"),
                password, ENCODER, NOW);
    }

    /** 测试替身：真正加密在基础设施层，领域测试只关心"有没有拿到哈希"。 */
    private static final class FakePasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(String rawPassword) {
            return "$2a$10$fake" + rawPassword.hashCode();
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
