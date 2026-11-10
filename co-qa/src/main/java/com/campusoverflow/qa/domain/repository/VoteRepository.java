package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.Vote;
import com.campusoverflow.shared.identity.UserId;

import java.util.Optional;

/**
 * 投票仓储（AGG-04）。"一人一目标一票"由 {@link #find} + 数据库唯一键共同保证。
 */
public interface VoteRepository {

    Optional<Vote> find(ContentTarget target, UserId voterId);

    Vote save(Vote vote);

    void delete(Vote vote);
}
