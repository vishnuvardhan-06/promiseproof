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
