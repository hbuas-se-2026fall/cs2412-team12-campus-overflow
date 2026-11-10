package com.campusoverflow.reputation.domain.repository;

import com.campusoverflow.reputation.domain.model.ReputationAccount;
import com.campusoverflow.shared.identity.UserId;

import java.util.Optional;

/**
 * 声誉账户仓储：接口先立在 domain 层，JPA 实现推到 US-11 的 Sprint 3。
 */
public interface ReputationAccountRepository {

    Optional<ReputationAccount> findByUserId(UserId userId);

    ReputationAccount save(ReputationAccount account);
}
