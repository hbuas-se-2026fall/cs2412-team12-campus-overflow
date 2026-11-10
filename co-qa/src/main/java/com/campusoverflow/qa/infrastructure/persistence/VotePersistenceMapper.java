package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.TargetType;
import com.campusoverflow.qa.domain.model.Vote;
import com.campusoverflow.qa.domain.model.VoteId;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.shared.identity.UserId;

/**
 * 投票：领域对象 ↔ 持久化对象。
 */
final class VotePersistenceMapper {

    private VotePersistenceMapper() {
    }

    static void apply(Vote vote, VoteJpaEntity entity) {
        entity.apply(
                vote.target().type().code(),
                vote.target().targetId(),
                vote.voterId().asString(),
                vote.value().code(),
                vote.castAt(),
                vote.castAt());
    }

    static Vote toDomain(VoteJpaEntity entity) {
        return Vote.rehydrate(
                VoteId.of(entity.getId()),
                new ContentTarget(TargetType.valueOf(entity.getTargetType().toUpperCase()), entity.getTargetId()),
                UserId.of(entity.getVoterId()),
                VoteValue.valueOf(entity.getValue().toUpperCase()),
                entity.getUpdatedAt());
    }
}
