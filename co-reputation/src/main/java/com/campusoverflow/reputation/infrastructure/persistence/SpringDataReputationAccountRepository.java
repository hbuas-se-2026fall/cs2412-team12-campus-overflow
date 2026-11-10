package com.campusoverflow.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 声誉账户 Spring Data 仓储。
 */
public interface SpringDataReputationAccountRepository extends JpaRepository<ReputationAccountJpaEntity, String> {
}
