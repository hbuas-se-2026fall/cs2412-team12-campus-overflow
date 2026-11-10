package com.campusoverflow.qa.interfaces.rest;

import com.campusoverflow.qa.application.QuestionApplicationService;
import com.campusoverflow.qa.application.command.AcceptAnswerCommand;
import com.campusoverflow.qa.application.command.CreateQuestionCommand;
import com.campusoverflow.qa.domain.model.Question;
import com.campusoverflow.qa.interfaces.rest.dto.AcceptAnswerRequest;
import com.campusoverflow.qa.interfaces.rest.dto.CreateQuestionRequest;
import com.campusoverflow.qa.interfaces.rest.dto.QuestionPageResponse;
import com.campusoverflow.qa.interfaces.rest.dto.QuestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 问答接口层。
 *
 * <p>Lab 4 的核心交付：{@code POST /api/v1/questions} 端到端贯通
 * Controller → ApplicationService → Repository → MySQL。</p>
 */
@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionApplicationService questionApplicationService;

    public QuestionController(QuestionApplicationService questionApplicationService) {
        this.questionApplicationService = questionApplicationService;
    }

    /** 发布问题：成功返回 201 + Location。 */
    @PostMapping
    public ResponseEntity<QuestionResponse> create(@Valid @RequestBody CreateQuestionRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        Question question = questionApplicationService.create(new CreateQuestionCommand(
                request.askerId(), request.title(), request.body(), request.tags()));
        var location = uriBuilder.path("/api/v1/questions/{id}")
                .buildAndExpand(question.id().asString()).toUri();
        return ResponseEntity.created(location).body(QuestionResponse.from(question));
    }

    /** 问题列表（US-03）：可选按标签筛选（US-10）。 */
    @GetMapping
    public QuestionPageResponse list(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "20") int size,
                                     @RequestParam(name = "tag", required = false) String tag) {
        return QuestionPageResponse.from(questionApplicationService.list(page, size, tag));
    }

    /** 查看问题详情（US-05 的最小实现，便于端到端验证"写进去能读出来"）。 */
    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable("id") String id) {
        return QuestionResponse.from(questionApplicationService.get(
                com.campusoverflow.qa.domain.model.QuestionId.of(id)));
    }

    /** 采纳答案（US-08）：验证 INV-Q-03 / INV-Q-04。 */
    @PostMapping("/{id}/acceptance")
    public QuestionResponse accept(@PathVariable("id") String id,
                                   @Valid @RequestBody AcceptAnswerRequest request) {
        Question question = questionApplicationService.acceptAnswer(
                new AcceptAnswerCommand(id, request.answerId(), request.operatorId()));
        return QuestionResponse.from(question);
    }
}
