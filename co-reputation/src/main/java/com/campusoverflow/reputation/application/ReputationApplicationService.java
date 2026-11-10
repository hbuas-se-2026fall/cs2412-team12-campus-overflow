package com.campusoverflow.reputation.application;

import com.campusoverflow.reputation.application.view.ReputationView;
import com.campusoverflow.reputation.domain.model.ReputationAccount;
import com.campusoverflow.reputation.domain.model.ReputationScore;
import com.campusoverflow.reputation.domain.model.RuleCode;
import com.campusoverflow.reputation.domain.repository.ReputationAccountRepository;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 声誉应用服务（US-11 / US-12）。
 *
 * <p>事件在源事务提交后才由装配层的监听器调用到这里；这里用 {@code REQUIRES_NEW}
 * 开启独立事务，保证"投票提交成功但声誉加分失败"不会回滚投票本身（最终一致）。</p>
 */
@Service
public class ReputationApplicationService {

    private final ReputationAccountRepository repository;

    public ReputationApplicationService(ReputationAccountRepository repository) {
        this.repository = repository;
    }

    /** 按规则码固定分值计分。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void apply(String userId, String eventId, RuleCode ruleCode, Instant occurredAt) {
        applyDelta(userId, eventId, ruleCode, ruleCode.delta(), occurredAt);
    }

    /** 按显式分值计分（投票改判等动态场景）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyDelta(String userId, String eventId, RuleCode ruleCode, int delta, Instant occurredAt) {
        UserId id = UserId.of(userId);
        ReputationAccount account = repository.findByUserId(id)
                .orElseGet(() -> ReputationAccount.open(id));
        if (account.applyDelta(eventId, ruleCode, delta, occurredAt)) {
            repository.save(account);
        }
    }

    @Transactional(readOnly = true)
    public ReputationView view(String userId) {
        return repository.findByUserId(UserId.of(userId))
                .map(ReputationView::of)
                .orElseGet(() -> ReputationView.initial(userId));
    }

    /** 便于测试断言：直接返回聚合。 */
    @Transactional(readOnly = true)
    public ReputationScore scoreOf(String userId) {
        return repository.findByUserId(UserId.of(userId))
                .map(ReputationAccount::score)
                .orElseGet(ReputationScore::initial);
    }
}
