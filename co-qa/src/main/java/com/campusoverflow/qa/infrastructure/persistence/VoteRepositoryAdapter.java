package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.Vote;
import com.campusoverflow.qa.domain.repository.VoteRepository;
import com.campusoverflow.shared.identity.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 投票仓储实现。
 */
@Repository
public class VoteRepositoryAdapter implements VoteRepository {

    private final SpringDataVoteRepository jpa;

    public VoteRepositoryAdapter(SpringDataVoteRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Vote> find(ContentTarget target, UserId voterId) {
        return jpa.findByTargetTypeAndTargetIdAndVoterId(
                        target.type().code(), target.targetId(), voterId.asString())
                .map(VotePersistenceMapper::toDomain);
    }

    @Override
    public Vote save(Vote vote) {
        VoteJpaEntity entity = jpa.findById(vote.id().asString())
                .orElseGet(() -> new VoteJpaEntity(vote.id().asString()));
        VotePersistenceMapper.apply(vote, entity);
        return VotePersistenceMapper.toDomain(jpa.save(entity));
    }

    @Override
    public void delete(Vote vote) {
        jpa.deleteById(vote.id().asString());
    }
}
