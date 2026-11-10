package com.campusoverflow.identity.application;

import com.campusoverflow.identity.application.command.RegisterUserCommand;
import com.campusoverflow.identity.application.exception.DuplicateEmailException;
import com.campusoverflow.identity.domain.model.Email;
import com.campusoverflow.identity.domain.model.Nickname;
import com.campusoverflow.identity.domain.model.User;
import com.campusoverflow.identity.domain.repository.UserRepository;
import com.campusoverflow.identity.domain.service.PasswordEncoder;
import com.campusoverflow.shared.domain.DomainEventPublisher;
import com.campusoverflow.shared.domain.EntityNotFoundException;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 身份应用服务：编排用例（注册 / 验证邮箱 / 查询），不承载业务规则。
 *
 * <p>业务规则全部在 {@link User} 聚合内；这里只负责：查重、装配依赖、保存、投递领域事件。</p>
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public UserApplicationService(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  DomainEventPublisher eventPublisher,
                                  Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** US-01 注册：邮箱唯一（INV-U-01）+ 密码策略（INV-U-03）+ 初始 pending（INV-U-04）。 */
    @Transactional
    public User register(RegisterUserCommand command) {
        Email email = Email.of(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = User.register(UserId.newId(), email, Nickname.of(command.nickname()),
                command.rawPassword(), passwordEncoder, now());
        User saved = userRepository.save(user);
        // 事件挂在"刚计算的聚合"上，不是仓储重建出来的新实例上（后者没有事件）
        eventPublisher.publish(user.pullDomainEvents());
        return saved;
    }

    /** 邮箱验证：pending → active。 */
    @Transactional
    public User verifyEmail(UserId userId) {
        User user = get(userId);
        user.verifyEmail(now());
        User saved = userRepository.save(user);
        eventPublisher.publish(user.pullDomainEvents());
        return saved;
    }

    /**
     * 统一时间精度：数据库是 DATETIME(6)（微秒），先在应用层截断，
     * 避免"刚创建返回纳秒、重新查询返回微秒"导致前端比对不上。
     */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    @Transactional(readOnly = true)
    public User get(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.of("用户", userId));
    }

    @Transactional(readOnly = true)
    public Optional<User> find(UserId userId) {
        return userRepository.findById(userId);
    }
}
