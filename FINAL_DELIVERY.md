# PromiseProof â€” Citizen Transparency Platform: Final Delivery

Status: **complete**. Backend tests are **71/71 passing**; frontend `lint` and `build` pass.

## Backend startup fix (Nov 19)

The backend startup failure was caused by missing `DB_URL`, `DB_USER` and `DB_PASSWORD`. The project now imports an optional local `.env` file:

- `promiseproof-backend/.env` contains local development database values and is ignored by git.
- `promiseproof-backend/.env.example` documents the required values without real credentials.
- `application.properties` uses `spring.config.import=optional:file:.env[.properties]`.
- Real environment variables still take precedence; production does not need a `.env` file.

The backend now starts successfully with `.\mvnw.cmd -o spring-boot:run` without manually exporting the variables. Public endpoints were verified live: `/api/promises/public` and `/api/stats/public` both returned HTTP 200. A stale Java process holding port 8080 was also stopped.

## What already existed and was reused

- Domain model: `Promise`, `Evidence`, `PromiseStatusChange`, `PromiseSource`, `User`, parties/elections.
- Workflows: publication review, evidence review, independent status proposals, notifications, audit logging.
- Auth/roles: Citizen, Researcher, Reviewer, Admin via Spring Security.
- Existing dashboards: Admin, Researcher, Reviewer and the earlier Citizen evidence workflows were preserved; no replacement or deletion.

## What was added

- Public read APIs for published promises, public detail, approved evidence, public status history and public aggregate stats.
- Service-level publication filtering so `DRAFT`, `SUBMITTED`, `REVIEWED` and rejected promises never appear in public listings, details, stats, evidence or history.
- Safe public DTOs with no creator email/name, reviewer identity, review comments, verification notes or internal moderation fields.
- Public status history restricted to approved independent reviews (`reviewerDecision = APPROVED`) and linked only to still-approved evidence.
- Citizen dashboard sections: Browse Promises (search/filter/sort/pagination), Reviewed Evidence, Promise Tracking, My Account, plus the existing submit/submissions/progress/notifications tabs.
- Five-tab public promise detail page: Details, Original Sources, Approved Evidence, Implementation Tracking, Status History.
- Responsive citizen styles and a top navigation with Home, Browse Promises, My Evidence, My Account and Logout.
- Evidence upload now treats `.jpg` and `.jpeg` as the same JPEG format, accepts common browser JPEG content types and generic `application/octet-stream`, and still verifies the real file signature. Upload helper text was simplified.

## Exact changed files

Backend:
- `promiseproof-backend/src/main/resources/application.properties`
- `promiseproof-backend/.env` (local only, gitignored)
- `promiseproof-backend/.env.example`
- `promiseproof-backend/.gitignore`
- `promiseproof-backend/src/main/java/promiseproof_backend/service/FileStorageService.java`
- `promiseproof-backend/src/test/java/promiseproof_backend/service/FileStorageServiceTest.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/dto/PublicSupportingEvidenceResponse.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/dto/PublicPromiseStatusHistoryResponse.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/entity/PromiseStatusChange.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/service/PromiseStatusProposalService.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/controller/PromiseController.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/controller/StatsController.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/service/EvidenceService.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/config/SecurityConfig.java`
- `promiseproof-backend/src/test/java/promiseproof_backend/service/PublicCitizenDashboardTest.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/dto/PublicPromiseResponse.java`
- `promiseproof-backend/src/main/java/promiseproof_backend/service/PromiseService.java`

Frontend:
- `promiseproof-frontend/src/services/api.js`
- `promiseproof-frontend/src/services/statsApi.js`
- `promiseproof-frontend/src/components/citizen/statusInfo.js`
- `promiseproof-frontend/src/components/citizen/PublicPromiseCard.jsx`
- `promiseproof-frontend/src/components/citizen/ApprovedEvidenceList.jsx`
- `promiseproof-frontend/src/components/citizen/PromiseTrackingTimeline.jsx`
- `promiseproof-frontend/src/components/citizen/PublicPromiseList.jsx`
- `promiseproof-frontend/src/pages/PromiseDetails.jsx`
- `promiseproof-frontend/src/App.jsx`
- `promiseproof-frontend/src/components/Navbar.jsx`
- `promiseproof-frontend/src/styles/citizen.css`
- `promiseproof-frontend/src/dashboards/CitizenDashboard.jsx`

## Non-destructive migration

No manual SQL or data rewrite is required. With `spring.jpa.hibernate.ddl-auto=update`, Hibernate adds:

- `promise_status_changes.evidence_id BIGINT NULL`
- `promise_status_changes.reviewer_decision VARCHAR(20) NULL`

Existing rows remain valid; both columns are nullable. No drop, truncate or delete was issued.

## Exact PowerShell commands

Backend tests (from `V:\PromiseProof\promiseproof-backend`):

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/promiseproof"; $env:DB_USER="root"; $env:DB_PASSWORD="root"; .\mvnw.cmd -o -B test
```

Backend run (the local `.env` file is loaded automatically):

```powershell
cd V:\PromiseProof\promiseproof-backend
.\mvnw.cmd -o spring-boot:run
```

If you prefer environment variables, they still override `.env`:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/promiseproof"; $env:DB_USER="root"; $env:DB_PASSWORD="root"; .\mvnw.cmd -o spring-boot:run
```

Frontend (from `V:\PromiseProof\promiseproof-frontend`):

```powershell
npm.cmd install
npm.cmd run lint
npm.cmd run build
npm.cmd run dev
```

Admin login: `http://localhost:5173/login/admin`. Seeded credentials are taken only from `PROMISEPROOF_ADMIN_EMAIL` and `PROMISEPROOF_ADMIN_PASSWORD` before first seed; there is no hardcoded fallback password.

## Test report

Passed:
- Backend: 71 tests, 0 failures, 0 errors, 0 skipped.
- Evidence upload tests cover `.jpg` and `.jpeg` filenames for the same JPEG content, generic browser content types, and content/extension mismatches.
- Seven `PublicCitizenDashboardTest` scenarios: published promise with no evidence; submitter-only evidence hidden publicly; approved evidence visible; rejected evidence hidden; approved evidence does not complete a promise; approved status proposal appears in public history; unpublished promise hidden from public detail/history/evidence/stats.
- Frontend `npm.cmd run lint`: clean.
- Frontend `npm.cmd run build`: success.

Not run here:
- Browser-based end-to-end UI clicks against a live backend, manual cross-role UI regression for every Admin/Researcher/Reviewer screen, and actual file download rendering in a browser. The dashboards were preserved structurally, and their code was not replaced.

## Complete code appendix

### `promiseproof-backend\src\main\java\promiseproof_backend\dto\PublicSupportingEvidenceResponse.java`

`$lang
package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A safe public reference to an approved evidence document that supports an
 * independently reviewed status change.
 *
 * Reviewer notes, submitter identity and moderation details are deliberately
 * not part of this projection. Only metadata that helps a citizen audit the
 * status decision is exposed.
 */
public record PublicSupportingEvidenceResponse(
        Long id,
        String title,
        String evidenceType,
        String issuingDepartment,
        String documentReference,
        LocalDate documentDate,
        LocalDateTime reviewedAt
) {
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\dto\PublicPromiseStatusHistoryResponse.java`

`$lang
package promiseproof_backend.dto;

import promiseproof_backend.dto.PublicSupportingEvidenceResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public, independently reviewed status history for a published promise.
 *
 * The response never carries reviewer identity or internal moderation notes.
 * Each entry is an approved independent review, and the supporting approved
 * evidence is included only as a safe public reference.
 */
public record PublicPromiseStatusHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        String reviewedBy,
        String reason,
        LocalDateTime changedAt,
        String reviewerDecision,
        List<PublicSupportingEvidenceResponse> supportingEvidence
) {
    public PublicPromiseStatusHistoryResponse {
        supportingEvidence =
                supportingEvidence == null ? List.of() : supportingEvidence;
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\entity\PromiseStatusChange.java`

`$lang
package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable history of promise status transitions that were applied through
 * an approved status verification proposal. Old and new status values are both
 * retained for every applied transition.
 */
@Entity
@Table(name = "promise_status_changes")
public class PromiseStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long promiseId;

    @Column(length = 20)
    private String fromStatus;

    @Column(length = 20)
    private String toStatus;

    @Column(length = 190)
    private String changedByEmail;

    @Column(length = 120)
    private String changedByName;

    @Column(length = 2000)
    private String reason;

    /**
     * Optional link to the approved evidence that supported this transition.
     * Used only for the public supporting-evidence reference.
     */
    private Long evidenceId;

    /**
     * Set to APPROVED when an independent status-review proposal was accepted.
     * Only independently approved transitions are exposed in public history;
     * administrator corrections intentionally remain internal.
     */
    @Column(length = 20)
    private String reviewerDecision;

    private LocalDateTime createdAt;

    public PromiseStatusChange() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPromiseId() {
        return promiseId;
    }

    public void setPromiseId(Long promiseId) {
        this.promiseId = promiseId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getChangedByEmail() {
        return changedByEmail;
    }

    public void setChangedByEmail(String changedByEmail) {
        this.changedByEmail = changedByEmail;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getReviewerDecision() {
        return reviewerDecision;
    }

    public void setReviewerDecision(String reviewerDecision) {
        this.reviewerDecision = reviewerDecision;
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\service\PromiseStatusProposalService.java`

`$lang
package promiseproof_backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.PromiseStatusDecisionRequest;
import promiseproof_backend.dto.PromiseStatusHistoryResponse;
import promiseproof_backend.dto.PromiseStatusProposalRequest;
import promiseproof_backend.dto.PromiseStatusProposalResponse;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.PromiseStatusChange;
import promiseproof_backend.entity.PromiseStatusProposal;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.PromiseStatusChangeRepository;
import promiseproof_backend.repository.PromiseStatusProposalRepository;

import java.time.LocalDateTime;

@Service
public class PromiseStatusProposalService {

    /**
     * Statuses a researcher may propose. NOT_ASSESSED is omitted on purpose:
     * it is the neutral initial state, not a verified finding.
     */
    private static final List<String> PROPOSABLE_STATUSES = List.of(
            "NOT_STARTED",
            "IN_PROGRESS",
            "COMPLETED",
            "DELAYED",
            "PARTIALLY_COMPLETED",
            "CANCELLED"
    );

    private final PromiseRepository promiseRepository;
    private final EvidenceRepository evidenceRepository;
    private final PromiseStatusProposalRepository proposalRepository;
    private final PromiseStatusChangeRepository changeRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PromiseStatusProposalService(
            PromiseRepository promiseRepository,
            EvidenceRepository evidenceRepository,
            PromiseStatusProposalRepository proposalRepository,
            PromiseStatusChangeRepository changeRepository,
            CurrentUser currentUser,
            AuditService auditService) {

        this.promiseRepository = promiseRepository;
        this.evidenceRepository = evidenceRepository;
        this.proposalRepository = proposalRepository;
        this.changeRepository = changeRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    /**
     * A researcher (or administrator) proposes a new promise status and links
     * it to an approved evidence document.
     */
    @Transactional
    public PromiseStatusProposalResponse submit(
            PromiseStatusProposalRequest request) {

        if (request == null || request.getPromiseId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promise id is required"
            );
        }

        String proposedStatus = resolveStatus(request.getProposedStatus());

        Promise promise = promiseRepository.findById(request.getPromiseId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        User actor = currentUser.get().orElse(null);

        if (actor != null &&
                "RESEARCHER".equals(actor.getRole().name()) &&
                (promise.getCreatedByEmail() == null ||
                        !promise.getCreatedByEmail()
                                .equalsIgnoreCase(actor.getEmail()))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Researchers can only propose status changes for " +
                            "their own promises"
            );
        }

        if (proposalRepository
                .findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(
                        promise.getId(),
                        "PENDING"
                )
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A status verification proposal is already pending " +
                            "for this promise"
            );
        }

        if (request.getEvidenceId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attach an approved evidence document before proposing " +
                            "a status change"
            );
        }

        Evidence evidence = evidenceRepository.findById(request.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evidence not found"
                ));

        assertEvidenceSupportsProposal(evidence, promise);

        if (promise.getStatus() != null &&
                promise.getStatus().equals(proposedStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The promise already has the requested status"
            );
        }

        PromiseStatusProposal proposal = new PromiseStatusProposal();

        proposal.setPromiseId(promise.getId());
        proposal.setProposedStatus(proposedStatus);
        proposal.setReason(request.getReason());
        proposal.setEvidenceId(evidence.getId());
        proposal.setProposedByEmail(
                currentUser.email()
        );
        proposal.setProposedByName(
                currentUser.displayName()
        );
        proposal.setStatus("PENDING");

        PromiseStatusProposal saved = proposalRepository.save(proposal);

        // A pending proposal means the recorded status is under scrutiny.
        // If the promise was already verified, the verification is disputed.
        boolean wasVerified = "STATUS_VERIFIED".equals(
                promise.getVerificationState()
        );

        promise.setVerificationState(
                wasVerified ? "DISPUTED" : "UNDER_REVIEW"
        );
        promiseRepository.save(promise);

        auditService.record(
                "STATUS_PROPOSAL_SUBMITTED",
                "PROMISE_STATUS",
                promise.getId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                request.getReason()
        );

        return toResponse(saved, promise);
    }

    /**
     * Reviewers and administrators review pending status proposals. Approval
     * applies the proposed status to the promise and keeps the transition in
     * the promise status history.
     *
     * The evidence is revalidated at decision time: it must still exist, still
     * belong to the promise, still be APPROVED, and its claim review must not
     * have found that the document fails to support the claim. This closes the
     * window in which evidence could change after the proposal was submitted.
     */
    @Transactional
    public PromiseStatusProposalResponse decide(
            Long proposalId,
            PromiseStatusDecisionRequest request) {

        PromiseStatusProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Status proposal not found"
                ));

        if (!"PENDING".equals(proposal.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This status proposal has already been decided"
            );
        }

        String decision = request == null || request.getDecision() == null
                ? null
                : request.getDecision().trim().toUpperCase(Locale.ROOT);

        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision must be APPROVED or REJECTED"
            );
        }

        if ("REJECTED".equals(decision) &&
                (request.getComment() == null || request.getComment().isBlank())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a reason for rejecting the proposal"
            );
        }

        // Separation of duties. Administrators are not exempt: the person who
        // proposed a change can never approve or reject it.
        String actorEmail = currentUser.email();

        if (actorEmail != null &&
                proposal.getProposedByEmail() != null &&
                actorEmail.equalsIgnoreCase(proposal.getProposedByEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot decide on a status proposal that you " +
                            "submitted yourself"
            );
        }

        Promise promise = promiseRepository.findById(proposal.getPromiseId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        Evidence evidence = proposal.getEvidenceId() == null
                ? null
                : evidenceRepository.findById(proposal.getEvidenceId())
                        .orElse(null);

        if (evidence != null &&
                actorEmail != null &&
                evidence.getUploadedByEmail() != null &&
                actorEmail.equalsIgnoreCase(evidence.getUploadedByEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The person who uploaded the supporting evidence cannot " +
                            "decide on the proposal that relies on it"
            );
        }

        if ("APPROVED".equals(decision)) {

            // Revalidate the supporting evidence at decision time.
            if (evidence == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The supporting evidence no longer exists. The " +
                                "proposal cannot be approved."
                );
            }

            assertEvidenceSupportsProposal(evidence, promise);
        }

        proposal.setStatus(decision);
        proposal.setReviewComment(request.getComment());
        proposal.setDecidedAt(LocalDateTime.now());

        PromiseStatusProposal saved = proposalRepository.save(proposal);

        auditService.record(
                "STATUS_PROPOSAL_" + decision,
                "PROMISE_STATUS",
                proposal.getPromiseId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                request.getComment()
        );

        if ("APPROVED".equals(decision)) {

            String fromStatus = promise.getStatus() == null
                    ? "NOT_ASSESSED"
                    : promise.getStatus();

            promise.setStatus(proposal.getProposedStatus());
            promise.setVerificationState("STATUS_VERIFIED");

            promiseRepository.save(promise);

            PromiseStatusChange change = new PromiseStatusChange();

            change.setPromiseId(promise.getId());
            change.setFromStatus(fromStatus);
            change.setToStatus(proposal.getProposedStatus());
            change.setChangedByEmail(currentUser.email());
            change.setChangedByName(currentUser.displayName());
            change.setReason(proposal.getReason());

            // Public tracking history only shows independently approved
            // transitions. Record the decision and the supporting evidence so
            // citizens see what the reviewer relied on.
            change.setReviewerDecision("APPROVED");
            change.setEvidenceId(proposal.getEvidenceId());

            changeRepository.save(change);

            return toResponse(saved, promise);
        }

        // A rejection keeps the public status, but it must not leave the record
        // looking verified. Fall back to the highest state the evidence
        // supports.
        boolean hasApprovedEvidence =
                evidenceRepository.countByPromise_IdAndStatus(
                        promise.getId(),
                        "APPROVED"
                ) > 0;

        promise.setVerificationState(
                hasApprovedEvidence ? "EVIDENCE_REVIEWED" : "UNREVIEWED"
        );
        promiseRepository.save(promise);

        return toResponse(saved, promise);
    }

    @Transactional(readOnly = true)
    public List<PromiseStatusProposalResponse> getPending() {

        return proposalRepository
                .findByStatusOrderByCreatedAtDesc("PENDING")
                .stream()
                .map(proposal -> toResponse(
                        proposal,
                        promiseRepository.findById(proposal.getPromiseId())
                                .orElse(null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PromiseStatusProposalResponse> getForPromise(Long promiseId) {

        return proposalRepository
                .findByPromiseIdOrderByCreatedAtDesc(promiseId)
                .stream()
                .map(proposal -> toResponse(
                        proposal,
                        promiseRepository.findById(promiseId)
                                .orElse(null)
                ))
                .toList();
    }

    /**
     * Evidence must exist, belong to the promise, be approved, and its claim
     * review must not have concluded that the document fails to support the
     * claim. Shared by submission and decision-time revalidation.
     */
    private void assertEvidenceSupportsProposal(
            Evidence evidence,
            Promise promise) {

        if (evidence.getPromise() == null ||
                !evidence.getPromise().getId().equals(promise.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected evidence does not belong to this promise"
            );
        }

        if (!"APPROVED".equals(evidence.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The supporting evidence is no longer approved. It must " +
                            "be reviewed and approved again before a status " +
                            "change can rely on it."
            );
        }

        if ("NOT_SUPPORTED".equals(evidence.getClaimReviewStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The reviewer found that this document does not support " +
                            "the claimed progress, so it cannot justify a " +
                            "status change."
            );
        }
    }

    private String resolveStatus(String status) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposed status is required"
            );
        }

        String normalised = status.trim().toUpperCase(Locale.ROOT);

        if (!PROPOSABLE_STATUSES.contains(normalised)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid proposed status. Allowed values: " +
                            String.join(", ", PROPOSABLE_STATUSES)
            );
        }

        return normalised;
    }

    private PromiseStatusProposalResponse toResponse(
            PromiseStatusProposal proposal,
            Promise promise
    ) {

        PromiseStatusProposalResponse response =
                new PromiseStatusProposalResponse();

        response.setId(proposal.getId());
        response.setPromiseId(proposal.getPromiseId());
        response.setProposedStatus(proposal.getProposedStatus());
        response.setReason(proposal.getReason());
        response.setEvidenceId(proposal.getEvidenceId());
        response.setProposedByEmail(proposal.getProposedByEmail());
        response.setProposedByName(proposal.getProposedByName());
        response.setStatus(proposal.getStatus());
        response.setReviewComment(proposal.getReviewComment());
        response.setCreatedAt(proposal.getCreatedAt());
        response.setDecidedAt(proposal.getDecidedAt());

        if (promise != null) {
            response.setPromiseTitle(promise.getTitle());
            response.setPromiseDescription(promise.getDescription());
            response.setCurrentStatus(
                    promise.getStatus() == null
                            ? "NOT_ASSESSED"
                            : promise.getStatus()
            );

            if (promise.getParty() != null) {
                response.setPromisePartyName(promise.getParty().getName());
            } else {
                response.setPromisePartyName(promise.getPartyName());
            }

            if (promise.getElection() != null) {
                response.setPromiseElectionName(
                        promise.getElection().getName()
                );
            } else {
                response.setPromiseElectionName(promise.getElectionName());
            }

            response.setStatusHistory(
                    changeRepository
                            .findByPromiseIdOrderByCreatedAtAsc(promise.getId())
                            .stream()
                            .map(PromiseStatusProposalService::toHistory)
                            .toList()
            );
        }

        if (proposal.getEvidenceId() != null) {

            evidenceRepository.findById(proposal.getEvidenceId())
                    .ifPresent(evidence -> {
                        response.setEvidenceTitle(evidence.getTitle());
                        response.setEvidenceType(evidence.getEvidenceType());
                        response.setEvidenceSourceUrl(
                                evidence.getSourceUrl()
                        );
                        response.setEvidenceFileName(
                                evidence.getOriginalFileName()
                        );
                        response.setEvidenceStatus(evidence.getStatus());
                        response.setEvidenceUploadedByName(
                                evidence.getUploadedByName()
                        );
                        response.setEvidenceUploadedByEmail(
                                evidence.getUploadedByEmail()
                        );
                    });
        }

        return response;
    }

    private static PromiseStatusHistoryResponse toHistory(
            PromiseStatusChange change) {

        return new PromiseStatusHistoryResponse(
                change.getId(),
                change.getFromStatus(),
                change.getToStatus(),
                change.getChangedByEmail(),
                change.getChangedByName(),
                change.getReason(),
                change.getCreatedAt()
        );
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\controller\PromiseController.java`

`$lang

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

```

### `promiseproof-backend\src\main\java\promiseproof_backend\controller\StatsController.java`

`$lang
package promiseproof_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import promiseproof_backend.dto.StatsOverviewResponse;
import promiseproof_backend.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    // Public dashboard statistics. The service filters to published promises
    // and never returns reviewer identities or moderation details.
    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview() {

        return ResponseEntity.ok(statsService.getOverview());
    }

    // Explicit public alias used by the citizen dashboard.
    @GetMapping("/public")
    public ResponseEntity<StatsOverviewResponse> getPublicOverview() {

        return ResponseEntity.ok(statsService.getOverview());
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\service\EvidenceService.java`

`$lang
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

```

### `promiseproof-backend\src\main\java\promiseproof_backend\config\SecurityConfig.java`

`$lang
package promiseproof_backend.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import promiseproof_backend.repository.UserRepository;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            UserRepository userRepository) throws Exception {

        ActiveUserFilter activeUserFilter =
                new ActiveUserFilter(userRepository);

        // CSRF token is issued as a readable cookie so the SPA can echo it in
        // the X-XSRF-TOKEN header on every state-changing request.
        CookieCsrfTokenRepository tokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        tokenRepository.setCookiePath("/");
        tokenRepository.setCookieName("XSRF-TOKEN");
        tokenRepository.setHeaderName("X-XSRF-TOKEN");

        XorCsrfTokenRequestAttributeHandler csrfHandler =
                new XorCsrfTokenRequestAttributeHandler();

        // Resolve the token from the request header rather than the masked
        // request attribute (the standard SPA configuration).
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(tokenRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .cors(Customizer.withDefaults())
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .addFilterAfter(
                        activeUserFilter,
                        org.springframework.security.web.context
                                .SecurityContextHolderFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public authentication endpoints.
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/logout",
                                "/api/auth/csrf"
                        ).permitAll()



                        // Personal collections need a signed-in user and are
                        // declared before the public read-only rules below.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/mine"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/search"
                        ).permitAll()

                        // Publication review queue: reviewers and admins. Must
                        // be declared before the public GET rules below.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/publication-queue"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Truly public, read-only citizen views. Declared
                        // before the generic authenticated rules below. Each
                        // service enforces publication visibility and strips
                        // private submitter/reviewer information.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/promises/public",
                                "/api/promises/*",
                                "/api/promises/*/history",
                                "/api/evidence/promise/*/approved",
                                "/api/stats/public"
                        ).permitAll()

                        // Publication decision: reviewers and admins.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/publication"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Submitting a draft and managing its original sources
                        // stays with researchers and administrators; ownership
                        // is enforced in the service layer.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/promises/*/submit-for-publication"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/promises/*/sources"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/sources/*"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/promises/*/sources/*"
                        ).hasAnyRole("RESEARCHER", "ADMIN")

                        // Public read-only views.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/parties/**",
                                "/api/elections/**",
                                "/api/promises/**",
                                "/api/stats/**"
                        ).authenticated()

                        // Public evidence: approved records only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/promise/*/approved"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/*/download"
                        ).authenticated()

                        // A signed-in user can always read their own uploads.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/mine"
                        ).authenticated()

                        // Evidence queues are reviewer-only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/pending"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/promise/*"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/evidence/**"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Any signed-in user may submit evidence.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/evidence"
                        ).authenticated()

                        // Controlled resubmission: the submitter or an
                        // administrator (checked in the service).
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/evidence/*/resubmit"
                        ).authenticated()

                        // Only reviewers and admins may verify evidence.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/evidence/*/review"
                        ).hasAnyRole("REVIEWER", "ADMIN")

                        // Party and election records are admin-managed.
                        .requestMatchers(
                                "/api/parties/**",
                                "/api/elections/**"
                        ).hasRole("ADMIN")

                        // Promise status verification: researchers propose
                        // changes backed by approved evidence; reviewers and
                        // administrators decide.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/status-proposals/pending"
                        ).hasAnyRole("REVIEWER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/status-proposals/*/decision"
                        ).hasAnyRole("REVIEWER", "ADMIN")
                        .requestMatchers("/api/status-proposals/**")
                        .hasAnyRole("RESEARCHER", "ADMIN")

                        // Administrative status corrections are a separate,
                        // audited exception path and are restricted to admins.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/promises/*/status"
                        ).hasRole("ADMIN")

                        // Researchers and admins manage promises.
                        .requestMatchers("/api/promises/**")
                        .hasAnyRole("ADMIN", "RESEARCHER")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Platform-wide audit trail is administrator-only.
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        .requestMatchers("/api/notifications/**")
                        .authenticated()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, error) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Unauthorized",
                                        "Please sign in to continue"
                                ))
                        .accessDeniedHandler((request, response, error) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Forbidden",
                                        "You do not have permission to perform this action"
                                ))
                );

        return http.build();
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(
                "{\"status\":" + status +
                        ",\"error\":\"" + error +
                        "\",\"message\":\"" + message + "\"}"
        );
    }
}

```

### `promiseproof-backend\src\test\java\promiseproof_backend\service\PublicCitizenDashboardTest.java`

`$lang
package promiseproof_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.EvidenceResponse;
import promiseproof_backend.dto.EvidenceReviewRequest;
import promiseproof_backend.dto.PublicPromiseResponse;
import promiseproof_backend.dto.PublicPromiseStatusHistoryResponse;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.PromiseStatusChange;
import promiseproof_backend.repository.ElectionRepository;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PartyRepository;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.PromiseSourceRepository;
import promiseproof_backend.repository.PromiseStatusChangeRepository;
import promiseproof_backend.service.AuditService;
import promiseproof_backend.service.CurrentUser;
import promiseproof_backend.service.EvidenceAuditService;
import promiseproof_backend.service.EvidenceService;
import promiseproof_backend.service.FileStorageService;
import promiseproof_backend.service.NotificationService;
import promiseproof_backend.service.PromiseService;

/**
 * Backend coverage for the seven citizen-dashboard visibility and workflow
 * scenarios. These are service-level tests: the rules are enforced in the
 * services, so the same guarantees apply to every controller that calls them.
 */
@ExtendWith(MockitoExtension.class)
class PublicCitizenDashboardTest {

    @Mock
    private PromiseRepository promiseRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private ElectionRepository electionRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private PromiseSourceRepository sourceRepository;

    @Mock
    private PromiseStatusChangeRepository statusChangeRepository;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private AuditService auditService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private EvidenceAuditService evidenceAuditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PromiseService promiseService;

    @InjectMocks
    private EvidenceService evidenceService;

    private static Promise promise(
            Long id, String status, String publicationStatus) {

        Promise promise = new Promise();
        promise.setId(id);
        promise.setTitle("Build 100 hospitals");
        promise.setDescription("A public commitment");
        promise.setStatus(status);
        promise.setVerificationState("UNREVIEWED");
        promise.setPublicationStatus(publicationStatus);
        promise.setPublishedAt(
                publicationStatus == null ||
                        Promise.PUBLISHED.equals(publicationStatus)
                        ? LocalDateTime.of(2026, 1, 10, 9, 0)
                        : null
        );
        return promise;
    }

    private static Evidence evidence(
            String status, String uploadedBy, String reviewer) {

        Evidence evidence = new Evidence();
        evidence.setPromise(promise(1L, "NOT_ASSESSED", Promise.PUBLISHED));
        evidence.setTitle("Official gazette");
        evidence.setEvidenceType("GOVERNMENT_ORDER");
        evidence.setDescription("Published government order");
        evidence.setIssuingDepartment("Finance Department");
        evidence.setDocumentReference("GO-2026-001");
        evidence.setDocumentDate(LocalDate.of(2026, 2, 1));
        evidence.setOriginalFileName("gazette.pdf");
        evidence.setStoredFileName("stored-gazette.pdf");
        evidence.setContentType("application/pdf");
        evidence.setFileSize(1024L);
        evidence.setStatus(status);
        evidence.setUploadedByEmail(uploadedBy);
        evidence.setUploadedByName("Submitter");
        evidence.setReviewedByEmail(reviewer);
        evidence.setReviewedByName("Reviewer");
        evidence.setReviewComment("Confidential moderation note");
        evidence.setVerificationNotes("Internal verification note");
        evidence.setReviewedAt(LocalDateTime.of(2026, 3, 1, 12, 0));
        return evidence;
    }

    // Scenario 1: a published promise with no evidence is still public, and
    // its approved-evidence section is empty.
    @Test
    void publishedPromiseWithoutEvidenceIsPublicWithEmptyEvidenceList() {

        Promise published = promise(1L, "NOT_ASSESSED", Promise.PUBLISHED);

        when(promiseRepository.findAll()).thenReturn(List.of(published));
        when(sourceRepository.findByPromise_IdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());
        when(evidenceRepository.countByPromise_IdAndStatus(1L, "APPROVED"))
                .thenReturn(0L);

        List<PublicPromiseResponse> promises = promiseService.getAllPromises();

        assertEquals(1, promises.size());
        assertEquals("Build 100 hospitals", promises.get(0).title());
        assertFalse(promises.get(0).hasApprovedEvidence());

        when(promiseRepository.findById(1L)).thenReturn(Optional.of(published));
        when(evidenceRepository
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(1L, "APPROVED"))
                .thenReturn(List.of());

        List<EvidenceResponse> approved =
                evidenceService.getApprovedEvidenceForPromise(1L);

        assertTrue(approved.isEmpty());
    }

    // Scenario 2: evidence is retrieved only for the signed-in uploader.
    @Test
    void citizenEvidenceIsVisibleOnlyToItsUploader() {

        Evidence mine = evidence("PENDING", "alice@example.com", null);

        when(evidenceRepository
                .findByUploadedByEmailIgnoreCaseOrderByUploadedAtDesc(
                        "alice@example.com"))
                .thenReturn(List.of(mine));
        when(evidenceRepository
                .findByUploadedByEmailIgnoreCaseOrderByUploadedAtDesc(
                        "bob@example.com"))
                .thenReturn(List.of());

        assertEquals(1, evidenceService.getMyEvidence("alice@example.com").size());
        assertTrue(evidenceService.getMyEvidence("bob@example.com").isEmpty());
        assertTrue(evidenceService.getMyEvidence("").isEmpty());

        verify(evidenceRepository, never())
                .findByUploadedByEmailIgnoreCaseOrderByUploadedAtDesc("");
    }

    // Scenario 3: an approved document becomes public, with reviewer notes and
    // identities removed.
    @Test
    void approvedEvidenceBecomesPublicWithoutPrivateReviewerInformation() {

        Promise published = promise(1L, "NOT_ASSESSED", Promise.PUBLISHED);
        Evidence approved =
                evidence("APPROVED", "alice@example.com", "reviewer@example.com");

        when(promiseRepository.findById(1L)).thenReturn(Optional.of(published));
        when(evidenceRepository
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(1L, "APPROVED"))
                .thenReturn(List.of(approved));

        List<EvidenceResponse> publicEvidence =
                evidenceService.getApprovedEvidenceForPromise(1L);

        assertEquals(1, publicEvidence.size());
        EvidenceResponse item = publicEvidence.get(0);
        assertEquals("Official gazette", item.getTitle());
        assertEquals("Finance Department", item.getIssuingDepartment());
        assertEquals("GO-2026-001", item.getDocumentReference());
        assertEquals("APPROVED", item.getStatus());
        assertNull(item.getUploadedByEmail());
        assertNull(item.getUploadedByName());
        assertNull(item.getReviewedByEmail());
        assertNull(item.getReviewedByName());
        assertNull(item.getReviewComment());
        assertNull(item.getVerificationNotes());
    }

    // Scenario 4: a rejected document stays private; the public query only ever
    // asks for APPROVED records.
    @Test
    void rejectedEvidenceIsNotExposedPublicly() {

        Promise published = promise(1L, "NOT_ASSESSED", Promise.PUBLISHED);
        Evidence rejected =
                evidence("REJECTED", "alice@example.com", "reviewer@example.com");

        when(promiseRepository.findById(1L)).thenReturn(Optional.of(published));
        when(evidenceRepository
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(1L, "APPROVED"))
                .thenReturn(List.of());

        assertTrue(evidenceService.getApprovedEvidenceForPromise(1L).isEmpty());
        assertEquals("REJECTED", rejected.getStatus());

        verify(evidenceRepository)
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(1L, "APPROVED");
    }

    // Scenario 5: approving evidence never completes the promise by itself.
    @Test
    void evidenceApprovalDoesNotCompletePromise() {

        Promise promise = promise(1L, "NOT_ASSESSED", Promise.PUBLISHED);
        Evidence pending = evidence("PENDING", "alice@example.com", null);
        pending.setPromise(promise);

        when(currentUser.email()).thenReturn("reviewer@example.com");
        when(currentUser.displayName()).thenReturn("Reviewer");
        when(evidenceRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(evidenceRepository.save(any(Evidence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceReviewRequest request = new EvidenceReviewRequest();
        request.setDocumentReview("ACCEPTED");
        request.setSourceReview("VERIFIED");
        request.setClaimReview("SUPPORTED");

        EvidenceResponse result = evidenceService.reviewEvidence(1L, request);

        assertEquals("APPROVED", result.getStatus());
        assertEquals("NOT_ASSESSED", promise.getStatus());
        assertEquals("EVIDENCE_REVIEWED", promise.getVerificationState());
        assertFalse("COMPLETED".equals(promise.getStatus()));
    }

    // Scenario 6: an independently approved status change appears in public
    // history, with an approved-evidence reference and no internal changes.
    @Test
    void independentlyApprovedStatusChangeAppearsInPublicHistory() {

        Promise published = promise(1L, "IN_PROGRESS", Promise.PUBLISHED);

        PromiseStatusChange reviewed = new PromiseStatusChange();
        reviewed.setId(11L);
        reviewed.setPromiseId(1L);
        reviewed.setFromStatus("NOT_ASSESSED");
        reviewed.setToStatus("IN_PROGRESS");
        reviewed.setReason("Budget released and work commenced");
        reviewed.setReviewerDecision("APPROVED");
        reviewed.setEvidenceId(10L);
        reviewed.setChangedByEmail("reviewer@example.com");

        PromiseStatusChange internalCorrection = new PromiseStatusChange();
        internalCorrection.setId(12L);
        internalCorrection.setPromiseId(1L);
        internalCorrection.setFromStatus("IN_PROGRESS");
        internalCorrection.setToStatus("DELAYED");
        internalCorrection.setReason("Internal admin correction");
        // reviewerDecision is null: never published as a verified fact.
        internalCorrection.setChangedByEmail("admin@example.com");

        Evidence supporting = evidence(
                "APPROVED", "alice@example.com", "reviewer@example.com");

        when(promiseRepository.findById(1L)).thenReturn(Optional.of(published));
        when(statusChangeRepository.findByPromiseIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(reviewed, internalCorrection));
        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(supporting));

        List<PublicPromiseStatusHistoryResponse> history =
                promiseService.getPublicStatusHistory(1L);

        assertEquals(1, history.size());
        PublicPromiseStatusHistoryResponse entry = history.get(0);
        assertEquals("NOT_ASSESSED", entry.fromStatus());
        assertEquals("IN_PROGRESS", entry.toStatus());
        assertEquals("APPROVED", entry.reviewerDecision());
        assertEquals("Independent review", entry.reviewedBy());
        assertEquals(1, entry.supportingEvidence().size());
        assertEquals("Official gazette",
                entry.supportingEvidence().get(0).title());
        assertEquals(LocalDateTime.of(2026, 3, 1, 12, 0),
                entry.supportingEvidence().get(0).reviewedAt());
    }

    // Scenario 7: an unpublished promise is invisible on every public surface.
    @Test
    void unpublishedPromiseIsInvisibleOnEveryPublicSurface() {

        Promise draft = promise(2L, "NOT_ASSESSED", Promise.DRAFT);

        when(promiseRepository.findAll()).thenReturn(List.of(draft));
        when(promiseRepository.findById(2L)).thenReturn(Optional.of(draft));
        when(currentUser.role()).thenReturn("SYSTEM");
        when(currentUser.email()).thenReturn(null);

        assertTrue(promiseService.getAllPromises().isEmpty());

        ResponseStatusException detailError = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.getPromiseById(2L)
        );
        assertEquals(404, detailError.getStatusCode().value());

        ResponseStatusException evidenceError = assertThrows(
                ResponseStatusException.class,
                () -> evidenceService.getApprovedEvidenceForPromise(2L)
        );
        assertEquals(404, evidenceError.getStatusCode().value());
        verify(evidenceRepository, never())
                .findByPromise_IdAndStatusOrderByUploadedAtDesc(
                        2L, "APPROVED");

        ResponseStatusException historyError = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.getPublicStatusHistory(2L)
        );
        assertEquals(404, historyError.getStatusCode().value());
        verify(statusChangeRepository, never())
                .findByPromiseIdOrderByCreatedAtAsc(2L);
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\dto\PublicPromiseResponse.java`

`$lang
package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public projection of a Promise.
 *
 * Creator identity (email and display name) is deliberately omitted: public
 * promise records are unattributed so that researcher contact details are never
 * exposed through public endpoints. Internal responses keep the creator fields.
 *
 * The implementation status and the verification state are reported
 * separately: a status of NOT_ASSESSED means the record has not been assessed
 * yet, which is not the same thing as evidence showing that nothing happened.
 */
public record PublicPromiseResponse(

        Long id,
        String title,
        String description,
        Long partyId,
        String partyName,
        Long electionId,
        String electionName,
        Integer electionYear,
        String state,
        String status,
        String verificationState,
        String category,
        String target,
        LocalDate targetDate,
        String geographicScope,
        List<PromiseSourceResponse> sources,
        boolean hasApprovedEvidence,
        long approvedEvidenceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {

    public PublicPromiseResponse {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (verificationState == null) {
            verificationState = "UNREVIEWED";
        }
    }
}

```

### `promiseproof-backend\src\main\java\promiseproof_backend\service\PromiseService.java`

`$lang
package promiseproof_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import promiseproof_backend.dto.PublicationDecisionRequest;
import promiseproof_backend.dto.PromiseRequest;
import promiseproof_backend.dto.PromiseResponse;
import promiseproof_backend.dto.PromiseSourceResponse;
import promiseproof_backend.dto.PromiseStatusCorrectionRequest;
import promiseproof_backend.dto.PublicPromiseResponse;
import promiseproof_backend.dto.PublicPromiseStatusHistoryResponse;
import promiseproof_backend.dto.PublicSupportingEvidenceResponse;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.Party;
import promiseproof_backend.entity.Election;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.PromiseStatusChange;
import promiseproof_backend.entity.Role;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.PartyRepository;
import promiseproof_backend.repository.ElectionRepository;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PromiseSourceRepository;
import promiseproof_backend.repository.PromiseStatusChangeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class PromiseService {

    /**
     * Every new promise starts unassessed. Missing information must never be
     * presented as evidence that implementation has not started.
     */
    public static final String INITIAL_STATUS = "NOT_ASSESSED";

    private static final List<String> VALID_STATUSES = List.of(
            "NOT_ASSESSED",
            "NOT_STARTED",
            "IN_PROGRESS",
            "COMPLETED",
            "DELAYED",
            "PARTIALLY_COMPLETED",
            "CANCELLED"
    );

    private static final Set<String> PUBLICATION_QUEUE_STATUSES = Set.of(
            Promise.SUBMITTED,
            Promise.REVIEWED
    );

    private static final String UNVERIFIED = "UNREVIEWED";

    private final PromiseRepository promiseRepository;
    private final PartyRepository partyRepository;
    private final ElectionRepository electionRepository;
    private final EvidenceRepository evidenceRepository;
    private final PromiseSourceRepository sourceRepository;
    private final PromiseStatusChangeRepository statusChangeRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PromiseService(
            PromiseRepository promiseRepository,
            PartyRepository partyRepository,
            ElectionRepository electionRepository,
            EvidenceRepository evidenceRepository,
            PromiseSourceRepository sourceRepository,
            PromiseStatusChangeRepository statusChangeRepository,
            CurrentUser currentUser,
            AuditService auditService) {

        this.promiseRepository = promiseRepository;
        this.partyRepository = partyRepository;
        this.electionRepository = electionRepository;
        this.evidenceRepository = evidenceRepository;
        this.sourceRepository = sourceRepository;
        this.statusChangeRepository = statusChangeRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    // Normalise and validate a promise status.
    private String resolveStatus(String status) {

        if (status == null || status.isBlank()) {
            return INITIAL_STATUS;
        }

        String normalised = status.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalised)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid promise status. Allowed values: " +
                            String.join(", ", VALID_STATUSES)
            );
        }

        return normalised;
    }

    /**
     * Promise create/update requests may not set or change the implementation
     * status. The only permitted value is the neutral initial status. Any other
     * change must go through the status verification workflow (or the audited
     * administrator correction endpoint).
     */
    private void rejectDirectStatusChange(
            String requestedStatus,
            String currentStatus) {

        if (requestedStatus == null || requestedStatus.isBlank()) {
            return;
        }

        String normalised = requestedStatus.trim().toUpperCase(Locale.ROOT);

        if (INITIAL_STATUS.equals(normalised)) {
            return;
        }

        if (currentStatus != null && currentStatus.equals(normalised)) {
            // No status change requested.
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Promise status is managed through the status verification " +
                        "workflow. Create promises without a status and use an " +
                        "approved status proposal to change it."
        );
    }

    private String verificationStateOf(Promise promise) {
        return promise.getVerificationState() == null
                ? UNVERIFIED
                : promise.getVerificationState();
    }

    private List<PromiseSourceResponse> sourcesOf(Promise promise) {
        return sourceRepository
                .findByPromise_IdOrderByCreatedAtAsc(promise.getId())
                .stream()
                .map(PromiseSourceService::toResponse)
                .toList();
    }

    // Convert Promise entity into a safe response object.
    private PromiseResponse convertToResponse(Promise promise) {

        PromiseResponse response = new PromiseResponse();

        response.setId(promise.getId());
        response.setTitle(promise.getTitle());
        response.setDescription(promise.getDescription());
        response.setState(promise.getState());
        response.setStatus(promise.getStatus());
        response.setVerificationState(verificationStateOf(promise));
        response.setPublicationStatus(publicationStatusOf(promise));
        response.setPublicationComment(promise.getPublicationComment());
        response.setCategory(promise.getCategory());
        response.setTarget(promise.getTarget());
        response.setTargetDate(promise.getTargetDate());
        response.setGeographicScope(promise.getGeographicScope());
        response.setSources(
                promise.getId() == null ? List.of() : sourcesOf(promise)
        );
        response.setHasApprovedEvidence(
                promise.getId() != null &&
                        evidenceRepository.countByPromise_IdAndStatus(
                                promise.getId(),
                                "APPROVED"
                        ) > 0
        );
        response.setCreatedAt(promise.getCreatedAt());
        response.setUpdatedAt(promise.getUpdatedAt());
        response.setCreatedByEmail(promise.getCreatedByEmail());
        response.setCreatedByName(promise.getCreatedByName());
        response.setSubmittedAt(promise.getSubmittedAt());
        response.setReviewedAt(promise.getReviewedAt());
        response.setPublishedAt(promise.getPublishedAt());

        if (promise.getParty() != null) {
            response.setPartyId(promise.getParty().getId());
            response.setPartyName(promise.getParty().getName());
        } else {
            response.setPartyName(promise.getPartyName());
        }

        if (promise.getElection() != null) {
            response.setElectionId(promise.getElection().getId());
            response.setElectionName(promise.getElection().getName());
        } else {
            response.setElectionName(promise.getElectionName());
        }

        return response;
    }

    /**
     * Public projection used by public promise endpoints. Creator identity is
     * intentionally excluded from public responses.
     */
    private PublicPromiseResponse toPublicResponse(Promise promise) {

        long approvedEvidenceCount = promise.getId() == null
                ? 0L
                : evidenceRepository.countByPromise_IdAndStatus(
                        promise.getId(),
                        "APPROVED"
                );

        return new PublicPromiseResponse(
                promise.getId(),
                promise.getTitle(),
                promise.getDescription(),
                promise.getParty() != null
                        ? promise.getParty().getId()
                        : null,
                promise.getParty() != null
                        ? promise.getParty().getName()
                        : promise.getPartyName(),
                promise.getElection() != null
                        ? promise.getElection().getId()
                        : null,
                promise.getElection() != null
                        ? promise.getElection().getName()
                        : promise.getElectionName(),
                promise.getElection() != null
                        ? promise.getElection().getYear()
                        : null,
                promise.getState(),
                promise.getStatus(),
                verificationStateOf(promise),
                promise.getCategory(),
                promise.getTarget(),
                promise.getTargetDate(),
                promise.getGeographicScope(),
                promise.getId() == null ? List.of() : sourcesOf(promise),
                approvedEvidenceCount > 0,
                approvedEvidenceCount,
                promise.getCreatedAt(),
                promise.getUpdatedAt(),
                promise.getPublishedAt()
        );
    }

    public static String publicationStatusOf(Promise promise) {
        return promise.getPublicationStatus() == null
                ? Promise.PUBLISHED
                : promise.getPublicationStatus();
    }

    private boolean canManage(Promise promise) {

        if (Role.ADMIN.name().equals(currentUser.role())) {
            return true;
        }

        String email = currentUser.email();

        return email != null &&
                promise.getCreatedByEmail() != null &&
                email.equalsIgnoreCase(promise.getCreatedByEmail());
    }

    private void assertCanManage(Promise promise) {
        if (!canManage(promise)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the researcher who recorded this promise or an " +
                            "administrator can modify it"
            );
        }
    }

    private void reopenForReview(Promise promise, String reason) {

        if (Promise.DRAFT.equals(publicationStatusOf(promise))) {
            return;
        }

        promise.setPublicationStatus(Promise.DRAFT);
        promise.setPublicationComment(
                "Returned to draft after " + reason
        );
        promise.setSubmittedAt(null);
        promise.setReviewedAt(null);
        promise.setPublishedAt(null);

        auditService.record(
                "PROMISE_REOPENED_FOR_REVIEW",
                "PROMISE",
                promise.getId(),
                promise.getTitle(),
                currentUser.email(),
                currentUser.role(),
                reason
        );
    }

    // Create a new Promise.
    @Transactional
    public PromiseResponse createPromise(PromiseRequest request) {

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promise title is required"
            );
        }

        // A new promise may not be created with an implementation status.
        rejectDirectStatusChange(request.getStatus(), null);

        Promise promise = new Promise();

        promise.setTitle(request.getTitle().trim());
        promise.setDescription(request.getDescription());
        promise.setState(request.getState());
        promise.setCategory(trimToNull(request.getCategory()));
        promise.setTarget(trimToNull(request.getTarget()));
        promise.setTargetDate(request.getTargetDate());
        promise.setGeographicScope(trimToNull(request.getGeographicScope()));
        promise.setCreatedByEmail(currentUser.email());
        promise.setCreatedByName(currentUser.displayName());

        promise.setStatus(INITIAL_STATUS);
        promise.setVerificationState(UNVERIFIED);

        // New records stay out of the public platform until a reviewer has
        // checked the original source and the exact wording.
        promise.setPublicationStatus(Promise.DRAFT);

        if (request.getPartyId() != null) {

            Party party = partyRepository.findById(request.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Selected party was not found"
                    ));

            promise.setParty(party);
            promise.setPartyName(party.getName());
        }

        if (request.getElectionId() != null) {

            Election election = electionRepository
                    .findById(request.getElectionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Selected election was not found"
                    ));

            promise.setElection(election);
            promise.setElectionName(election.getName());
        }

        Promise saved = promiseRepository.save(promise);

        auditService.record(
                "PROMISE_CREATED",
                "PROMISE",
                saved.getId(),
                saved.getTitle(),
                currentUser.email(),
                currentUser.role(),
                "Draft created"
        );

        return convertToResponse(saved);
    }

    // Get all published promises (public listing without creator details).
    @Transactional(readOnly = true)
    public List<PublicPromiseResponse> getAllPromises() {

        return promiseRepository.findAll()
                .stream()
                .filter(Promise::isPubliclyVisible)
                .map(this::toPublicResponse)
                .toList();
    }

    // Search and filter published promises with pagination. Kept for existing
    // callers; the citizen dashboard uses the category-aware variant below.
    @Transactional(readOnly = true)
    public Page<PublicPromiseResponse> searchPromises(
            String query,
            String status,
            Long partyId,
            Long electionId,
            String state,
            boolean onlyWithApprovedEvidence,
            Pageable pageable) {

        return searchPublicPromises(
                query,
                status,
                partyId,
                electionId,
                state,
                null,
                onlyWithApprovedEvidence,
                pageable
        );
    }

    /**
     * Public citizen listing. Publication visibility is enforced in the
     * specification so no draft, submitted or reviewed promise can leak.
     */
    @Transactional(readOnly = true)
    public Page<PublicPromiseResponse> searchPublicPromises(
            String query,
            String status,
            Long partyId,
            Long electionId,
            String state,
            String category,
            boolean onlyWithApprovedEvidence,
            Pageable pageable) {

        String normalisedStatus = null;

        if (status != null && !status.isBlank()) {
            normalisedStatus = resolveStatus(status);
        }

        final String queryText =
                query == null || query.isBlank()
                        ? null
                        : "%" + query.trim().toLowerCase() + "%";

        final String statusFilter = normalisedStatus;
        final String stateFilter =
                state == null || state.isBlank()
                        ? null
                        : state.trim();

        final String categoryFilter =
                category == null || category.isBlank()
                        ? null
                        : category.trim();

        Specification<Promise> specification = (root, criteriaQuery, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Drafts and records awaiting review never appear publicly.
            predicates.add(cb.or(
                    cb.isNull(root.get("publicationStatus")),
                    cb.equal(root.get("publicationStatus"), Promise.PUBLISHED)
            ));

            if (queryText != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), queryText),
                        cb.like(
                                cb.lower(cb.coalesce(
                                        root.get("description"), ""
                                )),
                                queryText
                        ),
                        cb.like(
                                cb.lower(cb.coalesce(
                                        root.get("partyName"), ""
                                )),
                                queryText
                        )
                ));
            }

            if (statusFilter != null) {
                predicates.add(cb.equal(
                        root.get("status"), statusFilter
                ));
            }

            if (partyId != null) {
                predicates.add(cb.equal(
                        root.get("party").get("id"), partyId
                ));
            }

            if (electionId != null) {
                predicates.add(cb.equal(
                        root.get("election").get("id"), electionId
                ));
            }

            if (stateFilter != null) {
                predicates.add(cb.equal(
                        cb.lower(root.get("state")),
                        stateFilter.toLowerCase()
                ));
            }

            if (categoryFilter != null) {
                predicates.add(cb.equal(
                        cb.lower(cb.coalesce(root.get("category"), "")),
                        categoryFilter.toLowerCase()
                ));
            }

            if (onlyWithApprovedEvidence) {

                Subquery<Long> subquery =
                        criteriaQuery.subquery(Long.class);

                Root<Evidence> evidenceRoot =
                        subquery.from(Evidence.class);

                subquery.select(cb.literal(1L)).where(
                        cb.equal(
                                evidenceRoot.get("promise").get("id"),
                                root.get("id")
                        ),
                        cb.equal(evidenceRoot.get("status"), "APPROVED")
                );

                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return promiseRepository
                .findAll(specification, pageable)
                .map(this::toPublicResponse);
    }

    // Promises created by the signed-in user (drafts included).
    @Transactional(readOnly = true)
    public List<PromiseResponse> getMyPromises(String email) {

        if (email == null || email.isBlank()) {
            return List.of();
        }

        return promiseRepository
                .findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get one published Promise by ID (public projection, no creator details).
    @Transactional(readOnly = true)
    public PublicPromiseResponse getPromiseById(Long id) {
        Promise promise = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        if (!promise.isPubliclyVisible() && !canManage(promise)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promise not found"
            );
        }

        return toPublicResponse(promise);
    }

    // Update an existing Promise. Only the owner or an administrator may do it.
    @Transactional
    public PromiseResponse updatePromise(
            Long id,
            PromiseRequest request) {

        Promise existing = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        assertCanManage(existing);

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promise title is required"
            );
        }

        existing.setTitle(request.getTitle().trim());
        existing.setDescription(request.getDescription());
        existing.setState(request.getState());
        existing.setCategory(trimToNull(request.getCategory()));
        existing.setTarget(trimToNull(request.getTarget()));
        existing.setTargetDate(request.getTargetDate());
        existing.setGeographicScope(trimToNull(request.getGeographicScope()));

        // The implementation status is not editable through this endpoint.
        // Use the status verification workflow (or, for administrators, the
        // audited correction endpoint).
        rejectDirectStatusChange(request.getStatus(), existing.getStatus());

        // Content changes invalidate a previous publication review.
        reopenForReview(existing, "promise wording or metadata edited");

        // Update Party only when a new Party ID is supplied.
        // Otherwise, preserve any existing legacy Party name.
        if (request.getPartyId() != null) {

            Party party = partyRepository.findById(request.getPartyId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Selected party was not found"
                    ));

            existing.setParty(party);
            existing.setPartyName(party.getName());
        }

        // Update Election only when a new Election ID is supplied.
        // Otherwise, preserve any existing legacy Election name.
        if (request.getElectionId() != null) {

            Election election = electionRepository
                    .findById(request.getElectionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Selected election was not found"
                    ));

            existing.setElection(election);
            existing.setElectionName(election.getName());
        }

        Promise saved = promiseRepository.save(existing);

        auditService.record(
                "PROMISE_UPDATED",
                "PROMISE",
                saved.getId(),
                saved.getTitle(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return convertToResponse(saved);
    }

    /**
     * Researcher submits a draft for publication review. At least one original
     * source with the exact quotation is mandatory.
     */
    @Transactional
    public PromiseResponse submitForPublication(Long id) {

        Promise promise = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        assertCanManage(promise);

        String current = publicationStatusOf(promise);

        if (Promise.DRAFT.equals(current) == false &&
                Promise.PUBLISHED.equals(current) == false) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This promise is already awaiting publication review"
            );
        }

        if (sourceRepository.countByPromise_Id(id) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Add at least one original source with the exact wording " +
                            "before submitting for publication"
            );
        }

        promise.setPublicationStatus(Promise.SUBMITTED);
        promise.setPublicationComment(null);
        promise.setSubmittedAt(LocalDateTime.now());
        promise.setReviewedAt(null);
        promise.setPublishedAt(null);

        Promise saved = promiseRepository.save(promise);

        auditService.record(
                "PROMISE_SUBMITTED_FOR_PUBLICATION",
                "PROMISE",
                saved.getId(),
                saved.getTitle(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return convertToResponse(saved);
    }

    /**
     * Reviewer/administrator publication decision. The reviewer who checks the
     * source and wording marks the promise REVIEWED; a second explicit action
     * publishes it. The creator cannot review or publish their own record.
     */
    @Transactional
    public PromiseResponse decidePublication(
            Long id,
            PublicationDecisionRequest request) {

        Promise promise = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        String actorEmail = currentUser.email();

        if (actorEmail != null &&
                promise.getCreatedByEmail() != null &&
                actorEmail.equalsIgnoreCase(promise.getCreatedByEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The researcher who recorded a promise cannot review or " +
                            "publish it"
            );
        }

        String decision = request == null || request.getDecision() == null
                ? null
                : request.getDecision().trim().toUpperCase(Locale.ROOT);

        if (decision == null ||
                !(decision.equals("REVIEWED") ||
                        decision.equals("PUBLISHED") ||
                        decision.equals("REJECTED"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Decision must be REVIEWED, PUBLISHED or REJECTED"
            );
        }

        String current = publicationStatusOf(promise);

        if ("REVIEWED".equals(decision)) {

            if (!Promise.SUBMITTED.equals(current)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only a submitted promise can be marked reviewed"
                );
            }

            promise.setPublicationStatus(Promise.REVIEWED);
            promise.setReviewedAt(LocalDateTime.now());
            promise.setPublicationComment(trimToNull(request.getComment()));

        } else if ("PUBLISHED".equals(decision)) {

            if (!Promise.REVIEWED.equals(current)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Mark the promise reviewed before publishing it"
                );
            }

            if (sourceRepository.countByPromise_Id(id) == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A promise cannot be published without an original " +
                                "source"
                );
            }

            promise.setPublicationStatus(Promise.PUBLISHED);
            promise.setPublishedAt(LocalDateTime.now());
            promise.setPublicationComment(trimToNull(request.getComment()));

        } else {

            if (request.getComment() == null ||
                    request.getComment().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please provide a reason for returning the promise " +
                                "to draft"
                );
            }

            promise.setPublicationStatus(Promise.DRAFT);
            promise.setSubmittedAt(null);
            promise.setReviewedAt(null);
            promise.setPublishedAt(null);
            promise.setPublicationComment(request.getComment().trim());
        }

        Promise saved = promiseRepository.save(promise);

        auditService.record(
                "PROMISE_PUBLICATION_" + decision,
                "PROMISE",
                saved.getId(),
                saved.getTitle(),
                currentUser.email(),
                currentUser.role(),
                request.getComment()
        );

        return convertToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PromiseResponse> getPublicationQueue() {

        return promiseRepository.findAll()
                .stream()
                .filter(promise -> PUBLICATION_QUEUE_STATUSES.contains(
                        publicationStatusOf(promise)
                ))
                .sorted((first, second) -> {
                    if (first.getSubmittedAt() == null) {
                        return 1;
                    }
                    if (second.getSubmittedAt() == null) {
                        return -1;
                    }
                    return first.getSubmittedAt().compareTo(
                            second.getSubmittedAt()
                    );
                })
                .map(this::convertToResponse)
                .toList();
    }

    // Delete a Promise. Published records are protected from owner deletion.
    @Transactional
    public void deletePromise(Long id) {

        Promise existing = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        assertCanManage(existing);

        if (!Role.ADMIN.name().equals(currentUser.role()) &&
                !Promise.DRAFT.equals(publicationStatusOf(existing))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an administrator can delete a promise that has been " +
                            "submitted, reviewed or published"
            );
        }

        long evidenceCount = evidenceRepository.countByPromise_Id(id);

        if (evidenceCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This promise has " + evidenceCount +
                            " evidence record(s) and cannot be deleted. " +
                            "Remove its evidence first."
            );
        }

        promiseRepository.delete(existing);

        auditService.record(
                "PROMISE_DELETED",
                "PROMISE",
                existing.getId(),
                existing.getTitle(),
                currentUser.email(),
                currentUser.role(),
                null
        );
    }

    /**
     * Administrator status correction: the audited exception path for fixing a
     * demonstrably wrong status. It records a status history row, a platform
     * audit entry with the mandatory reason, and resets verification so the
     * record must be verified again.
     */
    @Transactional
    public PromiseResponse correctStatus(
            Long id,
            PromiseStatusCorrectionRequest request) {

        Promise promise = promiseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        if (request == null ||
                request.getStatus() == null ||
                request.getStatus().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status is required"
            );
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reason is required for an administrative status " +
                            "correction"
            );
        }

        String newStatus = resolveStatus(request.getStatus());

        String fromStatus = promise.getStatus() == null
                ? INITIAL_STATUS
                : promise.getStatus();

        if (fromStatus.equals(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The promise already has the requested status"
            );
        }

        promise.setStatus(newStatus);
        // A correction invalidates the previous verification: the record must
        // be verified again through the normal workflow.
        promise.setVerificationState(UNVERIFIED);

        Promise saved = promiseRepository.save(promise);

        PromiseStatusChange change = new PromiseStatusChange();
        change.setPromiseId(saved.getId());
        change.setFromStatus(fromStatus);
        change.setToStatus(newStatus);
        change.setChangedByEmail(currentUser.email());
        change.setChangedByName(currentUser.displayName());
        change.setReason(request.getReason().trim());

        statusChangeRepository.save(change);

        auditService.record(
                "PROMISE_STATUS_ADMIN_CORRECTION",
                "PROMISE",
                saved.getId(),
                saved.getTitle(),
                currentUser.email(),
                currentUser.role(),
                "Administrative correction from " + fromStatus + " to " +
                        newStatus + ". Reason: " + request.getReason().trim()
        );

        return convertToResponse(saved);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Public tracking history. Only changes that were independently approved
     * are presented as verified facts. Administrator corrections and other
     * internal moderation actions are intentionally excluded.
     */
    @Transactional(readOnly = true)
    public List<PublicPromiseStatusHistoryResponse> getPublicStatusHistory(
            Long promiseId) {

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Promise not found"
                ));

        // Do not expose the history of unpublished promises.
        if (!promise.isPubliclyVisible()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Promise not found"
            );
        }

        return statusChangeRepository
                .findByPromiseIdOrderByCreatedAtAsc(promiseId)
                .stream()
                .filter(change ->
                        "APPROVED".equals(change.getReviewerDecision()))
                .map(change -> new PublicPromiseStatusHistoryResponse(
                        change.getId(),
                        change.getFromStatus(),
                        change.getToStatus(),
                        "Independent review",
                        change.getReason(),
                        change.getCreatedAt(),
                        change.getReviewerDecision(),
                        supportingEvidenceOf(change)
                ))
                .toList();
    }

    /**
     * Build the public reference to the approved evidence behind a status
     * decision. The evidence must still be approved and still belong to this
     * promise; otherwise no reference is exposed.
     */
    private List<PublicSupportingEvidenceResponse> supportingEvidenceOf(
            PromiseStatusChange change) {

        if (change.getEvidenceId() == null) {
            return List.of();
        }

        return evidenceRepository.findById(change.getEvidenceId())
                .filter(evidence ->
                        "APPROVED".equals(evidence.getStatus()))
                .filter(evidence ->
                        evidence.getPromise() != null &&
                                Objects.equals(
                                        evidence.getPromise().getId(),
                                        change.getPromiseId()
                                ))
                .map(evidence -> List.of(
                        new PublicSupportingEvidenceResponse(
                                evidence.getId(),
                                evidence.getTitle(),
                                evidence.getEvidenceType(),
                                evidence.getIssuingDepartment(),
                                evidence.getDocumentReference(),
                                evidence.getDocumentDate(),
                                evidence.getReviewedAt()
                        )
                ))
                .orElseGet(List::of);
    }
}

```

### `promiseproof-frontend\src\services\api.js`

`$lang
import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = API_BASE;

// Get all promises
export async function getPromises() {

    const response = await apiFetch(`${API_URL}/promises`);

    return handleResponse(response);
}

// Search, filter and paginate promises.
export async function searchPromises({
    q = "",
    status = "",
    partyId = "",
    electionId = "",
    state = "",
    withApprovedEvidence = false,
    page = 0,
    size = 9
} = {}) {

    const params = new URLSearchParams();

    if (q.trim()) params.set("q", q.trim());
    if (status) params.set("status", status);
    if (partyId) params.set("partyId", partyId);
    if (electionId) params.set("electionId", electionId);
    if (state.trim()) params.set("state", state.trim());
    if (withApprovedEvidence) params.set("withApprovedEvidence", "true");

    params.set("page", String(page));
    params.set("size", String(size));

    const response = await apiFetch(
        `${API_URL}/promises/search?${params.toString()}`
    );

    return handleResponse(response);
}

/**
 * Public citizen listing. The backend enforces publication visibility, so
 * drafts and unpublished promises can never be returned here.
 */
export async function getPublicPromises({
    q = "",
    status = "",
    partyId = "",
    electionId = "",
    state = "",
    category = "",
    withApprovedEvidence = false,
    sort = "recent",
    page = 0,
    size = 9
} = {}) {

    const params = new URLSearchParams();

    if (q.trim()) params.set("q", q.trim());
    if (status) params.set("status", status);
    if (partyId) params.set("partyId", partyId);
    if (electionId) params.set("electionId", electionId);
    if (state.trim()) params.set("state", state.trim());
    if (category.trim()) params.set("category", category.trim());
    if (withApprovedEvidence) params.set("withApprovedEvidence", "true");
    params.set("sort", sort);
    params.set("page", String(page));
    params.set("size", String(size));

    const response = await apiFetch(
        `${API_URL}/promises/public?${params.toString()}`
    );

    return handleResponse(response);
}

// Public detail of a published promise (creator identity omitted by backend).
export async function getPromise(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}`);

    return handleResponse(response);
}

// Public, independently reviewed status history for a published promise.
export async function getPromiseHistory(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}/history`);

    return handleResponse(response);
}

// Promises created by the signed-in user.
export async function getMyPromises() {

    const response = await apiFetch(`${API_URL}/promises/mine`);

    return handleResponse(response);
}

// Ask the server to suggest promises from pasted manifesto text.
export async function extractPromises(text) {

    const response = await apiFetch(`${API_URL}/promises/extract`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ text })
    });

    return handleResponse(response);
}

// Create a promise
export async function createPromise(promise) {

    const response = await apiFetch(`${API_URL}/promises`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(promise)
    });

    return handleResponse(response);
}

// Update a promise
export async function updatePromise(id, promise) {

    const response = await apiFetch(`${API_URL}/promises/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(promise)
    });

    return handleResponse(response);
}

// Delete a promise
export async function deletePromise(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}`, {
        method: "DELETE"
    });

    return handleResponse(response);
}

// Independent publication workflow -------------------------------

// Reviewer/administrator publication queue.
export async function getPublicationQueue() {

    const response = await apiFetch(
        `${API_URL}/promises/publication-queue`
    );

    return handleResponse(response);
}

// A researcher submits a draft for independent publication review.
export async function submitForPublication(id) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/submit-for-publication`,
        { method: "POST" }
    );

    return handleResponse(response);
}

// Reviewer/administrator publication decision.
export async function decidePublication(id, decision, comment) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/publication`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ decision, comment })
        }
    );

    return handleResponse(response);
}

// Original source tracking ---------------------------------------

export async function getPromiseSources(id) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources`
    );

    return handleResponse(response);
}

export async function createPromiseSource(id, source) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(source)
        }
    );

    return handleResponse(response);
}

export async function updatePromiseSource(id, sourceId, source) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources/${sourceId}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(source)
        }
    );

    return handleResponse(response);
}

export async function deletePromiseSource(id, sourceId) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources/${sourceId}`,
        { method: "DELETE" }
    );

    return handleResponse(response);
}

```

### `promiseproof-frontend\src\services\statsApi.js`

`$lang
import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/stats`;

// Public dashboard statistics.
export async function getStatsOverview() {

    const response = await apiFetch(`${API_URL}/overview`);

    return handleResponse(response);
}

// Public dashboard statistics (published promises only).
export async function getPublicStats() {

    const response = await apiFetch(`${API_URL}/public`);

    return handleResponse(response);
}

// Platform-wide evidence audit trail (administrators only).
export async function getRecentAudit(limit = 50) {

    const response = await apiFetch(
        `${API_BASE}/audit/evidence/recent?limit=${limit}`
    );

    return handleResponse(response);
}

```

### `promiseproof-frontend\src\components\citizen\statusInfo.js`

`$lang
/**
 * Public explanations for implementation statuses. These descriptions are
 * deliberately cautious: an approved document is not proof of completion and
 * a lack of assessment is not evidence of inaction.
 */
export const STATUS_DESCRIPTIONS = {
    NOT_ASSESSED: "Not enough verified information to assess progress yet. This is not evidence that nothing has happened.",
    NOT_STARTED: "No implementation has been established by the verified evidence.",
    IN_PROGRESS: "Implementation has begun and is supported by verified evidence.",
    DELAYED: "Implementation is behind its documented schedule.",
    COMPLETED: "Completion has been independently verified through the status review workflow.",
    PARTIALLY_COMPLETED: "Part of the commitment has been independently verified as complete.",
    CANCELLED: "The commitment was formally withdrawn or cancelled.",
    DISPUTED: "Available evidence or assessments conflict; the status is under dispute."
};

export const STATUS_LABELS = {
    NOT_ASSESSED: "Not yet assessed",
    NOT_STARTED: "Not started",
    IN_PROGRESS: "In progress",
    COMPLETED: "Completed",
    DELAYED: "Delayed",
    PARTIALLY_COMPLETED: "Partially completed",
    CANCELLED: "Cancelled",
    DISPUTED: "Disputed"
};

export function statusLabel(status) {
    return STATUS_LABELS[status] || status || "Not yet assessed";
}

export function statusDescription(status) {
    return STATUS_DESCRIPTIONS[status] ||
        "No verified assessment is available for this status.";
}

export function formatDate(value) {
    if (!value) return "";
    return new Date(value).toLocaleDateString();
}

export function formatDateTime(value) {
    if (!value) return "";
    return new Date(value).toLocaleString();
}

```

### `promiseproof-frontend\src\components\citizen\PublicPromiseCard.jsx`

`$lang
import StatusBadge, { VerificationBadge } from "../StatusBadge";
import { formatDate, statusDescription } from "./statusInfo";

/**
 * A published promise as seen by a citizen. Every field shown here comes from
 * the public API; no internal publication comment, creator identity or review
 * moderation detail is displayed.
 */
function PublicPromiseCard({
    promise,
    onViewDetails,
    onViewEvidence,
    onTrackProgress
}) {
    const approvedCount = Number(promise.approvedEvidenceCount || 0);
    const election = [promise.electionName, promise.electionYear]
        .filter(Boolean)
        .join(" · ");

    return (
        <article className="public-promise-card">
            <div className="public-card-head">
                <span className="public-card-id">PROMISE #{promise.id}</span>
                <div className="public-card-badges">
                    <StatusBadge status={promise.status} />
                    <VerificationBadge verificationState={promise.verificationState} />
                </div>
            </div>

            <h3 className="public-card-title">{promise.title}</h3>

            {promise.description && (
                <p className="public-card-description">{promise.description}</p>
            )}

            <p className="public-card-status-note">
                {statusDescription(promise.status)}
            </p>

            <dl className="public-card-meta">
                <div>
                    <dt>Party</dt>
                    <dd>{promise.partyName || "Not specified"}</dd>
                </div>
                <div>
                    <dt>Election</dt>
                    <dd>{election || "Not specified"}</dd>
                </div>
                <div>
                    <dt>State / scope</dt>
                    <dd>
                        {promise.state || promise.geographicScope || "Not specified"}
                    </dd>
                </div>
                <div>
                    <dt>Category</dt>
                    <dd>{promise.category || "Not specified"}</dd>
                </div>
                <div>
                    <dt>Target date</dt>
                    <dd>
                        {promise.targetDate
                            ? formatDate(promise.targetDate)
                            : promise.target || "Not available"}
                    </dd>
                </div>
                <div>
                    <dt>Approved evidence</dt>
                    <dd>
                        {approvedCount === 0
                            ? "No approved evidence yet"
                            : `${approvedCount} document${approvedCount === 1 ? "" : "s"}`}
                    </dd>
                </div>
            </dl>

            <div className="public-card-footer">
                <span>Updated {formatDate(promise.updatedAt) || "—"}</span>
                <div className="public-card-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => onViewDetails?.(promise)}
                    >
                        View Details
                    </button>
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => onViewEvidence?.(promise)}
                    >
                        View Approved Evidence
                    </button>
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() => onTrackProgress?.(promise)}
                    >
                        Track Progress
                    </button>
                </div>
            </div>
        </article>
    );
}

export default PublicPromiseCard;

```

### `promiseproof-frontend\src\components\citizen\ApprovedEvidenceList.jsx`

`$lang
import { useState } from "react";

import { downloadEvidence } from "../../services/evidenceApi";
import { formatDate, formatDateTime } from "./statusInfo";

function chronological(items) {
    return [...items].sort((a, b) => {
        const aTime = new Date(
            a.reviewedAt || a.uploadedAt || 0
        ).getTime();
        const bTime = new Date(
            b.reviewedAt || b.uploadedAt || 0
        ).getTime();
        return aTime - bTime;
    });
}

/**
 * Public, approved-evidence-only view. Pending, rejected and moderation
 * information never reaches this component because the backend strips it.
 */
function ApprovedEvidenceList({
    evidence = [],
    loading = false,
    error = "",
    compact = false
}) {
    const [downloadError, setDownloadError] = useState("");

    async function handleDownload(item) {
        try {
            setDownloadError("");
            await downloadEvidence(item.id, item.originalFileName);
        } catch (err) {
            setDownloadError(`Download failed: ${err.message}`);
        }
    }

    return (
        <div className={`approved-evidence${compact ? " compact" : ""}`}>
            <div className="approved-evidence-heading">
                <h4>Approved evidence</h4>
                <span className="public-count-chip">
                    {evidence.length} document{evidence.length === 1 ? "" : "s"}
                </span>
            </div>

            <p className="public-note">
                Only documents that passed independent review are listed here.
                Approving a document verifies the document itself; it does not
                change the implementation status of the promise.
            </p>

            {error && <p className="error-message">{error}</p>}
            {downloadError && <p className="error-message">{downloadError}</p>}

            {loading ? (
                <p className="info-message">Loading approved evidence...</p>
            ) : evidence.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No approved evidence available yet.</strong>
                    <p>
                        Evidence under review is not published, so no private or
                        pending documents are shown here.
                    </p>
                </div>
            ) : (
                <ol className="approved-evidence-list">
                    {chronological(evidence).map(item => (
                        <li key={item.id} className="approved-evidence-item">
                            <div className="approved-evidence-item-head">
                                <h5>{item.title}</h5>
                                <span className="evidence-status approved">
                                    APPROVED
                                </span>
                            </div>

                            {item.description && (
                                <p className="approved-evidence-description">
                                    {item.description}
                                </p>
                            )}

                            <dl className="approved-evidence-meta">
                                <div>
                                    <dt>Issuing department</dt>
                                    <dd>{item.issuingDepartment || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Reference number</dt>
                                    <dd>{item.documentReference || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Document date</dt>
                                    <dd>{formatDate(item.documentDate) || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Uploaded</dt>
                                    <dd>{formatDateTime(item.uploadedAt) || "—"}</dd>
                                </div>
                                <div>
                                    <dt>Reviewer approval</dt>
                                    <dd>{formatDateTime(item.reviewedAt) || "Not recorded"}</dd>
                                </div>
                                {item.claimScope && (
                                    <div className="wide">
                                        <dt>Claim scope</dt>
                                        <dd>{item.claimScope}</dd>
                                    </div>
                                )}
                            </dl>

                            {(item.sourceUrl || item.originalFileName) && (
                                <div className="approved-evidence-source">
                                    <span>Source: </span>
                                    {item.sourceUrl ? (
                                        <a
                                            href={item.sourceUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            {item.sourceUrl}
                                        </a>
                                    ) : (
                                        <span>{item.originalFileName}</span>
                                    )}
                                </div>
                            )}

                            <div className="approved-evidence-file">
                                <span>{item.originalFileName || "Document"}</span>
                                {item.id && (
                                    <button
                                        type="button"
                                        className="secondary-button"
                                        onClick={() => handleDownload(item)}
                                    >
                                        Download / view
                                    </button>
                                )}
                            </div>

                            {item.promiseTitle && (
                                <p className="approved-evidence-promise">
                                    Belongs to: {item.promiseTitle}
                                </p>
                            )}
                        </li>
                    ))}
                </ol>
            )}

            <p className="public-note muted">
                Evidence under review: pending and rejected documents are never
                published.
            </p>
        </div>
    );
}

export default ApprovedEvidenceList;

```

### `promiseproof-frontend\src\components\citizen\PromiseTrackingTimeline.jsx`

`$lang
import {
    formatDate,
    formatDateTime,
    statusDescription,
    statusLabel
} from "./statusInfo";

/**
 * Builds a public timeline from recorded events only. Nothing is inferred:
 * there are no invented progress percentages and no unapproved status changes.
 */
function buildEvents(promise, history, evidence) {
    const events = [];

    if (promise?.publishedAt) {
        events.push({
            key: `published-${promise.id}`,
            time: promise.publishedAt,
            kind: "publication",
            title: "Promise originally published",
            detail: "The promise passed independent publication review and became public."
        });
    }

    (evidence || []).forEach(item => {
        if (item.uploadedAt) {
            events.push({
                key: `submitted-${item.id}`,
                time: item.uploadedAt,
                kind: "evidence",
                title: "Evidence submitted",
                detail: `"${item.title}" was submitted for independent review.`
            });
        }

        if (item.reviewedAt) {
            events.push({
                key: `approved-${item.id}`,
                time: item.reviewedAt,
                kind: "evidence-approved",
                title: "Evidence approved",
                detail: `"${item.title}" passed document, source and claim review.`
            });
        }
    });

    (history || []).forEach(change => {
        events.push({
            key: `status-${change.id}`,
            time: change.changedAt,
            kind: "status",
            title: "Implementation status independently reviewed",
            fromStatus: change.fromStatus,
            toStatus: change.toStatus,
            detail: change.reason || "No review note was recorded.",
            reviewerDecision: change.reviewerDecision,
            reviewedBy: change.reviewedBy,
            supportingEvidence: change.supportingEvidence || []
        });
    });

    if (
        promise?.status === "COMPLETED" &&
        promise?.verificationState === "STATUS_VERIFIED"
    ) {
        events.push({
            key: `completed-${promise.id}`,
            time: null,
            kind: "completed",
            title: "Promise marked completed",
            detail: "Completion was independently verified through the status review workflow."
        });
    }

    return events.sort((a, b) => {
        if (!a.time && !b.time) return 0;
        if (!a.time) return 1;
        if (!b.time) return -1;
        return new Date(a.time).getTime() - new Date(b.time).getTime();
    });
}

function PromiseTrackingTimeline({
    promise,
    history = [],
    evidence = [],
    loading = false,
    error = ""
}) {
    const events = buildEvents(promise, history, evidence);

    return (
        <div className="tracking-timeline-section">
            <div className="approved-evidence-heading">
                <h4>Implementation tracking</h4>
                <span className="public-count-chip">
                    {history.length} verified status change
                    {history.length === 1 ? "" : "s"}
                </span>
            </div>

            <p className="public-note">
                This timeline contains only recorded, publicly reviewable events.
                Evidence approval and implementation-status verification are
                separate processes: approving a document never marks a promise
                complete by itself.
            </p>

            {error && <p className="error-message">{error}</p>}

            {loading ? (
                <p className="info-message">Loading tracking history...</p>
            ) : events.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No verified tracking events yet.</strong>
                    <p>
                        Published promises, approved evidence and independently
                        reviewed status changes will appear here as they are
                        recorded.
                    </p>
                </div>
            ) : (
                <ol className="tracking-timeline">
                    {events.map(event => (
                        <li key={event.key} className={`tracking-event ${event.kind}`}>
                            <span className="tracking-dot" aria-hidden="true" />
                            <div className="tracking-event-body">
                                <div className="tracking-event-head">
                                    <strong>{event.title}</strong>
                                    <time>
                                        {event.time
                                            ? formatDateTime(event.time)
                                            : "Verified"}
                                    </time>
                                </div>

                                {event.fromStatus && (
                                    <div className="tracking-status-change">
                                        <span>{statusLabel(event.fromStatus)}</span>
                                        <span aria-hidden="true">→</span>
                                        <strong>{statusLabel(event.toStatus)}</strong>
                                    </div>
                                )}

                                <p>{event.detail}</p>

                                {event.reviewerDecision && (
                                    <p className="tracking-decision">
                                        Reviewer decision: {event.reviewerDecision}
                                        {event.reviewedBy
                                            ? ` · ${event.reviewedBy}`
                                            : ""}
                                    </p>
                                )}

                                {event.supportingEvidence.length > 0 && (
                                    <div className="tracking-supporting-evidence">
                                        <strong>Supporting approved evidence:</strong>
                                        <ul>
                                            {event.supportingEvidence.map(item => (
                                                <li key={item.id}>
                                                    {item.title}
                                                    {item.issuingDepartment
                                                        ? ` — ${item.issuingDepartment}`
                                                        : ""}
                                                    {item.reviewedAt
                                                        ? ` (approved ${formatDate(item.reviewedAt)})`
                                                        : ""}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                            </div>
                        </li>
                    ))}
                </ol>
            )}

            <div className="tracking-legend">
                <h5>What the statuses mean</h5>
                <ul>
                    <li><strong>NOT_ASSESSED</strong> — {statusDescription("NOT_ASSESSED")}</li>
                    <li><strong>NOT_STARTED</strong> — {statusDescription("NOT_STARTED")}</li>
                    <li><strong>IN_PROGRESS</strong> — {statusDescription("IN_PROGRESS")}</li>
                    <li><strong>DELAYED</strong> — {statusDescription("DELAYED")}</li>
                    <li><strong>COMPLETED</strong> — {statusDescription("COMPLETED")}</li>
                    <li><strong>DISPUTED</strong> — {statusDescription("DISPUTED")}</li>
                </ul>
                <p className="public-note muted">
                    No progress percentage is displayed because no reliable,
                    independently verified progress values have been published.
                </p>
            </div>
        </div>
    );
}

export default PromiseTrackingTimeline;

```

### `promiseproof-frontend\src\components\citizen\PublicPromiseList.jsx`

`$lang
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getPublicPromises } from "../../services/api";
import { getElections } from "../../services/electionApi";
import { getParties } from "../../services/partyApi";
import PublicPromiseCard from "./PublicPromiseCard";

const STATUS_OPTIONS = [
    "NOT_ASSESSED",
    "NOT_STARTED",
    "IN_PROGRESS",
    "DELAYED",
    "COMPLETED",
    "PARTIALLY_COMPLETED",
    "DISPUTED"
];

const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0 };

/**
 * Section A of the citizen dashboard: published promises with search,
 * filtering, sorting and server-side pagination.
 */
function PublicPromiseList({ query = "", onQueryChange }) {
    const navigate = useNavigate();

    const [filters, setFilters] = useState({
        status: "",
        partyId: "",
        electionId: "",
        state: "",
        category: "",
        sort: "recent"
    });

    const [parties, setParties] = useState([]);
    const [elections, setElections] = useState([]);
    const [page, setPage] = useState(0);
    const [result, setResult] = useState(EMPTY_PAGE);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [debouncedQuery, setDebouncedQuery] = useState(query);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedQuery(query), 250);
        return () => clearTimeout(timer);
    }, [query]);

    useEffect(() => {
        let cancelled = false;

        async function loadOptions() {
            try {
                const [partyData, electionData] = await Promise.all([
                    getParties().catch(() => []),
                    getElections().catch(() => [])
                ]);
                if (!cancelled) {
                    setParties(Array.isArray(partyData) ? partyData : []);
                    setElections(Array.isArray(electionData) ? electionData : []);
                }
            } catch {
                // Filter dropdowns are optional; the promise list still works.
            }
        }

        loadOptions();
        return () => { cancelled = true; };
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError("");

                const data = await getPublicPromises({
                    q: debouncedQuery,
                    status: filters.status,
                    partyId: filters.partyId,
                    electionId: filters.electionId,
                    state: filters.state,
                    category: filters.category,
                    sort: filters.sort,
                    page,
                    size: 9
                });

                if (!cancelled) {
                    setResult(data || EMPTY_PAGE);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(`Could not load published promises: ${err.message}`);
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [debouncedQuery, filters.status, filters.partyId, filters.electionId,
        filters.state, filters.category, filters.sort, page]);

    function updateFilter(name, value) {
        setFilters(previous => ({ ...previous, [name]: value }));
        setPage(0);
    }

    function resetFilters() {
        setFilters({
            status: "",
            partyId: "",
            electionId: "",
            state: "",
            category: "",
            sort: "recent"
        });
        onQueryChange?.("");
        setPage(0);
    }

    const promises = Array.isArray(result.content) ? result.content : [];

    return (
        <div className="public-promise-list">
            <div className="panel-heading">
                <div>
                    <p className="eyebrow">PUBLIC RECORD</p>
                    <h3>Browse published promises</h3>
                </div>
                <span className="panel-tag">
                    {result.totalElements || 0} published
                </span>
            </div>

            <p className="panel-note">
                Only promises that passed independent publication review appear
                here. Drafts and unpublished records remain private.
            </p>

            <div className="public-filter-grid">
                <label>
                    <span>Search</span>
                    <input
                        type="search"
                        value={query}
                        placeholder="Title or keyword"
                        onChange={event => {
                            onQueryChange?.(event.target.value);
                            setPage(0);
                        }}
                    />
                </label>

                <label>
                    <span>Party</span>
                    <select
                        value={filters.partyId}
                        onChange={event => updateFilter("partyId", event.target.value)}
                    >
                        <option value="">All parties</option>
                        {parties.map(party => (
                            <option key={party.id} value={party.id}>
                                {party.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>Election</span>
                    <select
                        value={filters.electionId}
                        onChange={event => updateFilter("electionId", event.target.value)}
                    >
                        <option value="">All elections</option>
                        {elections.map(election => (
                            <option key={election.id} value={election.id}>
                                {election.name}
                                {election.year ? ` (${election.year})` : ""}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>State</span>
                    <input
                        type="text"
                        value={filters.state}
                        placeholder="Any state"
                        onChange={event => updateFilter("state", event.target.value)}
                    />
                </label>

                <label>
                    <span>Category</span>
                    <input
                        type="text"
                        value={filters.category}
                        placeholder="Any category"
                        onChange={event => updateFilter("category", event.target.value)}
                    />
                </label>

                <label>
                    <span>Implementation status</span>
                    <select
                        value={filters.status}
                        onChange={event => updateFilter("status", event.target.value)}
                    >
                        <option value="">All statuses</option>
                        {STATUS_OPTIONS.map(status => (
                            <option key={status} value={status}>
                                {status.replaceAll("_", " ")}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>Sort by</span>
                    <select
                        value={filters.sort}
                        onChange={event => updateFilter("sort", event.target.value)}
                    >
                        <option value="recent">Recently updated</option>
                        <option value="oldest">Oldest updated</option>
                    </select>
                </label>

                <button
                    type="button"
                    className="secondary-button public-reset"
                    onClick={resetFilters}
                >
                    Reset filters
                </button>
            </div>

            {error && <p className="error-message">{error}</p>}

            {loading ? (
                <p className="info-message">Loading published promises...</p>
            ) : promises.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No published promises match these filters.</strong>
                    <p>Try clearing a filter or searching for a different keyword.</p>
                </div>
            ) : (
                <div className="public-promise-grid">
                    {promises.map(promise => (
                        <PublicPromiseCard
                            key={promise.id}
                            promise={promise}
                            onViewDetails={item =>
                                navigate(`/promises/${item.id}`)}
                            onViewEvidence={item =>
                                navigate(`/promises/${item.id}?tab=evidence`)}
                            onTrackProgress={item =>
                                navigate(`/promises/${item.id}?tab=tracking`)}
                        />
                    ))}
                </div>
            )}

            {(result.totalPages || 0) > 1 && (
                <div className="public-pagination">
                    <button
                        type="button"
                        className="secondary-button"
                        disabled={page <= 0}
                        onClick={() => setPage(value => Math.max(0, value - 1))}
                    >
                        Previous
                    </button>
                    <span>
                        Page {page + 1} of {result.totalPages}
                    </span>
                    <button
                        type="button"
                        className="secondary-button"
                        disabled={page + 1 >= result.totalPages}
                        onClick={() => setPage(value => value + 1)}
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
}

export default PublicPromiseList;

```

### `promiseproof-frontend\src\pages\PromiseDetails.jsx`

`$lang
import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

import StatusBadge, { VerificationBadge } from "../components/StatusBadge";
import ApprovedEvidenceList from "../components/citizen/ApprovedEvidenceList";
import PromiseTrackingTimeline from "../components/citizen/PromiseTrackingTimeline";
import {
    formatDate,
    statusDescription,
    statusLabel
} from "../components/citizen/statusInfo";
import { getPromise, getPromiseHistory } from "../services/api";
import { getApprovedEvidenceForPromise } from "../services/evidenceApi";
import "../styles/citizen.css";

const TABS = [
    { key: "details", label: "Promise Details" },
    { key: "sources", label: "Original Sources" },
    { key: "evidence", label: "Approved Evidence" },
    { key: "tracking", label: "Implementation Tracking" },
    { key: "history", label: "Status History" }
];

function PromiseDetails() {
    const { id } = useParams();
    const [searchParams, setSearchParams] = useSearchParams();

    const requestedTab = searchParams.get("tab");
    const activeTab = TABS.some(tab => tab.key === requestedTab)
        ? requestedTab
        : "details";

    const [promise, setPromise] = useState(null);
    const [evidence, setEvidence] = useState([]);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError("");

                const [promiseData, evidenceData, historyData] =
                    await Promise.all([
                        getPromise(id),
                        getApprovedEvidenceForPromise(id).catch(() => []),
                        getPromiseHistory(id).catch(() => [])
                    ]);

                if (!cancelled) {
                    setPromise(promiseData);
                    setEvidence(Array.isArray(evidenceData) ? evidenceData : []);
                    setHistory(Array.isArray(historyData) ? historyData : []);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(
                        `This promise is not publicly available: ${err.message}`
                    );
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [id]);

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading promise details...</p>
            </section>
        );
    }

    if (error || !promise) {
        return (
            <section className="dashboard-page">
                <div className="panel">
                    <h3>Promise unavailable</h3>
                    <p className="error-message">
                        {error || "This promise is not publicly available."}
                    </p>
                    <Link to="/promises" className="secondary-button">
                        Back to published promises
                    </Link>
                </div>
            </section>
        );
    }

    return (
        <section className="dashboard-page promise-detail-page">
            <Link to="/promises" className="dashboard-sidebar-link">
                ← Back to published promises
            </Link>

            <div className="page-heading promise-detail-heading">
                <div>
                    <p className="eyebrow">PROMISE #{promise.id}</p>
                    <h2>{promise.title}</h2>
                    <div className="public-card-badges">
                        <StatusBadge status={promise.status} />
                        <VerificationBadge
                            verificationState={promise.verificationState}
                        />
                    </div>
                </div>
            </div>

            <p className="public-note">
                {statusDescription(promise.status)}
            </p>

            <div className="promise-detail-tabs" role="tablist">
                {TABS.map(tab => (
                    <button
                        key={tab.key}
                        type="button"
                        role="tab"
                        aria-selected={activeTab === tab.key}
                        className={`promise-detail-tab ${
                            activeTab === tab.key ? "active" : ""
                        }`}
                        onClick={() => setSearchParams({ tab: tab.key })}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="panel promise-detail-panel">
                {activeTab === "details" && (
                    <div>
                        <p className="panel-note">{promise.description}</p>
                        <dl className="public-card-meta detailed">
                            <div>
                                <dt>Political party</dt>
                                <dd>{promise.partyName || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Election</dt>
                                <dd>
                                    {promise.electionName || "Not specified"}
                                    {promise.electionYear
                                        ? ` (${promise.electionYear})`
                                        : ""}
                                </dd>
                            </div>
                            <div>
                                <dt>State / scope</dt>
                                <dd>
                                    {promise.state ||
                                        promise.geographicScope ||
                                        "Not specified"}
                                </dd>
                            </div>
                            <div>
                                <dt>Category</dt>
                                <dd>{promise.category || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Target</dt>
                                <dd>{promise.target || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Target completion date</dt>
                                <dd>
                                    {formatDate(promise.targetDate) ||
                                        "Not available"}
                                </dd>
                            </div>
                            <div>
                                <dt>Implementation status</dt>
                                <dd>{statusLabel(promise.status)}</dd>
                            </div>
                            <div>
                                <dt>Last updated</dt>
                                <dd>{formatDate(promise.updatedAt) || "—"}</dd>
                            </div>
                            <div>
                                <dt>Originally published</dt>
                                <dd>
                                    {formatDate(promise.publishedAt) || "—"}
                                </dd>
                            </div>
                            <div>
                                <dt>Approved evidence</dt>
                                <dd>
                                    {promise.approvedEvidenceCount || 0}
                                </dd>
                            </div>
                        </dl>
                    </div>
                )}

                {activeTab === "sources" && (
                    <div>
                        <h3>Original sources</h3>
                        <p className="panel-note">
                            Sources preserve the manifesto or document name,
                            location, exact quotation and publication date used
                            when the promise was recorded.
                        </p>
                        {(promise.sources || []).length === 0 ? (
                            <p className="empty-note">
                                No original sources are attached to this promise.
                            </p>
                        ) : (
                            <ol className="public-source-list">
                                {promise.sources.map(source => (
                                    <li key={source.id}>
                                        <strong>{source.manifestoName}</strong>
                                        <span>
                                            {(source.sourceType || "MANIFESTO")
                                                .replaceAll("_", " ")}
                                            {source.pageNumber
                                                ? ` · page ${source.pageNumber}`
                                                : ""}
                                            {source.publishedDate
                                                ? ` · ${formatDate(source.publishedDate)}`
                                                : ""}
                                        </span>
                                        <blockquote>
                                            {source.exactQuotation}
                                        </blockquote>
                                        {source.sourceUrl && (
                                            <a
                                                href={source.sourceUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                            >
                                                View original source
                                            </a>
                                        )}
                                    </li>
                                ))}
                            </ol>
                        )}
                    </div>
                )}

                {activeTab === "evidence" && (
                    <ApprovedEvidenceList evidence={evidence} />
                )}

                {activeTab === "tracking" && (
                    <PromiseTrackingTimeline
                        promise={promise}
                        history={history}
                        evidence={evidence}
                    />
                )}

                {activeTab === "history" && (
                    <div>
                        <h3>Status history</h3>
                        <p className="panel-note">
                            Only independently approved status changes are shown.
                            Internal corrections and unapproved proposals are
                            not public facts.
                        </p>
                        {history.length === 0 ? (
                            <p className="empty-note">
                                No independently reviewed status changes have
                                been recorded for this promise.
                            </p>
                        ) : (
                            <div className="table-wrap">
                                <table className="admin-table">
                                    <thead>
                                        <tr>
                                            <th>Previous status</th>
                                            <th>New status</th>
                                            <th>Date</th>
                                            <th>Review note</th>
                                            <th>Decision</th>
                                            <th>Supporting evidence</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {history.map(change => (
                                            <tr key={change.id}>
                                                <td>{statusLabel(change.fromStatus)}</td>
                                                <td>{statusLabel(change.toStatus)}</td>
                                                <td>{formatDate(change.changedAt)}</td>
                                                <td>{change.reason || "—"}</td>
                                                <td>{change.reviewerDecision || "—"}</td>
                                                <td>
                                                    {(change.supportingEvidence || [])
                                                        .map(item => item.title)
                                                        .join(", ") || "—"}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </section>
    );
}

export default PromiseDetails;

```

### `promiseproof-frontend\src\App.jsx`

`$lang

import {
    Navigate,
    Route,
    Routes,
    useLocation
} from "react-router-dom";

import Navbar from "./components/Navbar";
import HomePage from "./pages/HomePage";
import PartyPage from "./pages/PartyPage";
import ElectionPage from "./pages/ElectionPage";
import Login from "./pages/Login";
import Register from "./pages/Register";
import DashboardPage from "./pages/DashboardPage";
import PromiseDetails from "./pages/PromiseDetails";
import ReviewQueuePage from "./pages/ReviewQueuePage";
import AdminPage from "./pages/AdminPage";
import { useAuth } from "./auth/useAuth";

import "./App.css";
import "./styles/dashboard.css";

const ROLE_PATHS = {
    citizen: "CITIZEN",
    researcher: "RESEARCHER",
    reviewer: "REVIEWER",
    admin: "ADMIN"
};

function LoadingScreen() {
    return <div className="auth-loading">Checking your session...</div>;
}

function WelcomePage() {
    const { user } = useAuth();

    if (user) {
        return <Navigate to="/dashboard" replace />;
    }

    return (
        <div className="welcome-page">
            <nav className="welcome-nav">
                <div className="welcome-brand">
                    <span className="brand-mark">P</span>
                    PromiseProof
                </div>
                <a href="/login">Sign in</a>
            </nav>

            <section className="welcome-hero">
                <div className="welcome-copy">
                    <span className="welcome-label">
                        PUBLIC PROMISE ACCOUNTABILITY
                    </span>

                    <h1>
                        Every public promise.
                        <span> Backed by proof.</span>
                    </h1>

                    <p>
                        PromiseProof helps people track public commitments
                        through original manifestos, official documents,
                        verified evidence and independent review.
                    </p>

                    <div className="welcome-actions">
                        <a className="welcome-primary" href="/login">
                            Sign in to continue
                        </a>
                        <a className="welcome-secondary" href="/register">
                            Create citizen account
                        </a>
                    </div>

                    <p className="welcome-note">
                        Independent. Evidence-based. Politically neutral.
                    </p>
                </div>

                <div className="welcome-visual">
                    <div className="visual-topline">
                        <span>ACCOUNTABILITY WORKFLOW</span>
                        <span className="live-dot">● SYSTEM</span>
                    </div>

                    <div className="visual-step">
                        <span className="visual-number">01</span>
                        <div>
                            <strong>Original commitment</strong>
                            <small>Manifesto and official source</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">02</span>
                        <div>
                            <strong>Supporting evidence</strong>
                            <small>Documents and source verification</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">03</span>
                        <div>
                            <strong>Independent review</strong>
                            <small>Evidence and status decisions</small>
                        </div>
                    </div>

                    <div className="visual-line" />

                    <div className="visual-step">
                        <span className="visual-number">04</span>
                        <div>
                            <strong>Public accountability record</strong>
                            <small>Traceable decisions and history</small>
                        </div>
                    </div>
                </div>
            </section>

            <section className="role-entry">
                <div>
                    <span className="welcome-label">ACCESS PORTAL</span>
                    <h2>Choose your workspace</h2>
                    <p>Sign in using the account assigned to your role.</p>
                </div>

                <div className="role-entry-grid">
                    {Object.entries({
                        citizen: ["Citizen", "Submit evidence and follow contributions"],
                        researcher: ["Researcher", "Record promises and source documents"],
                        reviewer: ["Reviewer", "Review evidence and status proposals"],
                        admin: ["Administrator", "Manage accounts and platform records"]
                    }).map(([path, details]) => (
                        <a
                            className="role-entry-card"
                            href={`/login/${path}`}
                            key={path}
                        >
                            <strong>{details[0]}</strong>
                            <span>{details[1]}</span>
                            <b>Continue →</b>
                        </a>
                    ))}
                </div>
            </section>

            <footer className="welcome-footer">
                PromiseProof · Evidence before conclusions.
            </footer>
        </div>
    );
}

function RequireAuth({ children }) {
    const { user, loading } = useAuth();
    const location = useLocation();

    if (loading) return <LoadingScreen />;

    if (!user) {
        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location.pathname }}
            />
        );
    }

    return children;
}

function RequireRole({ roles, children }) {
    const { user, loading } = useAuth();

    if (loading) return <LoadingScreen />;

    if (!user) return <Navigate to="/login" replace />;

    if (!roles.includes(user.role)) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

function RoleLogin() {
    const { user, loading } = useAuth();
    const location = useLocation();

    const roleKey = location.pathname.split("/").pop();
    const expectedRole = ROLE_PATHS[roleKey];

    if (loading) return <LoadingScreen />;

    if (user) return <Navigate to="/dashboard" replace />;

    if (!expectedRole) return <Navigate to="/login" replace />;

    return <Login expectedRole={expectedRole} />;
}

function App() {
    const { user, loading } = useAuth();

    if (loading) return <LoadingScreen />;

    const isAdmin = user?.role === "ADMIN";

    return (
        <div className="app">
            {user && <Navbar />}

            <main className={user ? "main-container" : "guest-container"}>
                <Routes>
                    <Route path="/" element={<WelcomePage />} />

                    <Route path="/login" element={<Navigate to="/login/citizen" replace />} />

                    <Route path="/login/:role" element={<RoleLogin />} />

                    <Route
                        path="/register"
                        element={user ? <Navigate to="/dashboard" replace /> : <Register />}
                    />

                    <Route
                        path="/dashboard"
                        element={
                            <RequireAuth>
                                <DashboardPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/promises"
                        element={
                            <RequireAuth>
                                <HomePage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/promises/:id"
                        element={
                            <RequireAuth>
                                <PromiseDetails />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/parties"
                        element={
                            <RequireRole roles={["ADMIN", "RESEARCHER", "REVIEWER", "CITIZEN"]}>
                                <PartyPage canManage={isAdmin} />
                            </RequireRole>
                        }
                    />

                    <Route
                        path="/elections"
                        element={
                            <RequireAuth>
                                <ElectionPage canManage={isAdmin} />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/review"
                        element={
                            <RequireRole roles={["REVIEWER", "ADMIN"]}>
                                <ReviewQueuePage />
                            </RequireRole>
                        }
                    />

                    <Route
                        path="/admin"
                        element={
                            <RequireRole roles={["ADMIN"]}>
                                <AdminPage />
                            </RequireRole>
                        }
                    />

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </main>
        </div>
    );
}

export default App;

```

### `promiseproof-frontend\src\components\Navbar.jsx`

`$lang
import { Link, NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import NotificationBell from "./NotificationBell";

function navClass({ isActive }) {
    return isActive ? "nav-tab active" : "nav-tab";
}

function Navbar() {

    const { user, logout, loading } = useAuth();
    const navigate = useNavigate();

    const canReview = Boolean(
        user && (user.role === "REVIEWER" || user.role === "ADMIN")
    );

    const isAdmin = Boolean(user && user.role === "ADMIN");
    const isCitizen = Boolean(user && user.role === "CITIZEN");

    async function handleLogout() {

        try {
            await logout();
        } finally {
            navigate("/");
        }
    }

    return (
        <header className="navbar">

            <Link to="/dashboard" className="brand">
                <span className="brand-icon">P</span>
                <span>PromiseProof</span>
            </Link>

            <nav className="nav-tabs" aria-label="Main navigation">

                <NavLink to="/" end className={navClass}>
                    Home
                </NavLink>

                <NavLink to="/promises" end className={navClass}>
                    Browse Promises
                </NavLink>

                {isCitizen && (
                    <Link to="/dashboard?tab=submissions" className="nav-tab">
                        My Evidence
                    </Link>
                )}

                {isCitizen && (
                    <Link to="/dashboard?tab=account" className="nav-tab">
                        My Account
                    </Link>
                )}

                <NavLink to="/parties" className={navClass}>
                    Parties
                </NavLink>

                <NavLink to="/elections" className={navClass}>
                    Elections
                </NavLink>

                {user && (
                    <NavLink to="/dashboard" className={navClass}>
                        Dashboard
                    </NavLink>
                )}

                {canReview && (
                    <NavLink to="/review" className={navClass}>
                        Review
                    </NavLink>
                )}

                {isAdmin && (
                    <NavLink to="/admin" className={navClass}>
                        Admin
                    </NavLink>
                )}

            </nav>

            <div className="nav-auth">

                {loading ? null : user ? (
                    <>
                        <NotificationBell />

                        <div className="nav-user">
                            <span className="nav-user-name">
                                {user.name}
                            </span>
                            <span className="role-chip">
                                {user.role}
                            </span>
                        </div>

                        <button
                            type="button"
                            className="nav-link"
                            onClick={handleLogout}
                        >
                            Logout
                        </button>
                    </>
                ) : (
                    <>
                        <Link className="nav-link" to="/login">
                            Sign in
                        </Link>

                        <Link
                            className="nav-link nav-link-primary"
                            to="/register"
                        >
                            Register
                        </Link>
                    </>
                )}

            </div>

        </header>
    );
}

export default Navbar;

```

### `promiseproof-frontend\src\styles\citizen.css`

`$lang
/* Citizen public-record views: promises, approved evidence and tracking. */

.public-note {
    margin: 0 0 16px;
    color: #475569;
    font-size: 0.94rem;
    line-height: 1.55;
}

.public-note.muted {
    color: #64748b;
    font-size: 0.86rem;
    margin-top: 14px;
    margin-bottom: 0;
}

.public-count-chip {
    display: inline-flex;
    align-items: center;
    padding: 5px 12px;
    border-radius: 999px;
    background: #eff6ff;
    color: #1d4ed8;
    font-size: 0.8rem;
    font-weight: 700;
}

.public-empty-state {
    padding: 28px;
    border: 1px dashed #cbd5e1;
    border-radius: 12px;
    background: #f8fafc;
    text-align: center;
}

.public-empty-state strong {
    display: block;
    color: #1e293b;
    margin-bottom: 6px;
}

.public-empty-state p {
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
}

/* Search bar in the welcome area. */
.citizen-search {
    display: flex;
    gap: 10px;
    margin-top: 18px;
    max-width: 640px;
}

.citizen-search input {
    flex: 1;
    padding: 11px 14px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    font-size: 0.95rem;
}

/* Published promise list ------------------------------------------------- */

.public-promise-list {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.public-filter-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 14px;
    padding: 18px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    background: #f8fafc;
}

.public-filter-grid label {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.public-filter-grid label > span {
    color: #475569;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.public-filter-grid input,
.public-filter-grid select {
    width: 100%;
    padding: 9px 10px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    background: #fff;
    font-size: 0.9rem;
}

.public-reset {
    align-self: end;
}

.public-promise-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
    gap: 18px;
}

.public-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 10px 0;
    color: #475569;
    font-size: 0.9rem;
}

.public-promise-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 20px;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    background: #fff;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}

.public-card-head,
.public-card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    flex-wrap: wrap;
}

.public-card-id {
    color: #64748b;
    font-size: 0.75rem;
    font-weight: 800;
    letter-spacing: 0.06em;
}

.public-card-badges {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.public-card-title {
    margin: 0;
    color: #0f172a;
    font-size: 1.15rem;
    line-height: 1.35;
}

.public-card-description {
    margin: 0;
    color: #475569;
    font-size: 0.9rem;
    line-height: 1.55;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.public-card-status-note {
    margin: 0;
    padding: 9px 12px;
    border-left: 3px solid #93c5fd;
    background: #eff6ff;
    color: #1e3a8a;
    font-size: 0.84rem;
    line-height: 1.5;
}

.public-card-meta {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 10px 14px;
    margin: 0;
}

.public-card-meta > div {
    min-width: 0;
}

.public-card-meta dt {
    color: #64748b;
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
}

.public-card-meta dd {
    margin: 2px 0 0;
    color: #1e293b;
    font-size: 0.88rem;
    overflow-wrap: anywhere;
}

.public-card-footer {
    padding-top: 12px;
    border-top: 1px solid #edf0f5;
    color: #64748b;
    font-size: 0.8rem;
}

.public-card-actions {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.public-card-actions button {
    font-size: 0.82rem;
    padding: 8px 12px;
}

/* Approved evidence ------------------------------------------------------ */

.approved-evidence-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.approved-evidence-heading h4 {
    margin: 0;
    color: #0f172a;
    font-size: 1.05rem;
}

.approved-evidence-list {
    margin: 0;
    padding: 0 0 0 22px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.approved-evidence-item {
    padding: 16px;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    background: #fff;
}

.approved-evidence-item-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.approved-evidence-item-head h5 {
    margin: 0;
    color: #0f172a;
    font-size: 1rem;
}

.approved-evidence-description {
    margin: 8px 0;
    color: #475569;
    font-size: 0.9rem;
}

.approved-evidence-meta {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 10px 16px;
    margin: 12px 0;
}

.approved-evidence-meta dt {
    color: #64748b;
    font-size: 0.72rem;
    font-weight: 800;
    text-transform: uppercase;
}

.approved-evidence-meta dd {
    margin: 2px 0 0;
    color: #1e293b;
    font-size: 0.86rem;
    overflow-wrap: anywhere;
}

.approved-evidence-meta .wide {
    grid-column: 1 / -1;
}

.approved-evidence-source {
    font-size: 0.84rem;
    color: #475569;
    overflow-wrap: anywhere;
}

.approved-evidence-source a {
    color: #1d4ed8;
}

.approved-evidence-file {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid #edf0f5;
    font-size: 0.84rem;
    color: #334155;
}

.approved-evidence-promise {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 0.8rem;
}

.evidence-status.approved {
    padding: 3px 10px;
    border-radius: 999px;
    background: #dcfce7;
    color: #166534;
    font-size: 0.72rem;
    font-weight: 800;
}

/* Tracking timeline ------------------------------------------------------ */

.tracking-timeline-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.tracking-timeline {
    list-style: none;
    margin: 0;
    padding: 0 0 0 22px;
    border-left: 2px solid #cbd5e1;
}

.tracking-event {
    position: relative;
    padding: 0 0 22px 18px;
}

.tracking-event:last-child {
    padding-bottom: 0;
}

.tracking-dot {
    position: absolute;
    left: -29px;
    top: 4px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #3b82f6;
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px #bfdbfe;
}

.tracking-event.evidence-approved .tracking-dot,
.tracking-event.completed .tracking-dot {
    background: #16a34a;
    box-shadow: 0 0 0 2px #bbf7d0;
}

.tracking-event-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
}

.tracking-event-head strong {
    color: #0f172a;
    font-size: 0.96rem;
}

.tracking-event-head time {
    color: #64748b;
    font-size: 0.8rem;
}

.tracking-event-body p {
    margin: 6px 0 0;
    color: #475569;
    font-size: 0.88rem;
    line-height: 1.5;
}

.tracking-status-change {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 6px;
    color: #475569;
    font-size: 0.86rem;
}

.tracking-status-change strong {
    color: #166534;
}

.tracking-decision {
    color: #1d4ed8 !important;
    font-weight: 600;
}

.tracking-supporting-evidence {
    margin-top: 8px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f1f5f9;
    font-size: 0.84rem;
}

.tracking-supporting-evidence ul {
    margin: 6px 0 0;
    padding-left: 18px;
}

.tracking-legend {
    padding: 16px;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    background: #f8fafc;
}

.tracking-legend h5 {
    margin: 0 0 8px;
    color: #0f172a;
}

.tracking-legend ul {
    margin: 0;
    padding-left: 18px;
    color: #475569;
    font-size: 0.84rem;
    line-height: 1.7;
}

/* Promise detail page ---------------------------------------------------- */

.promise-detail-page {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.promise-detail-heading {
    margin-bottom: 0;
}

.promise-detail-tabs {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 8px;
}

.promise-detail-tab {
    padding: 9px 14px;
    border: 1px solid transparent;
    border-radius: 8px;
    background: transparent;
    color: #475569;
    font-size: 0.88rem;
    font-weight: 600;
    cursor: pointer;
}

.promise-detail-tab.active {
    background: #eff6ff;
    border-color: #bfdbfe;
    color: #1d4ed8;
}

.promise-detail-panel {
    min-height: 180px;
}

.public-card-meta.detailed {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    padding: 16px 0;
}

.public-source-list {
    margin: 0;
    padding-left: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.public-source-list li {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.public-source-list strong {
    color: #0f172a;
}

.public-source-list span {
    color: #64748b;
    font-size: 0.82rem;
}

.public-source-list blockquote {
    margin: 0;
    padding: 10px 12px;
    border-left: 3px solid #93c5fd;
    background: #eff6ff;
    color: #1e3a8a;
    font-size: 0.88rem;
}

.public-source-list a {
    color: #1d4ed8;
    font-size: 0.84rem;
}

/* Citizen dashboard additions ------------------------------------------- */

.public-summary {
    margin-top: -8px;
}

.tracking-promise-picker {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-width: 620px;
    margin: 4px 0 18px;
}

.tracking-promise-picker span {
    color: #475569;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.tracking-promise-picker select {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    background: #fff;
    font-size: 0.92rem;
}

.tracking-selected-summary {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    padding: 16px;
    margin-bottom: 18px;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    background: #f8fafc;
}

.tracking-selected-summary strong {
    color: #0f172a;
    font-size: 1.05rem;
}

.tracking-selected-summary .public-note {
    margin: 6px 0 0;
}

.tracking-public-sections {
    display: flex;
    flex-direction: column;
    gap: 28px;
}

.account-details {
    padding: 16px 0;
}

.account-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    padding-top: 12px;
    border-top: 1px solid #edf0f5;
}

@media (max-width: 900px) {
    .public-promise-grid {
        grid-template-columns: 1fr;
    }

    .citizen-search {
        flex-direction: column;
    }

    .tracking-selected-summary {
        flex-direction: column;
    }
}

```

### `promiseproof-frontend\src\dashboards\CitizenDashboard.jsx`

`$lang
import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import StatusBadge, { VerificationBadge } from "../components/StatusBadge";
import ApprovedEvidenceList from "../components/citizen/ApprovedEvidenceList";
import PromiseTrackingTimeline from "../components/citizen/PromiseTrackingTimeline";
import PublicPromiseList from "../components/citizen/PublicPromiseList";
import { statusDescription } from "../components/citizen/statusInfo";
import {
    downloadEvidence,
    getApprovedEvidenceForPromise,
    getMyEvidence,
    resubmitEvidence,
    uploadEvidence
} from "../services/evidenceApi";
import { getPromiseHistory } from "../services/api";
import {
    getNotifications,
    markAllNotificationsRead,
    markNotificationRead
} from "../services/notificationApi";
import { getPromises } from "../services/api";
import { getPublicStats } from "../services/statsApi";
import "../styles/citizen.css";

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "";
}

function formatRelative(value) {
    if (!value) return "";
    const then = new Date(value).getTime();
    const seconds = Math.max(0, Math.round((Date.now() - then) / 1000));
    if (seconds < 60) return "just now";
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.round(hours / 24);
    if (days < 7) return `${days}d ago`;
    return new Date(value).toLocaleDateString();
}

function statusClass(status) {
    return String(status || "").toLowerCase();
}

function displayCount(value) {
    return value === null || value === undefined ? "—" : value;
}

const DASHBOARD_TABS = [
    "browse", "submissions", "progress", "notifications",
    "submit", "tracking", "account"
];

function CitizenDashboard() {
    const { user } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();

    // The URL is the source of truth so top-nav deep links and sidebar clicks
    // always agree. Unknown tabs fall back to the published-promise listing.
    const requestedTab = searchParams.get("tab");
    const activeTab = DASHBOARD_TABS.includes(requestedTab)
        ? requestedTab
        : "browse";

    function selectTab(tab) {
        setSearchParams(tab === "browse" ? {} : { tab });
    }

    const [statusFilter, setStatusFilter] = useState("ALL");

    const [publicQuery, setPublicQuery] = useState("");
    const [publicStats, setPublicStats] = useState(null);

    const [selectedPromiseId, setSelectedPromiseId] = useState("");
    const [trackingEvidence, setTrackingEvidence] = useState([]);
    const [trackingHistory, setTrackingHistory] = useState([]);
    const [trackingLoading, setTrackingLoading] = useState(false);
    const [trackingError, setTrackingError] = useState("");

    const [myEvidence, setMyEvidence] = useState([]);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [availablePromises, setAvailablePromises] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Submit evidence form state
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState("");
    const [submitSuccess, setSubmitSuccess] = useState("");
    const [evidenceForm, setEvidenceForm] = useState({
        promiseId: "",
        title: "",
        evidenceType: "GOVERNMENT_ORDER",
        description: "",
        sourceUrl: "",
        issuingDepartment: "",
        documentReference: "",
        documentDate: "",
        claimScope: "",
        file: null
    });

    // Derive the tracked promise from the id and the already-loaded public
    // list. The first published promise is used by default on the tracking tab.
    const selectedPromise = selectedPromiseId
        ? availablePromises.find(
              item => String(item.id) === String(selectedPromiseId)
          ) || null
        : activeTab === "tracking" && availablePromises.length > 0
        ? availablePromises[0]
        : null;

    const loadData = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const [evidenceData, notifData, promisesData, statsData] =
                await Promise.all([
                    getMyEvidence(),
                    getNotifications().catch(() => ({ items: [], unreadCount: 0 })),
                    getPromises().catch(() => []),
                    getPublicStats().catch(() => null)
                ]);

            setMyEvidence(evidenceData || []);
            setNotifications(notifData.items || []);
            setUnreadCount(notifData.unreadCount || 0);
            setAvailablePromises(promisesData || []);
            setPublicStats(statsData);
        } catch (err) {
            setError("Could not load your citizen dashboard: " + err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function refresh() {
            await loadData();
            if (cancelled) return;
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadData]);

    // Load the public evidence and reviewed history for the tracked promise.
    useEffect(() => {
        if (!selectedPromise) return;

        let cancelled = false;

        async function loadTracking() {
            try {
                setTrackingLoading(true);
                setTrackingError("");

                const [evidenceData, historyData] = await Promise.all([
                    getApprovedEvidenceForPromise(selectedPromise.id)
                        .catch(() => []),
                    getPromiseHistory(selectedPromise.id).catch(() => [])
                ]);

                if (!cancelled) {
                    setTrackingEvidence(
                        Array.isArray(evidenceData) ? evidenceData : []
                    );
                    setTrackingHistory(
                        Array.isArray(historyData) ? historyData : []
                    );
                }
            } catch (err) {
                if (!cancelled) {
                    setTrackingError(`Could not load tracking data: ${err.message}`);
                }
            } finally {
                if (!cancelled) setTrackingLoading(false);
            }
        }

        loadTracking();
        return () => { cancelled = true; };
    }, [selectedPromise]);

    // Download evidence file
    async function handleDownload(item) {
        try {
            setError("");
            await downloadEvidence(item.id, item.originalFileName);
        } catch (err) {
            setError("Download failed: " + err.message);
        }
    }

    // Resubmit rejected evidence
    async function handleResubmit(item) {
        if (!window.confirm("Resubmit this rejected document for a fresh review?")) {
            return;
        }

        try {
            setError("");
            setSuccess("");
            await resubmitEvidence(item.id);
            setSuccess(`"${item.title}" resubmitted for review.`);
            setMyEvidence(await getMyEvidence());
        } catch (err) {
            setError("Could not resubmit: " + err.message);
        }
    }

    // Mark single notification read
    async function handleNotificationClick(item) {
        if (item.read) return;
        try {
            await markNotificationRead(item.id);
            setNotifications(prev =>
                prev.map(n => (n.id === item.id ? { ...n, read: true } : n))
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch {
            // Non-critical
        }
    }

    // Mark all notifications read
    async function handleMarkAllNotificationsRead() {
        try {
            await markAllNotificationsRead();
            setNotifications(prev => prev.map(n => ({ ...n, read: true })));
            setUnreadCount(0);
        } catch (err) {
            setError("Failed to mark notifications read: " + err.message);
        }
    }

    // Handle form change
    function handleFormChange(event) {
        const { name, value } = event.target;
        setEvidenceForm(prev => ({
            ...prev,
            [name]: value
        }));
    }

    function handleFileChange(event) {
        const file = event.target.files?.[0] || null;
        setEvidenceForm(prev => ({
            ...prev,
            file
        }));
    }

    // Submit new evidence
    async function handleFormSubmit(event) {
        event.preventDefault();

        if (!evidenceForm.promiseId) {
            setSubmitError("Please select which public promise this document supports.");
            return;
        }
        if (!evidenceForm.title.trim()) {
            setSubmitError("Please provide a title for the evidence document.");
            return;
        }
        if (!evidenceForm.file) {
            setSubmitError("Please attach a document file (PDF, image, or doc).");
            return;
        }

        try {
            setSubmitting(true);
            setSubmitError("");
            setSubmitSuccess("");

            await uploadEvidence(evidenceForm);

            setSubmitSuccess("Evidence submitted successfully! It is now in the review queue.");
            setEvidenceForm({
                promiseId: "",
                title: "",
                evidenceType: "GOVERNMENT_ORDER",
                description: "",
                sourceUrl: "",
                issuingDepartment: "",
                documentReference: "",
                documentDate: "",
                claimScope: "",
                file: null
            });

            // Reload evidence & switch to submissions after a short moment
            const updated = await getMyEvidence();
            setMyEvidence(updated);
            setTimeout(() => {
                selectTab("submissions");
                setSuccess("Your new document was added to the review queue.");
            }, 1000);
        } catch (err) {
            setSubmitError(err.message || "Failed to submit evidence.");
        } finally {
            setSubmitting(false);
        }
    }

    // Calculate metrics
    const counts = myEvidence.reduce(
        (acc, item) => {
            const key = statusClass(item.status);
            acc[key] = (acc[key] || 0) + 1;
            return acc;
        },
        { approved: 0, pending: 0, rejected: 0 }
    );

    const filteredEvidence = myEvidence.filter(item => {
        if (statusFilter === "ALL") return true;
        return item.status === statusFilter;
    });

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading your citizen dashboard...</p>
            </section>
        );
    }

    return (
        <section className="dashboard-page">
            {/* Welcome Header */}
            <div className="page-heading">
                <div>
                    <p className="eyebrow">CITIZEN WORKSPACE</p>
                    <h2>Welcome back, {user?.name || "Citizen"}</h2>
                    <p>
                        Track your supporting evidence documents, follow independent review progress,
                        and submit new public records to hold commitments accountable.
                    </p>

                    <form
                        className="citizen-search"
                        onSubmit={event => {
                            event.preventDefault();
                            selectTab("browse");
                        }}
                    >
                        <input
                            type="search"
                            value={publicQuery}
                            placeholder="Search published promises by title or keyword"
                            aria-label="Search published promises"
                            onChange={event => setPublicQuery(event.target.value)}
                        />
                        <button type="submit" className="primary-button">
                            Search public record
                        </button>
                    </form>
                </div>

                <div className="dashboard-identity">
                    <span className="nav-user-name">{user?.name}</span>
                    <span className="role-chip">CITIZEN</span>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {/* Meaningful Summary Cards (Citizen specific) */}
            <section className="summary-grid dashboard-summary">
                <div className="summary-card">
                    <span>My submissions</span>
                    <strong>{myEvidence.length}</strong>
                </div>

                <div className="summary-card">
                    <span>Approved evidence</span>
                    <strong style={{ color: "#166534" }}>{counts.approved}</strong>
                </div>

                <div className="summary-card">
                    <span>Under review</span>
                    <strong style={{ color: "#b45309" }}>{counts.pending}</strong>
                </div>

                <div className="summary-card">
                    <span>Unread alerts</span>
                    <strong style={{ color: unreadCount > 0 ? "#b91c1c" : "#1746a2" }}>
                        {unreadCount}
                    </strong>
                </div>
            </section>

            {/* Public figures: published promises and approved evidence only. */}
            <section className="summary-grid dashboard-summary public-summary">
                <div className="summary-card">
                    <span>Published promises</span>
                    <strong>
                        {displayCount(publicStats?.totalPromises)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Approved evidence</span>
                    <strong style={{ color: "#166534" }}>
                        {displayCount(publicStats?.approvedEvidence)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Promises with proof</span>
                    <strong>
                        {displayCount(publicStats?.promisesWithApprovedEvidence)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>States covered</span>
                    <strong>
                        {displayCount(publicStats?.totalStates)}
                    </strong>
                </div>
            </section>

            {/* Main Action Banner */}
            <section className="dashboard-banner">
                <div>
                    <strong>Have a document that verifies or challenges a promise?</strong>
                    <p>
                        Attach official gazettes, government orders, budget reports or audit papers
                        to any public promise.
                    </p>
                </div>
                <button
                    type="button"
                    className="primary-button"
                    onClick={() => selectTab("submit")}
                >
                    + Submit evidence
                </button>
            </section>

            {/* Dashboard Layout: Sidebar + Main Content Area */}
            <div className="dashboard-layout">
                {/* Dedicated Sidebar Navigation */}
                <aside className="dashboard-sidebar">
                    <div className="dashboard-sidebar-title">Citizen Menu</div>
                    <ul className="dashboard-sidebar-nav">
                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "browse" ? "active" : ""}`}
                                onClick={() => selectTab("browse")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🏛️</span> Browse Promises
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "tracking" ? "active" : ""}`}
                                onClick={() => selectTab("tracking")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📈</span> Track Progress
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "account" ? "active" : ""}`}
                                onClick={() => selectTab("account")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>👤</span> My Account
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "submissions" ? "active" : ""}`}
                                onClick={() => selectTab("submissions")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📄</span> My Submissions
                                </span>
                                <span className="dashboard-nav-badge">{myEvidence.length}</span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "progress" ? "active" : ""}`}
                                onClick={() => selectTab("progress")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>⏱️</span> Review Progress
                                </span>
                                {counts.pending > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {counts.pending}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "notifications" ? "active" : ""}`}
                                onClick={() => selectTab("notifications")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🔔</span> Notifications
                                </span>
                                {unreadCount > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {unreadCount}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "submit" ? "active" : ""}`}
                                onClick={() => selectTab("submit")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📤</span> Submit Evidence
                                </span>
                            </button>
                        </li>
                    </ul>

                    <div className="dashboard-sidebar-divider" />

                    <div className="dashboard-sidebar-actions">
                        <Link to="/promises" className="dashboard-sidebar-link">
                            <span>🔍</span> Browse public promises
                        </Link>
                    </div>
                </aside>

                {/* Main Task Area */}
                <main className="dashboard-main-area">
                    {/* TAB: BROWSE PUBLISHED PROMISES */}
                    {activeTab === "browse" && (
                        <section className="panel">
                            <PublicPromiseList
                                query={publicQuery}
                                onQueryChange={setPublicQuery}
                            />
                        </section>
                    )}

                    {/* TAB: TRACK PROGRESS / REVIEWED EVIDENCE */}
                    {activeTab === "tracking" && (
                        <section className="panel tracking-panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">PUBLIC TRACKING</p>
                                    <h3>Promise tracking</h3>
                                </div>
                                {selectedPromise && (
                                    <Link
                                        to={`/promises/${selectedPromise.id}?tab=tracking`}
                                        className="secondary-button"
                                    >
                                        Full detail page
                                    </Link>
                                )}
                            </div>

                            <p className="panel-note">
                                Reviewed evidence and independently approved status
                                changes are shown below. Approving a document does
                                not by itself mark a promise complete.
                            </p>

                            {availablePromises.length === 0 ? (
                                <p className="empty-note">
                                    No published promises are available to track yet.
                                </p>
                            ) : (
                                <>
                                    <label className="tracking-promise-picker">
                                        <span>Choose a published promise</span>
                                        <select
                                            value={selectedPromise?.id || ""}
                                            onChange={event =>
                                                setSelectedPromiseId(
                                                    event.target.value
                                                )
                                            }
                                        >
                                            <option value="">
                                                Select a promise...
                                            </option>
                                            {availablePromises.map(item => (
                                                <option
                                                    key={item.id}
                                                    value={item.id}
                                                >
                                                    #{item.id} — {item.title}
                                                </option>
                                            ))}
                                        </select>
                                    </label>

                                    {selectedPromise && (
                                        <div className="tracking-selected-summary">
                                            <div>
                                                <strong>
                                                    {selectedPromise.title}
                                                </strong>
                                                <p className="public-note">
                                                    {statusDescription(
                                                        selectedPromise.status
                                                    )}
                                                </p>
                                            </div>
                                            <div className="public-card-badges">
                                                <StatusBadge
                                                    status={selectedPromise.status}
                                                />
                                                <VerificationBadge
                                                    verificationState={
                                                        selectedPromise.verificationState
                                                    }
                                                />
                                            </div>
                                        </div>
                                    )}

                                    {selectedPromise ? (
                                        <div className="tracking-public-sections">
                                            <ApprovedEvidenceList
                                                evidence={trackingEvidence}
                                                loading={trackingLoading}
                                                error={trackingError}
                                                compact
                                            />
                                            <PromiseTrackingTimeline
                                                promise={selectedPromise}
                                                history={trackingHistory}
                                                evidence={trackingEvidence}
                                                loading={trackingLoading}
                                                error={trackingError}
                                            />
                                        </div>
                                    ) : (
                                        <p className="empty-note">
                                            Select a promise to see its reviewed
                                            evidence and verified status history.
                                        </p>
                                    )}
                                </>
                            )}
                        </section>
                    )}

                    {/* TAB: MY ACCOUNT */}
                    {activeTab === "account" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">MY ACCOUNT</p>
                                    <h3>Your PromiseProof account</h3>
                                </div>
                                <span className="role-chip">
                                    {user?.role || "CITIZEN"}
                                </span>
                            </div>

                            <p className="panel-note">
                                Your account identity is private. Published promises
                                and approved evidence are shown without contributor
                                names or contact details.
                            </p>

                            <dl className="public-card-meta detailed account-details">
                                <div>
                                    <dt>Name</dt>
                                    <dd>{user?.name || "Citizen"}</dd>
                                </div>
                                <div>
                                    <dt>Email</dt>
                                    <dd>{user?.email || "—"}</dd>
                                </div>
                                <div>
                                    <dt>Role</dt>
                                    <dd>{user?.role || "CITIZEN"}</dd>
                                </div>
                                <div>
                                    <dt>My submissions</dt>
                                    <dd>{myEvidence.length}</dd>
                                </div>
                                <div>
                                    <dt>Approved evidence</dt>
                                    <dd>{counts.approved}</dd>
                                </div>
                                <div>
                                    <dt>Unread alerts</dt>
                                    <dd>{unreadCount}</dd>
                                </div>
                            </dl>

                            <div className="account-actions">
                                <Link
                                    to="/dashboard?tab=submissions"
                                    className="secondary-button"
                                >
                                    My evidence
                                </Link>
                                <Link
                                    to="/dashboard?tab=tracking"
                                    className="primary-button"
                                >
                                    Track promises
                                </Link>
                            </div>
                        </section>
                    )}

                    {/* TAB 1: SUBMIT EVIDENCE (MAIN ACTION) */}
                    {activeTab === "submit" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">CONTRIBUTE PROOF</p>
                                    <h3>Submit supporting evidence</h3>
                                </div>
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => selectTab("submissions")}
                                >
                                    Back to submissions
                                </button>
                            </div>

                            <p className="panel-note">
                                Select a public promise and attach official records, gazettes, budget allocations,
                                or verified audit reports. Your document will be reviewed independently before
                                appearing on the public record.
                            </p>

                            {submitError && <p className="error-message">{submitError}</p>}
                            {submitSuccess && <p className="success-message">{submitSuccess}</p>}

                            <form className="promise-form" onSubmit={handleFormSubmit}>
                                <div className="form-group">
                                    <label htmlFor="promiseId">Select promise *</label>
                                    <select
                                        id="promiseId"
                                        name="promiseId"
                                        value={evidenceForm.promiseId}
                                        onChange={handleFormChange}
                                        required
                                    >
                                        <option value="">-- Choose a promise to support --</option>
                                        {availablePromises.map(p => (
                                            <option key={p.id} value={p.id}>
                                                #{p.id} - {p.title} ({p.partyName || "Party"})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label htmlFor="title">Document title *</label>
                                        <input
                                            id="title"
                                            name="title"
                                            type="text"
                                            value={evidenceForm.title}
                                            onChange={handleFormChange}
                                            placeholder="e.g. Ministry Gazette Notification #402"
                                            required
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="evidenceType">Evidence type *</label>
                                        <select
                                            id="evidenceType"
                                            name="evidenceType"
                                            value={evidenceForm.evidenceType}
                                            onChange={handleFormChange}
                                        >
                                            <option value="GOVERNMENT_ORDER">Government Order</option>
                                            <option value="OFFICIAL_REPORT">Official Report</option>
                                            <option value="BUDGET_DOCUMENT">Budget Document</option>
                                            <option value="AUDIT_REPORT">Audit Report</option>
                                            <option value="PARLIAMENTARY_RECORD">Parliamentary Record</option>
                                            <option value="OTHER">Other Official Source</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label htmlFor="issuingDepartment">Issuing department</label>
                                        <input
                                            id="issuingDepartment"
                                            name="issuingDepartment"
                                            type="text"
                                            value={evidenceForm.issuingDepartment}
                                            onChange={handleFormChange}
                                            placeholder="e.g. Ministry of Finance"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="documentReference">Document reference / number</label>
                                        <input
                                            id="documentReference"
                                            name="documentReference"
                                            type="text"
                                            value={evidenceForm.documentReference}
                                            onChange={handleFormChange}
                                            placeholder="e.g. F.No. 12/2026-P"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="documentDate">Document date</label>
                                        <input
                                            id="documentDate"
                                            name="documentDate"
                                            type="date"
                                            value={evidenceForm.documentDate}
                                            onChange={handleFormChange}
                                        />
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label htmlFor="sourceUrl">Source link / Public URL</label>
                                    <input
                                        id="sourceUrl"
                                        name="sourceUrl"
                                        type="url"
                                        value={evidenceForm.sourceUrl}
                                        onChange={handleFormChange}
                                        placeholder="https://official-portal.gov/records/..."
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="description">Description & context</label>
                                    <textarea
                                        id="description"
                                        name="description"
                                        rows="3"
                                        value={evidenceForm.description}
                                        onChange={handleFormChange}
                                        placeholder="Explain what this document shows and how it relates to the commitment..."
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="claimScope">Claim scope</label>
                                    <input
                                        id="claimScope"
                                        name="claimScope"
                                        type="text"
                                        value={evidenceForm.claimScope}
                                        onChange={handleFormChange}
                                        placeholder="e.g. Sanctioned funds for Phase 1 construction"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="file">Attach document file *</label>
                                    <input
                                        id="file"
                                        name="file"
                                        type="file"
                                        onChange={handleFileChange}
                                        required
                                    />
                                    <small style={{ color: "#64748b", marginTop: "4px" }}>
                                        Accepts PDF, PNG, JPG, or DOC documents.
                                    </small>
                                </div>

                                <div className="form-actions">
                                    <button
                                        type="submit"
                                        className="primary-button"
                                        disabled={submitting}
                                    >
                                        {submitting ? "Uploading evidence..." : "Submit for review"}
                                    </button>
                                </div>
                            </form>
                        </section>
                    )}

                    {/* TAB 2: MY SUBMISSIONS */}
                    {activeTab === "submissions" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">MY SUBMISSIONS</p>
                                    <h3>Evidence I submitted</h3>
                                </div>
                                <span className="panel-tag">
                                    {filteredEvidence.length}{" "}
                                    {filteredEvidence.length === 1 ? "document" : "documents"}
                                </span>
                            </div>

                            <p className="panel-note">
                                Documents you upload are verified by reviewers before becoming public.
                                If a document is rejected, review comments explain what is missing so you can resubmit.
                            </p>

                            {/* Status Filter Bar */}
                            <div className="dashboard-filter-bar">
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "ALL" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("ALL")}
                                >
                                    All ({myEvidence.length})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "PENDING" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("PENDING")}
                                >
                                    Pending ({counts.pending})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "APPROVED" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("APPROVED")}
                                >
                                    Approved ({counts.approved})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "REJECTED" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("REJECTED")}
                                >
                                    Rejected ({counts.rejected})
                                </button>
                            </div>

                            {filteredEvidence.length === 0 ? (
                                <p className="empty-note">
                                    {myEvidence.length === 0
                                        ? "You have not submitted any evidence yet. Click 'Submit evidence' to get started."
                                        : `No submissions with status ${statusFilter}.`}
                                </p>
                            ) : (
                                <div className="table-wrap">
                                    <table className="admin-table">
                                        <thead>
                                            <tr>
                                                <th>Document</th>
                                                <th>Promise</th>
                                                <th>Status</th>
                                                <th>Submitted</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filteredEvidence.map(item => (
                                                <tr key={item.id}>
                                                    <td>
                                                        <strong>{item.title}</strong>
                                                        <small>{item.evidenceType?.replaceAll("_", " ")}</small>
                                                        {item.originalFileName && (
                                                            <small style={{ color: "#94a3b8" }}>
                                                                File: {item.originalFileName}
                                                            </small>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <strong>{item.promiseTitle || `Promise #${item.promiseId}`}</strong>
                                                    </td>
                                                    <td>
                                                        <span className={`status-chip ${statusClass(item.status)}`}>
                                                            {item.status}
                                                        </span>
                                                        {item.reviewComment && (
                                                            <small style={{ color: "#991b1b" }}>
                                                                Note: {item.reviewComment}
                                                            </small>
                                                        )}
                                                    </td>
                                                    <td>{formatDateTime(item.uploadedAt)}</td>
                                                    <td>
                                                        <div style={{ display: "flex", gap: "6px" }}>
                                                            <button
                                                                type="button"
                                                                className="secondary-button"
                                                                onClick={() => handleDownload(item)}
                                                            >
                                                                Download
                                                            </button>

                                                            {item.status === "REJECTED" && (
                                                                <button
                                                                    type="button"
                                                                    className="primary-button"
                                                                    onClick={() => handleResubmit(item)}
                                                                >
                                                                    Resubmit
                                                                </button>
                                                            )}
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 3: REVIEW PROGRESS */}
                    {activeTab === "progress" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">QUALITY ASSURANCE</p>
                                    <h3>Submission review progress</h3>
                                </div>
                                <span className="panel-tag">
                                    {myEvidence.length} items tracked
                                </span>
                            </div>

                            <p className="panel-note">
                                Reviewers evaluate evidence across three criteria: document authenticity,
                                source traceability, and claim relevance. Track the stage of each submitted document below.
                            </p>

                            {myEvidence.length === 0 ? (
                                <p className="empty-note">
                                    No submissions to track yet. Once you submit evidence, its step-by-step review
                                    progress will appear here.
                                </p>
                            ) : (
                                <div>
                                    {myEvidence.map(item => {
                                        const isApproved = item.status === "APPROVED";
                                        const isRejected = item.status === "REJECTED";
                                        const isPending = item.status === "PENDING";

                                        return (
                                            <div className="progress-card" key={item.id}>
                                                <div className="progress-header">
                                                    <div>
                                                        <h4>{item.title}</h4>
                                                        <p>
                                                            Supporting: <strong>{item.promiseTitle || `Promise #${item.promiseId}`}</strong>
                                                        </p>
                                                    </div>
                                                    <span className={`status-chip ${statusClass(item.status)}`}>
                                                        {item.status}
                                                    </span>
                                                </div>

                                                {/* Visual Review Timeline */}
                                                <div className="progress-timeline">
                                                    <div className="timeline-step completed">
                                                        <div className="timeline-dot">✓</div>
                                                        <span className="timeline-label">Submitted</span>
                                                    </div>

                                                    <div className={`timeline-connector ${!isPending ? "completed" : ""}`} />

                                                    <div className={`timeline-step ${isPending ? "active" : "completed"}`}>
                                                        <div className="timeline-dot">{isPending ? "●" : "✓"}</div>
                                                        <span className="timeline-label">Under Review</span>
                                                    </div>

                                                    <div className={`timeline-connector ${isApproved ? "completed" : ""}`} />

                                                    <div
                                                        className={`timeline-step ${
                                                            isApproved
                                                                ? "completed"
                                                                : isRejected
                                                                ? "rejected"
                                                                : ""
                                                        }`}
                                                    >
                                                        <div className="timeline-dot">
                                                            {isApproved ? "✓" : isRejected ? "✕" : "○"}
                                                        </div>
                                                        <span className="timeline-label">
                                                            {isApproved ? "Approved" : isRejected ? "Rejected" : "Decision"}
                                                        </span>
                                                    </div>
                                                </div>

                                                {/* Reviewer Feedback / Comments */}
                                                {item.reviewComment && (
                                                    <div className="publication-comment">
                                                        <strong>Reviewer note:</strong> {item.reviewComment}
                                                    </div>
                                                )}

                                                {/* Recorded review dimensions */}
                                                {(item.documentReviewStatus ||
                                                    item.sourceReviewStatus ||
                                                    item.claimReviewStatus) && (

                                                    <div className="progress-review-details">
                                                        <div>
                                                            <strong>Document review:</strong>{" "}
                                                            {item.documentReviewStatus || "PENDING"}
                                                        </div>

                                                        <div>
                                                            <strong>Source review:</strong>{" "}
                                                            {item.sourceReviewStatus || "PENDING"}
                                                        </div>

                                                        <div>
                                                            <strong>Claim review:</strong>{" "}
                                                            {item.claimReviewStatus || "PENDING"}
                                                        </div>

                                                        {item.verificationNotes && (
                                                            <div>
                                                                <strong>Verification notes:</strong>{" "}
                                                                {item.verificationNotes}
                                                            </div>
                                                        )}
                                                    </div>
                                                )}

                                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "12px" }}>
                                                    <small style={{ color: "#64748b" }}>
                                                        Submitted on {formatDateTime(item.uploadedAt)}
                                                    </small>

                                                    <div style={{ display: "flex", gap: "8px" }}>
                                                        <button
                                                            type="button"
                                                            className="secondary-button"
                                                            onClick={() => handleDownload(item)}
                                                        >
                                                            Download
                                                        </button>
                                                        {isRejected && (
                                                            <button
                                                                type="button"
                                                                className="primary-button"
                                                                onClick={() => handleResubmit(item)}
                                                            >
                                                                Resubmit
                                                            </button>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 4: NOTIFICATIONS */}
                    {activeTab === "notifications" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">UPDATES & ALERTS</p>
                                    <h3>System notifications</h3>
                                </div>
                                {notifications.length > 0 && (
                                    <button
                                        type="button"
                                        className="secondary-button"
                                        onClick={handleMarkAllNotificationsRead}
                                        disabled={unreadCount === 0}
                                    >
                                        Mark all as read
                                    </button>
                                )}
                            </div>

                            <p className="panel-note">
                                Real-time notifications about reviewer decisions, publication approvals,
                                or comments on your submitted evidence.
                            </p>

                            {notifications.length === 0 ? (
                                <p className="empty-note">
                                    You have no notifications right now.
                                </p>
                            ) : (
                                <ul className="notification-list" style={{ maxHeight: "none", border: "1px solid #edf0f5", borderRadius: "10px" }}>
                                    {notifications.map(item => (
                                        <li key={item.id}>
                                            <button
                                                type="button"
                                                className={`notification-item ${!item.read ? "unread" : ""}`}
                                                onClick={() => handleNotificationClick(item)}
                                            >
                                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                                    <span className="notification-title">{item.title}</span>
                                                    <span className="notification-time">{formatRelative(item.createdAt)}</span>
                                                </div>
                                                <span className="notification-message">{item.message}</span>
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    )}
                </main>
            </div>
        </section>
    );
}

export default CitizenDashboard;

```

### `promiseproof-backend\src\main\resources\application.properties`

```properties
spring.application.name=promiseproof-backend

# Load local development values from .env when present. Real environment
# variables still take precedence, and production can omit this file entirely.
spring.config.import=optional:file:.env[.properties]

server.port=${SERVER_PORT:8080}

# Session cookie hardening. Set PROMISEPROOF_COOKIE_SECURE=true behind HTTPS.
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.secure=${PROMISEPROOF_COOKIE_SECURE:false}

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# Never expose stack traces or internals in API errors
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never

# Evidence file upload configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=12MB

# Store uploaded files outside the application source folders
promiseproof.upload-dir=${PROMISEPROOF_UPLOAD_DIR:V:/PromiseProof/evidence-storage}

# Comma-separated list of browser origins allowed to call the API
promiseproof.cors.allowed-origins=${PROMISEPROOF_CORS_ORIGINS:http://localhost:5173}

# Initial administrator account (created on first startup only).
# No default password is allowed: the backend requires a strong initial
# password through PROMISEPROOF_ADMIN_PASSWORD when no admin exists yet.
promiseproof.admin.name=${PROMISEPROOF_ADMIN_NAME:Platform Administrator}
promiseproof.admin.email=${PROMISEPROOF_ADMIN_EMAIL:admin@promiseproof.local}
promiseproof.admin.password=${PROMISEPROOF_ADMIN_PASSWORD:}
```

### `promiseproof-backend\.env.example`

```properties
# Copy to .env for local development. Never commit real credentials.
DB_URL=jdbc:mysql://localhost:3306/promiseproof
DB_USER=your_mysql_user
DB_PASSWORD=your_mysql_password

# Optional local overrides
# SERVER_PORT=8080
# PROMISEPROOF_ADMIN_NAME=Platform Administrator
# PROMISEPROOF_ADMIN_EMAIL=admin@example.com
# PROMISEPROOF_ADMIN_PASSWORD=change-me-before-first-start
# PROMISEPROOF_UPLOAD_DIR=V:/PromiseProof/evidence-storage
# PROMISEPROOF_CORS_ORIGINS=http://localhost:5173
```

### `promiseproof-backend\.gitignore`

```text
HELP.md
target/
.env
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
```
