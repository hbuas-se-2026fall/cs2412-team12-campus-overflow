package com.campusoverflow.qa.application.view;

import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.VoteValue;
import com.campusoverflow.shared.identity.UserId;

/**
 * 投票结果：只回传"目标现在的票数"，页面刷新时再拉详情即可。
 */
public record VoteResult(ContentTarget target, UserId voterId, VoteValue value, int targetVoteCount) {
}
