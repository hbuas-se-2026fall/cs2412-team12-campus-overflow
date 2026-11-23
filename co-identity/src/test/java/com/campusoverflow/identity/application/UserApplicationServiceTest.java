package com.campusoverflow.identity.application;

import com.campusoverflow.identity.application.command.RegisterUserCommand;
import com.campusoverflow.identity.application.exception.DuplicateEmailException;
import com.campusoverflow.identity.domain.event.AccountRegistered;
import com.campusoverflow.identity.domain.event.EmailVerified;
import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.Nickname;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.identity.domain.model.UserStatus;
import com.campusoverflow.identity.domain.repository.UserRepository;
import com.campusoverflow.identity.domain.service.PasswordEncoder;
import com.campusoverflow.shared.domain.DomainEvent;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身份应用服务单元测试：用 Mockito 隔离仓储与编码器，
 * 用 {@link ArgumentCaptor} 验证"发布领域事件"这一副作用（Lab 7 Step 1 要求）。
 */
@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T00:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DomainEventPublisher eventPublisher;

    private UserApplicationService service;

    @BeforeEach
    void setUp() {
        service = new UserApplicationService(userRepository, passwordEncoder, eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("注册：保存 pending 用户，并发布 AccountRegistered 事件")
    void registerPublishesAccountRegistered() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordEncoder.encode("abc12345")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.register(new RegisterUserCommand("New@HBUAS.edu.cn", "新同学", "abc12345"));

        assertEquals(UserStatus.PENDING, user.status());
        assertEquals("new@hbuas.edu.cn", user.email().value());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        AccountRegistered event = (AccountRegistered) captor.getValue().get(0);
        assertEquals(user.id(), event.userId());
        assertEquals("new@hbuas.edu.cn", event.email());
    }

    @Test
    @DisplayName("INV-U-01：邮箱已存在时拒绝注册，且不保存、不发事件")
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterUserCommand("dup@hbuas.edu.cn", "重复", "abc12345")));

        assertEquals("INV-U-01", ex.code());
        verify(userRepository, never()).save(any(User.class));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("邮箱验证：pending → active，并发布 EmailVerified 事件")
    void verifyEmailPublishesEvent() {
        when(passwordEncoder.encode("abc12345")).thenReturn("$2a$10$hash");
        User pending = User.register(UserId.newId(), Email.of("a@hbuas.edu.cn"),
                Nickname.of("待验证"), "abc12345", passwordEncoder, NOW);
        pending.pullDomainEvents(); // 清掉注册事件，只验证"验证邮箱"产生的事件
        when(userRepository.findById(pending.id())).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User verified = service.verifyEmail(pending.id());

        assertEquals(UserStatus.ACTIVE, verified.status());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publish(captor.capture());
        EmailVerified event = (EmailVerified) captor.getValue().get(0);
        assertEquals(pending.id(), event.userId());
    }

    @Test
    @DisplayName("查询不存在的用户抛 EntityNotFoundException")
    void getMissingUserThrows() {
        UserId id = UserId.newId();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.get(id));
        assertEquals("NOT_FOUND", ex.code());
    }

    @Test
    @DisplayName("find 返回空 Optional 而不是抛异常")
    void findReturnsEmpty() {
        UserId id = UserId.newId();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertTrue(service.find(id).isEmpty());
    }
}
