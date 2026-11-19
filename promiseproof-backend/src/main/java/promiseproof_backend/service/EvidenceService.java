package promiseproof_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.EvidenceResponse;
import promiseproof_backend.dto.EvidenceReviewRequest;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.Role;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PromiseRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EvidenceService {

    private static final Set<String> DOCUMENT_REVIEWS =
            Set.of("ACCEPTED", "REJECTED");
    private static final Set<String> SOURCE_REVIEWS =
            Set.of("VERIFIED", "DISPUTED");
    private static final Set<String> CLAIM_REVIEWS =
            Set.of("SUPPORTED", "PARTIAL", "NOT_SUPPORTED");

    private final EvidenceRepository evidenceRepository;
    private final PromiseRepository promiseRepository;
    private final FileStorageService fileStorageService;
    private final EvidenceAuditService auditService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public EvidenceService(
            EvidenceRepository evidenceRepository,
            PromiseRepository promiseRepository,
            FileStorageService fileStorageService,
            EvidenceAuditService auditService,
            NotificationService notificationService,
            CurrentUser currentUser) {

        this.evidenceRepository = evidenceRepository;
        this.promiseRepository = promiseRepository;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    private EvidenceResponse toResponse(Evidence evidence) {

        EvidenceResponse response = new EvidenceResponse();

        response.setId(evidence.getId());
        response.setPromiseId(evidence.getPromise().getId());
        response.setPromiseTitle(evidence.getPromise().getTitle());
        response.setTitle(evidence.getTitle());
        response.setEvidenceType(evidence.getEvidenceType());
        response.setDescription(evidence.getDescription());
        response.setSourceUrl(evidence.getSourceUrl());
        response.setIssuingDepartment(evidence.getIssuingDepartment());
        response.setDocumentReference(evidence.getDocumentReference());
        response.setDocumentDate(evidence.getDocumentDate());
        response.setClaimScope(evidence.getClaimScope());
        response.setSha256Hash(evidence.getSha256Hash());
        response.setOriginalFileName(evidence.getOriginalFileName());
        response.setContentType(evidence.getContentType());
        response.setFileSize(evidence.getFileSize());
        response.setStatus(evidence.getStatus());
        response.setDocumentReviewStatus(evidence.getDocumentReviewStatus());
        response.setSourceReviewStatus(evidence.getSourceReviewStatus());
        response.setClaimReviewStatus(evidence.getClaimReviewStatus());
        response.setVerificationNotes(evidence.getVerificationNotes());
        response.setReviewedByEmail(evidence.getReviewedByEmail());
        response.setReviewedByName(evidence.getReviewedByName());
        response.setUploadedByEmail(evidence.getUploadedByEmail());
        response.setUploadedByName(evidence.getUploadedByName());
        response.setReviewComment(evidence.getReviewComment());
        response.setUploadedAt(evidence.getUploadedAt());
        response.setReviewedAt(evidence.getReviewedAt());

        return response;
    }

    @Transactional
    public EvidenceResponse uploadEvidence(
            Long promiseId,
            String title,
            String evidenceType,
            String description,
            String sourceUrl,
            String issuingDepartment,
            String documentReference,
            LocalDate documentDate,
            String claimScope,
            MultipartFile file) {

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Evidence title is required"
            );
        }

        if (evidenceType == null || evidenceType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Evidence type is required"
            );
        }

        PromiseSourceService.validateUrl(sourceUrl);

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        // Save file first.
        String storedName = fileStorageService.storeFile(file);

        String actorEmail = currentUser.email();
        String actorName = currentUser.displayName();

        try {

            Evidence evidence = new Evidence();

            evidence.setPromise(promise);
            evidence.setTitle(title.trim());
            evidence.setEvidenceType(evidenceType);
            evidence.setDescription(description);
            evidence.setSourceUrl(trimToNull(sourceUrl));
            evidence.setIssuingDepartment(trimToNull(issuingDepartment));
            evidence.setDocumentReference(trimToNull(documentReference));
            evidence.setDocumentDate(documentDate);
            evidence.setClaimScope(trimToNull(claimScope));
            evidence.setOriginalFileName(
                    file.getOriginalFilename()
            );
            evidence.setStoredFileName(storedName);
            evidence.setContentType(file.getContentType());
            evidence.setFileSize(file.getSize());
            evidence.setSha256Hash(fileStorageService.sha256(storedName));
            evidence.setStatus("PENDING");
            evidence.setDocumentReviewStatus("PENDING");
            evidence.setSourceReviewStatus("PENDING");
            evidence.setClaimReviewStatus("PENDING");
            evidence.setUploadedByEmail(actorEmail);
            evidence.setUploadedByName(actorName);

            Evidence saved = evidenceRepository.save(evidence);

            auditService.record(
                    saved,
                    "UPLOADED",
                    null,
                    saved.getStatus(),
                    "SHA-256: " + saved.getSha256Hash()
            );

            String submitter = actorName != null
                    ? actorName
                    : (actorEmail != null ? actorEmail : "A user");

            String message = submitter + " submitted \"" +
                    saved.getTitle() + "\" as evidence for \"" +
                    promise.getTitle() + "\".";

            notificationService.notifyRole(
                    Role.REVIEWER,
                    "EVIDENCE_SUBMITTED",
                    "New evidence awaiting review",
                    message,
                    "/review",
                    actorEmail
            );

            notificationService.notifyRole(
                    Role.ADMIN,
                    "EVIDENCE_SUBMITTED",
                    "New evidence awaiting review",
                    message,
                    "/review",
                    actorEmail
            );

            return toResponse(saved);

        } catch (RuntimeException exception) {

            // Avoid leaving an orphaned file if database save fails.
            fileStorageService.deleteFile(storedName);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> getEvidenceForPromise(Long promiseId) {

        if (!promiseRepository.existsById(promiseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promise not found"
            );
        }

        return evidenceRepository
                .findByPromise_IdOrderByUploadedAtDesc(promiseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Public view: only evidence that a reviewer has approved, and only for a
    // promise that is actually published.
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getApprovedEvidenceForPromise(
            Long promiseId) {

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        if (!promise.isPubliclyVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promise not found"
            );
        }

        return evidenceRepository
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(
                        promiseId,
                        "APPROVED"
                )
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    /**
     * Public projection of an approved document. Submitter and reviewer
     * identity are not part of the public record, so they are omitted here.
     */
    private EvidenceResponse toPublicResponse(Evidence evidence) {

        EvidenceResponse response = toResponse(evidence);

        response.setUploadedByEmail(null);
        response.setUploadedByName(null);
        response.setReviewedByEmail(null);
        response.setReviewedByName(null);

        // Reviewer notes and internal verification commentary are private.
        // The approval date remains public as evidence of the review.
        response.setReviewComment(null);
        response.setVerificationNotes(null);

        return response;
    }

    // Evidence submitted by the signed-in user.
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getMyEvidence(String email) {

        if (email == null || email.isBlank()) {
            return List.of();
        }

        return evidenceRepository
                .findByUploadedByEmailIgnoreCaseOrderByUploadedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Review queue: every pending document across all promises.
    @Transactional(readOnly = true)
    public List<EvidenceResponse> getPendingEvidence() {

        return evidenceRepository
                .findByStatusOrderByUploadedAtDesc("PENDING")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Evidence getEvidenceEntity(Long evidenceId) {

        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evidence not found"
                ));
    }

    /**
     * Three independent review decisions:
     *  - document review: may the document be published?
     *  - source review: can it be traced to the claimed issuer?
     *  - claim review: does it support the specific progress claimed?
     *
     * The overall status is derived; approving a document never proves that a
     * promise was completed.
     */
    @Transactional
    public EvidenceResponse reviewEvidence(
            Long evidenceId,
            EvidenceReviewRequest request) {

        Evidence evidence = getEvidenceEntity(evidenceId);

        if (evidence.getUploadedByEmail() != null &&
                evidence.getUploadedByEmail()
                        .equalsIgnoreCase(currentUser.email())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A person who uploaded a document cannot review it"
            );
        }

        if (!"PENDING".equals(evidence.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This document has already been reviewed. The submitter " +
                            "must resubmit it before it can be reviewed again."
            );
        }

        String documentReview = normalise(
                request.getDocumentReview(),
                DOCUMENT_REVIEWS,
                "Document review"
        );
        String sourceReview = normalise(
                request.getSourceReview(),
                SOURCE_REVIEWS,
                "Source review"
        );
        String claimReview = normalise(
                request.getClaimReview(),
                CLAIM_REVIEWS,
                "Claim review"
        );

        // Backwards-compatible shortcut for clients that only send status.
        if (documentReview == null &&
                sourceReview == null &&
                claimReview == null) {

            String legacy = request.getStatus() == null
                    ? null
                    : request.getStatus().trim().toUpperCase(Locale.ROOT);

            if ("APPROVED".equals(legacy)) {
                documentReview = "ACCEPTED";
                sourceReview = "VERIFIED";
                claimReview = "SUPPORTED";
            } else if ("REJECTED".equals(legacy)) {
                documentReview = "REJECTED";
                sourceReview = "DISPUTED";
                claimReview = "NOT_SUPPORTED";
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Provide documentReview, sourceReview and claimReview"
                );
            }
        }

        if (documentReview == null ||
                sourceReview == null ||
                claimReview == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document review, source review and claim review are all " +
                            "required"
            );
        }

        if ("REJECTED".equals(documentReview) &&
                (request.getReviewComment() == null ||
                        request.getReviewComment().isBlank())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a reason for rejection"
            );
        }

        String newStatus = deriveStatus(documentReview, sourceReview);

        String previousStatus = evidence.getStatus();

        evidence.setDocumentReviewStatus(documentReview);
        evidence.setSourceReviewStatus(sourceReview);
        evidence.setClaimReviewStatus(claimReview);
        evidence.setStatus(newStatus);
        evidence.setReviewComment(request.getReviewComment());
        evidence.setVerificationNotes(request.getVerificationNotes());
        evidence.setReviewedAt(LocalDateTime.now());
        evidence.setReviewedByEmail(currentUser.email());
        evidence.setReviewedByName(currentUser.displayName());

        Evidence saved = evidenceRepository.save(evidence);

        // Document approval is not promise verification. Approval only raises
        // the promise's verification state to "evidence reviewed"; verifying
        // the recorded implementation status still requires an approved status
        // proposal. An existing verification or active dispute is preserved.
        if ("APPROVED".equals(newStatus)) {

            Promise promise = evidence.getPromise();

            if (promise != null &&
                    !"STATUS_VERIFIED".equals(promise.getVerificationState()) &&
                    !"DISPUTED".equals(promise.getVerificationState())) {

                promise.setVerificationState("EVIDENCE_REVIEWED");
                promiseRepository.save(promise);
            }
        }

        auditService.record(
                saved,
                newStatus,
                previousStatus,
                newStatus,
                "Document: " + documentReview +
                        ", source: " + sourceReview +
                        ", claim: " + claimReview +
                        (request.getReviewComment() == null
                                ? ""
                                : ". " + request.getReviewComment())
        );

        notifyUploader(saved, newStatus, request.getReviewComment());

        return toResponse(saved);
    }

    /**
     * Controlled resubmission: a rejected document returns to the queue with
     * fresh review dimensions. Only the submitter (or an administrator) can do
     * this.
     */
    @Transactional
    public EvidenceResponse resubmitEvidence(Long evidenceId) {

        Evidence evidence = getEvidenceEntity(evidenceId);

        boolean admin = Role.ADMIN.name().equals(currentUser.role());
        boolean owner = evidence.getUploadedByEmail() != null &&
                evidence.getUploadedByEmail()
                        .equalsIgnoreCase(currentUser.email());

        if (!admin && !owner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the submitter or an administrator can resubmit this " +
                            "document"
            );
        }

        if (!"REJECTED".equals(evidence.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only rejected documents can be resubmitted"
            );
        }

        String previousStatus = evidence.getStatus();

        evidence.setStatus("PENDING");
        evidence.setDocumentReviewStatus("PENDING");
        evidence.setSourceReviewStatus("PENDING");
        evidence.setClaimReviewStatus("PENDING");
        evidence.setReviewComment(null);
        evidence.setVerificationNotes(null);
        evidence.setReviewedAt(null);
        evidence.setReviewedByEmail(null);
        evidence.setReviewedByName(null);

        Evidence saved = evidenceRepository.save(evidence);

        auditService.record(
                saved,
                "RESUBMITTED",
                previousStatus,
                "PENDING",
                "Document resubmitted for review"
        );

        notificationService.notifyRole(
                Role.REVIEWER,
                "EVIDENCE_RESUBMITTED",
                "Evidence resubmitted",
                "\"" + saved.getTitle() + "\" was resubmitted for review.",
                "/review",
                currentUser.email()
        );

        notificationService.notifyRole(
                Role.ADMIN,
                "EVIDENCE_RESUBMITTED",
                "Evidence resubmitted",
                "\"" + saved.getTitle() + "\" was resubmitted for review.",
                "/review",
                currentUser.email()
        );

        return toResponse(saved);
    }

    private static String normalise(
            String value,
            Set<String> allowed,
            String label) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalised = value.trim().toUpperCase(Locale.ROOT);

        if (!allowed.contains(normalised)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " must be one of " + String.join(", ", allowed)
            );
        }

        return normalised;
    }

    private static String deriveStatus(
            String documentReview,
            String sourceReview) {

        if ("REJECTED".equals(documentReview) ||
                "DISPUTED".equals(sourceReview)) {
            return "REJECTED";
        }

        if ("ACCEPTED".equals(documentReview) &&
                "VERIFIED".equals(sourceReview)) {
            return "APPROVED";
        }

        return "PENDING";
    }

    private void notifyUploader(
            Evidence evidence,
            String status,
            String reviewComment) {

        String uploader = evidence.getUploadedByEmail();
        String reviewer = currentUser.email();

        if (uploader == null ||
                uploader.isBlank() ||
                uploader.equalsIgnoreCase(reviewer)) {

            return;
        }

        boolean approved = "APPROVED".equals(status);

        String message = "\"" + evidence.getTitle() + "\" for \"" +
                evidence.getPromise().getTitle() + "\" was " +
                (approved ? "approved" : "rejected") + ".";

        if (reviewComment != null && !reviewComment.isBlank()) {
            message += " Reviewer note: " + reviewComment;
        }

        notificationService.notifyUser(
                uploader,
                approved ? "EVIDENCE_APPROVED" : "EVIDENCE_REJECTED",
                approved
                        ? "Your evidence was approved"
                        : "Your evidence was rejected",
                message,
                "/dashboard"
        );
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
