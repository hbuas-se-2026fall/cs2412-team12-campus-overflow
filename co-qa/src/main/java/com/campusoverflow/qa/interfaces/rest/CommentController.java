package com.campusoverflow.qa.interfaces.rest;

import com.campusoverflow.qa.application.CommentApplicationService;
import com.campusoverflow.qa.application.command.PostCommentCommand;
import com.campusoverflow.qa.domain.model.Comment;
import com.campusoverflow.qa.interfaces.rest.dto.CommentResponse;
import com.campusoverflow.qa.interfaces.rest.dto.CreateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论接口（US-09）。
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentApplicationService commentApplicationService;

    public CommentController(CommentApplicationService commentApplicationService) {
        this.commentApplicationService = commentApplicationService;
    }

    @PostMapping
    public CommentResponse post(@Valid @RequestBody CreateCommentRequest request) {
        Comment comment = commentApplicationService.post(new PostCommentCommand(
                request.targetType(), request.targetId(), request.authorId(), request.body()));
        return CommentResponse.from(comment);
    }

    @GetMapping
    public List<CommentResponse> list(@RequestParam("targetType") String targetType,
                                      @RequestParam("targetId") String targetId) {
        return commentApplicationService.list(targetType, targetId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /** 删除自己的评论（INV-C-03）。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id,
                                       @RequestParam("operatorId") String operatorId) {
        commentApplicationService.delete(id, operatorId);
        return ResponseEntity.noContent().build();
    }
}
