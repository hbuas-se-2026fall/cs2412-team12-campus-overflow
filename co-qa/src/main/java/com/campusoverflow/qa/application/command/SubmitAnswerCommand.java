package com.campusoverflow.qa.application.command;

/**
 * 提交回答命令（{@code SubmitAnswer}，US-06）。
 */
public record SubmitAnswerCommand(String questionId, String authorId, String body) {
}
