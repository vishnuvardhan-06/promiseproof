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
