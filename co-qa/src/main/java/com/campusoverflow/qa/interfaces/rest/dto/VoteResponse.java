package com.campusoverflow.qa.interfaces.rest.dto;

import com.campusoverflow.qa.application.view.VoteResult;

/**
 * 投票响应：返回目标的最新票数，前端据此更新计数。
 */
public record VoteResponse(
        String targetType,
        String targetId,
        String voterId,
        String value,
        int targetVoteCount
) {

    public static VoteResponse from(VoteResult result) {
        return new VoteResponse(
                result.target().type().code(),
                result.target().targetId(),
                result.voterId().asString(),
                result.value().code(),
                result.targetVoteCount());
    }
}
