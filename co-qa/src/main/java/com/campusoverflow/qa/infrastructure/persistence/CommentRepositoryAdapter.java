package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;
import com.campusoverflow.qa.domain.repository.CommentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论仓储实现。
 */
@Repository
public class CommentRepositoryAdapter implements CommentRepository {

    private final SpringDataCommentRepository jpa;

    public CommentRepositoryAdapter(SpringDataCommentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Comment save(Comment comment) {
        CommentJpaEntity entity = jpa.findById(comment.id().asString())
                .orElseGet(() -> new CommentJpaEntity(comment.id().asString()));
        CommentPersistenceMapper.apply(comment, entity);
        return CommentPersistenceMapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Comment> findById(CommentId id) {
        return jpa.findById(id.asString()).map(CommentPersistenceMapper::toDomain);
    }

    @Override
    public List<Comment> findByTarget(ContentTarget target) {
        return jpa.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        target.type().code(), target.targetId()).stream()
                .map(CommentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(CommentId id) {
        jpa.deleteById(id.asString());
    }
}
