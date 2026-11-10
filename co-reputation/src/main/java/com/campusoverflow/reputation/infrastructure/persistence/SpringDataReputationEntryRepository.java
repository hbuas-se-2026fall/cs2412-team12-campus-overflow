package com.campusoverflow.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 声誉明细 Spring Data 仓储。
 */
public interface SpringDataReputationEntryRepository extends JpaRepository<ReputationEntryJpaEntity, String> {

    List<ReputationEntryJpaEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    boolean existsByEventId(String eventId);
}
