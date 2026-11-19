package promiseproof_backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.EvidenceAuditResponse;
import promiseproof_backend.dto.EvidenceResponse;
import promiseproof_backend.dto.EvidenceReviewRequest;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.service.EvidenceAuditService;
import promiseproof_backend.service.EvidenceService;
import promiseproof_backend.service.FileStorageService;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final EvidenceAuditService auditService;
    private final FileStorageService fileStorageService;

    public EvidenceController(
            EvidenceService evidenceService,
            EvidenceAuditService auditService,
            FileStorageService fileStorageService) {

        this.evidenceService = evidenceService;
        this.auditService = auditService;
        this.fileStorageService = fileStorageService;
    }

    // Upload a new evidence file for a Promise.
    @PostMapping
    public ResponseEntity<EvidenceResponse> uploadEvidence(
            @RequestParam Long promiseId,
            @RequestParam String title,
            @RequestParam String evidenceType,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String issuingDepartment,
            @RequestParam(required = false) String documentReference,
            @RequestParam(required = false) LocalDate documentDate,
            @RequestParam(required = false) String claimScope,
            @RequestParam("file") MultipartFile file) {

        EvidenceResponse saved = evidenceService.uploadEvidence(
                promiseId,
                title,
                evidenceType,
                description,
                sourceUrl,
                issuingDepartment,
                documentReference,
                documentDate,
                claimScope,
                file
        );

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // Get all evidence submitted for a particular Promise.
    @GetMapping("/promise/{promiseId}")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForPromise(
            @PathVariable Long promiseId) {

        return ResponseEntity.ok(
                evidenceService.getEvidenceForPromise(promiseId)
        );
    }

    // Public view: only approved evidence is exposed.
    @GetMapping("/promise/{promiseId}/approved")
    public ResponseEntity<List<EvidenceResponse>> getApprovedEvidenceForPromise(
            @PathVariable Long promiseId) {

        return ResponseEntity.ok(
                evidenceService.getApprovedEvidenceForPromise(promiseId)
        );
    }

    // Evidence submitted by the signed-in user.
    @GetMapping("/mine")
    public ResponseEntity<List<EvidenceResponse>> getMyEvidence(
            Authentication authentication) {

        return ResponseEntity.ok(
                evidenceService.getMyEvidence(authentication.getName())
        );
    }

    // Review queue across every promise (reviewers and administrators).
    @GetMapping("/pending")
    public ResponseEntity<List<EvidenceResponse>> getPendingEvidence() {

        return ResponseEntity.ok(evidenceService.getPendingEvidence());
    }

    // Download an evidence file.
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadEvidence(
            @PathVariable Long id,
            Authentication authentication) {

        Evidence evidence = evidenceService.getEvidenceEntity(id);

        // Pending or rejected evidence must not be publicly downloadable.
        // The submitter may still download their own document, and reviewers
        // and administrators can download anything.
        boolean privileged = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_REVIEWER") ||
                                        authority.getAuthority().equals("ROLE_ADMIN"));

        boolean owner = authentication != null &&
                evidence.getUploadedByEmail() != null &&
                evidence.getUploadedByEmail()
                        .equalsIgnoreCase(authentication.getName());

        if (!privileged && !owner &&
                (!"APPROVED".equals(evidence.getStatus()) ||
                        !evidence.getPromise().isPubliclyVisible())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Evidence not found"
            );
        }

        Resource resource = fileStorageService.loadFile(
                evidence.getStoredFileName()
        );

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                evidence.getOriginalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        String contentType = evidence.getContentType();

        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    // Audit history for a single evidence document.
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<EvidenceAuditResponse>> getAuditHistory(
            @PathVariable Long id) {

        // 404 when the evidence does not exist.
        evidenceService.getEvidenceEntity(id);

        return ResponseEntity.ok(auditService.getForEvidence(id));
    }

    // Approve or reject a piece of evidence.
    @PutMapping("/{id}/review")
    public ResponseEntity<EvidenceResponse> reviewEvidence(
            @PathVariable Long id,
            @RequestBody EvidenceReviewRequest request) {

        return ResponseEntity.ok(
                evidenceService.reviewEvidence(id, request)
        );
    }

    // Controlled resubmission of a rejected document.
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<EvidenceResponse> resubmitEvidence(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                evidenceService.resubmitEvidence(id)
        );
    }
}
