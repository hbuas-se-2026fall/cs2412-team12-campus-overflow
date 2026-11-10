package com.campusoverflow.qa.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 投票 Spring Data 仓储。
 */
public interface SpringDataVoteRepository extends JpaRepository<VoteJpaEntity, String> {

    Optional<VoteJpaEntity> findByTargetTypeAndTargetIdAndVoterId(String targetType, String targetId, String voterId);
}
