package promiseproof_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import promiseproof_backend.dto.PromiseStatusDecisionRequest;
import promiseproof_backend.dto.PromiseStatusProposalRequest;
import promiseproof_backend.dto.PromiseStatusProposalResponse;
import promiseproof_backend.service.PromiseStatusProposalService;

@RestController
@RequestMapping("/api/status-proposals")
public class StatusProposalController {

    private final PromiseStatusProposalService proposalService;

    public StatusProposalController(
            PromiseStatusProposalService proposalService) {

        this.proposalService = proposalService;
    }

    // Researchers and administrators propose a status change backed by
    // approved evidence.
    @PostMapping
    public ResponseEntity<PromiseStatusProposalResponse> submit(
            @RequestBody PromiseStatusProposalRequest request) {

        PromiseStatusProposalResponse response =
                proposalService.submit(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    // Pending status proposals for the reviewer queue.
    @GetMapping("/pending")
    public ResponseEntity<List<PromiseStatusProposalResponse>> getPending() {

        return ResponseEntity.ok(proposalService.getPending());
    }

    // Status proposals recorded for a single promise.
    @GetMapping("/promise/{promiseId}")
    public ResponseEntity<List<PromiseStatusProposalResponse>> getForPromise(
            @PathVariable Long promiseId
    ) {

        return ResponseEntity.ok(proposalService.getForPromise(promiseId));
    }

    // Reviewer/administrator decision on a status verification proposal.
    @PutMapping("/{id}/decision")
    public ResponseEntity<PromiseStatusProposalResponse> decide(
            @PathVariable Long id,
            @RequestBody PromiseStatusDecisionRequest request) {

        return ResponseEntity.ok(
                proposalService.decide(id, request)
        );
    }
}
