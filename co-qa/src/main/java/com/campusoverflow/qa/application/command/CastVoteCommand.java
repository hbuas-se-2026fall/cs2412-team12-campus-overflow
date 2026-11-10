package com.campusoverflow.qa.application.command;

/**
 * 投票命令（{@code CastVote} / {@code RetractVote}，US-07）。
 *
 * @param targetType QUESTION / ANSWER
 * @param targetId   目标标识
 * @param voterId    投票人
 * @param value      up / down；撤票时可为空
 */
public record CastVoteCommand(String targetType, String targetId, String voterId, String value) {
}
