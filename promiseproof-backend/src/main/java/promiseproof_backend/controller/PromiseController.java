
package promiseproof_backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import promiseproof_backend.dto.ExtractedPromiseResponse;
import promiseproof_backend.dto.ManifestoExtractRequest;
import promiseproof_backend.dto.PublicationDecisionRequest;
import promiseproof_backend.dto.PromiseRequest;
import promiseproof_backend.dto.PromiseResponse;
import promiseproof_backend.dto.PromiseSourceRequest;
import promiseproof_backend.dto.PromiseSourceResponse;
import promiseproof_backend.dto.PromiseStatusCorrectionRequest;
import promiseproof_backend.dto.PublicPromiseResponse;
import promiseproof_backend.dto.PublicPromiseStatusHistoryResponse;
import promiseproof_backend.service.ManifestoService;
import promiseproof_backend.service.PromiseService;
import promiseproof_backend.service.PromiseSourceService;

import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/promises")
public class PromiseController {

    private final PromiseService promiseService;
    private final PromiseSourceService sourceService;
    private final ManifestoService manifestoService;

    public PromiseController(
            PromiseService promiseService,
            PromiseSourceService sourceService,
            ManifestoService manifestoService) {

        this.promiseService = promiseService;
        this.sourceService = sourceService;
        this.manifestoService = manifestoService;
    }

    @PostMapping
    public ResponseEntity<PromiseResponse> createPromise(
            @RequestBody PromiseRequest request) {

        PromiseResponse saved =
                promiseService.createPromise(request);

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PublicPromiseResponse>> getAllPromises() {

        return ResponseEntity.ok(
                promiseService.getAllPromises()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PublicPromiseResponse>> searchPromises(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long electionId,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "false")
            boolean withApprovedEvidence,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                promiseService.searchPromises(
                        q,
                        status,
                        partyId,
                        electionId,
                        state,
                        withApprovedEvidence,
                        pageable
                )
        );
    }

    /**
     * Public citizen listing with search, filters, sorting and pagination.
     * Publication visibility is enforced by the service, never by the caller.
     */
    @GetMapping("/public")
    public ResponseEntity<Page<PublicPromiseResponse>> getPublicPromises(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long electionId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false")
            boolean withApprovedEvidence,
            @RequestParam(required = false, defaultValue = "recent")
            String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Sort ordering = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "updatedAt")
                : Sort.by(Sort.Direction.DESC, "updatedAt");

        PageRequest pageable = PageRequest.of(safePage, safeSize, ordering);

        return ResponseEntity.ok(
                promiseService.searchPublicPromises(
                        q,
                        status,
                        partyId,
                        electionId,
                        state,
                        category,
                        withApprovedEvidence,
                        pageable
                )
        );
    }

    // Promises created by the signed-in user.
    @GetMapping("/mine")
    public ResponseEntity<List<PromiseResponse>> getMyPromises(
            Authentication authentication) {

        return ResponseEntity.ok(
                promiseService.getMyPromises(authentication.getName())
        );
    }

    // Independent publication review queue (reviewers and administrators).
    @GetMapping("/publication-queue")
    public ResponseEntity<List<PromiseResponse>> getPublicationQueue() {

        return ResponseEntity.ok(
                promiseService.getPublicationQueue()
        );
    }

    // Suggest promises from pasted manifesto text. Nothing is saved until the
    // researcher confirms the selection.
    @PostMapping("/extract")
    public ResponseEntity<List<ExtractedPromiseResponse>> extractPromises(
            @RequestBody ManifestoExtractRequest request) {

        return ResponseEntity.ok(
                manifestoService.extract(request.text())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicPromiseResponse> getPromiseById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                promiseService.getPromiseById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromiseResponse> updatePromise(
            @PathVariable Long id,
            @RequestBody PromiseRequest request) {

        return ResponseEntity.ok(
                promiseService.updatePromise(id, request)
        );
    }

    // Researcher submits a draft for independent publication review.
    @PostMapping("/{id}/submit-for-publication")
    public ResponseEntity<PromiseResponse> submitForPublication(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                promiseService.submitForPublication(id)
        );
    }

    // Reviewer/administrator publication decision (REVIEWED/PUBLISHED/REJECTED).
    @PutMapping("/{id}/publication")
    public ResponseEntity<PromiseResponse> decidePublication(
            @PathVariable Long id,
            @RequestBody PublicationDecisionRequest request) {

        return ResponseEntity.ok(
                promiseService.decidePublication(id, request)
        );
    }

    // Original source records for a promise.
    @GetMapping("/{id}/sources")
    public ResponseEntity<List<PromiseSourceResponse>> getSources(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                sourceService.getSources(id)
        );
    }

    @PostMapping("/{id}/sources")
    public ResponseEntity<PromiseSourceResponse> createSource(
            @PathVariable Long id,
            @RequestBody PromiseSourceRequest request) {

        return new ResponseEntity<>(
                sourceService.createSource(id, request),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}/sources/{sourceId}")
    public ResponseEntity<PromiseSourceResponse> updateSource(
            @PathVariable Long id,
            @PathVariable Long sourceId,
            @RequestBody PromiseSourceRequest request) {

        return ResponseEntity.ok(
                sourceService.updateSource(id, sourceId, request)
        );
    }

    @DeleteMapping("/{id}/sources/{sourceId}")
    public ResponseEntity<Void> deleteSource(
            @PathVariable Long id,
            @PathVariable Long sourceId) {

        sourceService.deleteSource(id, sourceId);

        return ResponseEntity.noContent().build();
    }

    // Administrator-only, separately audited status correction. This is the
    // exception path for fixing a demonstrably wrong status; the normal
    // workflow is an approved status verification proposal.
    @PutMapping("/{id}/status")
    public ResponseEntity<PromiseResponse> correctStatus(
            @PathVariable Long id,
            @RequestBody PromiseStatusCorrectionRequest request) {

        return ResponseEntity.ok(
                promiseService.correctStatus(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromise(
            @PathVariable Long id) {

        promiseService.deletePromise(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PublicPromiseStatusHistoryResponse>>
    getPromiseHistory(@PathVariable Long id) {

        return ResponseEntity.ok(
                promiseService.getPublicStatusHistory(id)
        );
    }
}
