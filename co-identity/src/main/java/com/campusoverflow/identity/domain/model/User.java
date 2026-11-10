package com.campusoverflow.identity.domain.model;

import com.campusoverflow.identity.domain.event.AccountRegistered;
import com.campusoverflow.identity.domain.event.EmailVerified;
import com.campusoverflow.identity.domain.event.UserDisabled;
import com.campusoverflow.identity.domain.service.PasswordEncoder;
import com.campusoverflow.shared.domain.AggregateRoot;
import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.identity.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * 用户聚合根（Lab 3 AGG-01，所属上下文：身份与访问）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>所有字段私有且 <b>没有 setter</b>，状态只能通过业务方法改变；</li>
 *   <li>构造走工厂方法 {@link #register}，构造即满足 {@code INV-U-03}（密码策略）与 {@code INV-U-04}（初始 pending）；</li>
 *   <li>状态流转由 {@link UserStatus#canTransitTo} 守卫，非法流转抛 {@code INV-U-04}；</li>
 *   <li>能否发布内容由 {@link #assertCanPublish()} 判定（{@code INV-U-02}），问答上下文通过端口调用它。</li>
 * </ul>
 */
public final class User extends AggregateRoot {

    private final UserId id;
    private final Email email;
    private final Instant createdAt;

    private Nickname nickname;
    private PasswordHash passwordHash;
    private UserStatus status;
    private Instant updatedAt;

    private User(UserId id, Email email, Nickname nickname, PasswordHash passwordHash,
                 UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.email = Objects.requireNonNull(email, "email 不能为空");
        this.nickname = Objects.requireNonNull(nickname, "nickname 不能为空");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash 不能为空");
        this.status = Objects.requireNonNull(status, "status 不能为空");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /**
     * 注册新账号（{@code RegisterAccount} 命令）：初始状态 pending，并登记 {@code AccountRegistered} 事件。
     */
    public static User register(UserId id, Email email, Nickname nickname,
                                String rawPassword, PasswordEncoder encoder, Instant now) {
        assertPasswordPolicy(rawPassword);
        PasswordHash hash = PasswordHash.of(encoder.encode(rawPassword));
        User user = new User(id, email, nickname, hash, UserStatus.PENDING, now, now);
        user.registerEvent(new AccountRegistered(user.id, user.email.value(), now));
        return user;
    }

    /**
     * 从持久化状态重建聚合：只给基础设施层用，不登记任何事件，也不做业务校验
     * （历史数据按"当时"的规则已合法）。
     */
    public static User rehydrate(UserId id, Email email, Nickname nickname, PasswordHash passwordHash,
                                 UserStatus status, Instant createdAt, Instant updatedAt) {
        return new User(id, email, nickname, passwordHash, status, createdAt, updatedAt);
    }

    /** 邮箱验证（{@code VerifyEmail}）：pending → active。 */
    public void verifyEmail(Instant now) {
        transitTo(UserStatus.ACTIVE, now, "邮箱验证");
        registerEvent(new EmailVerified(id, email.value(), now));
    }

    /** 禁用账号（{@code DisableUser}）：active → disabled，不可回退。 */
    public void disable(Instant now, String reason) {
        transitTo(UserStatus.DISABLED, now, "禁用账号");
        registerEvent(new UserDisabled(id, reason, now));
    }

    /** 修改昵称：已禁用账号不允许修改。 */
    public void changeNickname(Nickname newNickname, Instant now) {
        if (status == UserStatus.DISABLED) {
            throw new InvariantViolationException(UserInvariants.STATUS_TRANSITION, "已禁用的账号不能修改昵称");
        }
        this.nickname = Objects.requireNonNull(newNickname, "nickname 不能为空");
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    /** 修改密码：仍然走密码策略校验。 */
    public void changePassword(String rawPassword, PasswordEncoder encoder, Instant now) {
        if (status == UserStatus.DISABLED) {
            throw new InvariantViolationException(UserInvariants.STATUS_TRANSITION, "已禁用的账号不能修改密码");
        }
        assertPasswordPolicy(rawPassword);
        this.passwordHash = PasswordHash.of(encoder.encode(rawPassword));
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    /** {@code INV-U-02}：只有 active 账号可以发布问题/回答。 */
    public boolean canPublish() {
        return status == UserStatus.ACTIVE;
    }

    public void assertCanPublish() {
        if (!canPublish()) {
            throw new InvariantViolationException(UserInvariants.CANNOT_PUBLISH,
                    "账号当前状态为 " + status.code() + "，请先完成邮箱验证再发布内容");
        }
    }

    private void transitTo(UserStatus target, Instant now, String action) {
        if (!status.canTransitTo(target)) {
            throw new InvariantViolationException(UserInvariants.STATUS_TRANSITION,
                    "非法状态流转：" + action + " 不允许 " + status.code() + " → " + target.code());
        }
        this.status = target;
        this.updatedAt = Objects.requireNonNull(now, "now 不能为空");
    }

    /** {@code INV-U-03}：≥ 8 位，且同时包含字母与数字。 */
    private static void assertPasswordPolicy(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new InvariantViolationException(UserInvariants.PASSWORD_POLICY, "密码至少 8 位");
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new InvariantViolationException(UserInvariants.PASSWORD_POLICY, "密码必须同时包含字母与数字");
        }
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public Nickname nickname() {
        return nickname;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
