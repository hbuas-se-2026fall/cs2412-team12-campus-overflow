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

    @Test
    @DisplayName("修改密码：校验策略后重新编码，禁用账号被拒绝")
    void changePasswordRules() {
        User user = newUser("pwd@hbuas.edu.cn", "abc12345");
        user.verifyEmail(NOW.plusSeconds(60));

        user.changePassword("newpass123", ENCODER, NOW.plusSeconds(120));
        assertTrue(user.passwordHash().value().startsWith("$2a$10$"));

        assertThrows(InvariantViolationException.class,
                () -> user.changePassword("short1", ENCODER, NOW.plusSeconds(180)));

        user.disable(NOW.plusSeconds(240), "违规");
        assertThrows(InvariantViolationException.class,
                () -> user.changePassword("newpass456", ENCODER, NOW.plusSeconds(300)));
    }

    @Test
    @DisplayName("rehydrate 保留全部状态，且不产生新事件")
    void rehydrateKeepsState() {
        User user = User.rehydrate(UserId.newId(), Email.of("re@hbuas.edu.cn"), Nickname.of("回放"),
                PasswordHash.of("$2a$10$hash"), UserStatus.ACTIVE, NOW, NOW.plusSeconds(60));

        assertEquals(UserStatus.ACTIVE, user.status());
        assertTrue(user.canPublish());
        assertTrue(user.pullDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("密码哈希：只接受看起来像 bcrypt 的编码值")
    void passwordHashDetection() {
        assertTrue(PasswordHash.of("$2a$10$abcdef").looksEncoded());
        assertFalse(PasswordHash.of("plain-text").looksEncoded());
        assertEquals("PasswordHash{***}", PasswordHash.of("$2a$10$x").toString());
        assertThrows(InvariantViolationException.class, () -> PasswordHash.of("  "));
    }

    @Test
    @DisplayName("邮箱与昵称的长度/格式边界")
    void emailAndNicknameBoundaries() {
        assertThrows(InvariantViolationException.class, () -> Email.of("not-an-email"));
        assertThrows(InvariantViolationException.class,
                () -> Email.of("a".repeat(250) + "@x.com"));
        assertThrows(InvariantViolationException.class, () -> Nickname.of("长".repeat(33)));
        assertEquals("昵称", Nickname.of("  昵称  ").value());
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
