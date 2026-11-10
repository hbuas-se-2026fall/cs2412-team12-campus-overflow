package com.campusoverflow.qa.application.command;

/**
 * 发表评论命令（{@code PostComment}，US-09）。
 */
public record PostCommentCommand(String targetType, String targetId, String authorId, String body) {
}
