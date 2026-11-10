package com.campusoverflow.qa.domain.repository;

import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.domain.model.CommentId;
import com.campusoverflow.qa.domain.model.ContentTarget;

import java.util.List;
import java.util.Optional;

/**
 * 评论仓储（AGG-05）。
 */
public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(CommentId id);

    List<Comment> findByTarget(ContentTarget target);

    void delete(CommentId id);
}
