package com.campusoverflow.qa.interfaces.rest;

import com.campusoverflow.qa.application.QuestionApplicationService;
import com.campusoverflow.qa.application.command.SubmitAnswerCommand;
import com.campusoverflow.qa.domain.model.Answer;
import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.interfaces.rest.dto.AnswerResponse;
import com.campusoverflow.qa.interfaces.rest.dto.CreateAnswerRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * 回答接口（US-06）。
 */
@RestController
@RequestMapping("/api/v1/answers")
public class AnswerController {

    private final QuestionApplicationService questionApplicationService;

    public AnswerController(QuestionApplicationService questionApplicationService) {
        this.questionApplicationService = questionApplicationService;
    }

    /** 提交回答：成功后 201，Location 指向所属问题详情。 */
    @PostMapping
    public ResponseEntity<AnswerResponse> submit(@Valid @RequestBody CreateAnswerRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        Answer answer = questionApplicationService.submitAnswer(
                new SubmitAnswerCommand(request.questionId(), request.authorId(), request.body()));
        var location = uriBuilder.path("/api/v1/questions/{id}")
                .buildAndExpand(answer.questionId().asString()).toUri();
        return ResponseEntity.created(location).body(AnswerResponse.from(answer));
    }

    /** 某问题下的全部回答（按时间正序）。 */
    @GetMapping
    public List<AnswerResponse> list(@RequestParam("questionId") String questionId) {
        return questionApplicationService.answersOf(QuestionId.of(questionId)).stream()
                .map(AnswerResponse::from)
                .toList();
    }
}
