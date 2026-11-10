package com.campusoverflow.qa.application.command;

/**
 * 采纳答案命令（{@code AcceptAnswer}，对应 US-08）。
 */
public record AcceptAnswerCommand(String questionId, String answerId, String operatorId) {
}
