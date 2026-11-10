package com.campusoverflow.reputation.interfaces.rest;

import com.campusoverflow.reputation.application.ReputationApplicationService;
import com.campusoverflow.reputation.interfaces.rest.dto.ReputationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 声誉查询接口（US-11）：个人卡片与问题详情都用它。
 */
@RestController
@RequestMapping("/api/v1/users")
public class ReputationController {

    private final ReputationApplicationService reputationApplicationService;

    public ReputationController(ReputationApplicationService reputationApplicationService) {
        this.reputationApplicationService = reputationApplicationService;
    }

    @GetMapping("/{id}/reputation")
    public ReputationResponse get(@PathVariable("id") String id) {
        return ReputationResponse.from(reputationApplicationService.view(id));
    }
}
