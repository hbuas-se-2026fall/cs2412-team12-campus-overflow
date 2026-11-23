package com.campusoverflow.reputation.infrastructure.persistence;

import com.campusoverflow.reputation.domain.model.ReputationAccount;
import com.campusoverflow.reputation.domain.model.ReputationEntry;
import com.campusoverflow.reputation.domain.model.ReputationScore;
import com.campusoverflow.reputation.domain.model.RuleCode;
import com.campusoverflow.reputation.domain.repository.ReputationAccountRepository;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 声誉仓储实现：账户一行 + 明细多行。
 *
 * <p>保存时只插入"库里还没有"的明细（按 {@code eventId} 判断），
 * 这样即使同一事件被处理两次，也不会重复计分。</p>
 */
@Repository
public class ReputationRepositoryAdapter implements ReputationAccountRepository {

    private final SpringDataReputationAccountRepository accountJpa;
    private final SpringDataReputationEntryRepository entryJpa;

    public ReputationRepositoryAdapter(SpringDataReputationAccountRepository accountJpa,
                                       SpringDataReputationEntryRepository entryJpa) {
        this.accountJpa = accountJpa;
        this.entryJpa = entryJpa;
    }

    @Override
    public Optional<ReputationAccount> findByUserId(UserId userId) {
        return accountJpa.findById(userId.asString()).map(account -> {
            Map<String, ReputationEntry> entries = new LinkedHashMap<>();
            for (ReputationEntryJpaEntity entity : entryJpa.findByUserIdOrderByCreatedAtAsc(userId.asString())) {
                entries.put(entity.getEventId(), new ReputationEntry(
                        entity.getEventId(),
                        RuleCode.valueOf(entity.getRuleCode()),
                        entity.getDelta(),
                        entity.getCreatedAt()));
            }
            return ReputationAccount.rehydrate(userId,
                    new ReputationScore(account.getScore()), entries);
        });
    }

    @Override
    public ReputationAccount save(ReputationAccount account) {
        String userId = account.userId().asString();
        Instant updatedAt = account.entries().values().stream()
                .map(ReputationEntry::occurredAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.now());

        ReputationAccountJpaEntity accountEntity = accountJpa.findById(userId)
                .orElseGet(() -> new ReputationAccountJpaEntity(userId));
        accountEntity.apply(account.score().value(), updatedAt);
        accountJpa.save(accountEntity);

        for (ReputationEntry entry : account.entries().values()) {
            if (!entryJpa.existsByEventId(entry.eventId())) {
                entryJpa.save(new ReputationEntryJpaEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        entry.eventId(),
                        entry.ruleCode().code(),
                        entry.delta(),
                        entry.occurredAt()));
            }
        }
        return account;
    }
}
