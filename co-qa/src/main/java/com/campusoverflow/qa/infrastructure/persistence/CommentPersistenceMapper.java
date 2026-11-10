package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.model.TargetType;
import com.campusoverflow.shared.identity.UserId;

/**
 * 评论：领域对象 ↔ 持久化对象。
 */
final class CommentPersistenceMapper {

    private CommentPersistenceMapper() {
    }

    static void apply(Comment comment, CommentJpaEntity entity) {
        entity.apply(
                comment.target().type().code(),
                comment.target().targetId(),
                comment.authorId().asString(),
                comment.body(),
                comment.createdAt());
    }

    static Comment toDomain(CommentJpaEntity entity) {
        return Comment.rehydrate(
                CommentId.of(entity.getId()),
                new ContentTarget(TargetType.valueOf(entity.getTargetType().toUpperCase()), entity.getTargetId()),
                UserId.of(entity.getAuthorId()),
                entity.getBody(),
                entity.getCreatedAt());
    }
}
