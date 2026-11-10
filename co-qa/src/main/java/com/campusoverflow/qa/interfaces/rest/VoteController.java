package com.campusoverflow.qa.interfaces.rest;

import com.campusoverflow.qa.application.VoteApplicationService;
import com.campusoverflow.qa.application.command.CastVoteCommand;
import com.campusoverflow.qa.application.view.VoteResult;
import com.campusoverflow.qa.interfaces.rest.dto.CastVoteRequest;
import com.campusoverflow.qa.interfaces.rest.dto.VoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投票接口（US-07）：投/改票与撤票。
 */
@RestController
@RequestMapping("/api/v1/votes")
public class VoteController {

    private final VoteApplicationService voteApplicationService;

    public VoteController(VoteApplicationService voteApplicationService) {
        this.voteApplicationService = voteApplicationService;
    }

    /** 投票或改票：同一人重复投同一票是幂等的。 */
    @PostMapping
    public VoteResponse cast(@Valid @RequestBody CastVoteRequest request) {
        VoteResult result = voteApplicationService.cast(new CastVoteCommand(
                request.targetType(), request.targetId(), request.voterId(), request.value()));
        return VoteResponse.from(result);
    }

    /** 撤票：没投过也返回 204（幂等）。 */
    @DeleteMapping
    public ResponseEntity<Void> retract(@RequestParam("targetType") String targetType,
                                        @RequestParam("targetId") String targetId,
                                        @RequestParam("voterId") String voterId) {
        voteApplicationService.retract(new CastVoteCommand(targetType, targetId, voterId, null));
        return ResponseEntity.noContent().build();
    }
}
