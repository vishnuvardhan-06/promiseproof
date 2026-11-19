package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.PromiseStatusDecisionRequest;
import promiseproof_backend.dto.PromiseStatusProposalRequest;
import promiseproof_backend.dto.PromiseStatusProposalResponse;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.PromiseStatusChange;
import promiseproof_backend.entity.PromiseStatusProposal;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.PromiseStatusChangeRepository;
import promiseproof_backend.repository.PromiseStatusProposalRepository;

@ExtendWith(MockitoExtension.class)
class PromiseStatusProposalServiceTest {

    @Mock
    private PromiseRepository promiseRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private PromiseStatusProposalRepository proposalRepository;

    @Mock
    private PromiseStatusChangeRepository changeRepository;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PromiseStatusProposalService proposalService;

    @Test
    void submitRejectsProposalWithoutApprovedEvidence() {

        PromiseStatusProposalRequest request = new PromiseStatusProposalRequest();
        request.setPromiseId(1L);
        request.setProposedStatus("COMPLETED");
        request.setEvidenceId(10L);

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("PENDING");

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(proposalRepository
                .findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(1L, "PENDING"))
                .thenReturn(Optional.empty());

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.submit(request)
        );

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void submitRejectsNotAssessedAsProposedStatus() {

        PromiseStatusProposalRequest request = new PromiseStatusProposalRequest();
        request.setPromiseId(1L);
        request.setProposedStatus("NOT_ASSESSED");
        request.setEvidenceId(10L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.submit(request)
        );

        assertEquals(400, exception.getStatusCode().value());

        verify(proposalRepository, never())
                .save(any(PromiseStatusProposal.class));
    }

    @Test
    void submitMarksUnverifiedPromiseAsUnderReview() {

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("NOT_ASSESSED");
        promise.setVerificationState("UNREVIEWED");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("APPROVED");

        PromiseStatusProposalRequest request = new PromiseStatusProposalRequest();
        request.setPromiseId(1L);
        request.setProposedStatus("COMPLETED");
        request.setEvidenceId(10L);
        request.setReason("Completion certificate published");

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(proposalRepository
                .findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(1L, "PENDING"))
                .thenReturn(Optional.empty());

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(proposalRepository.save(any(PromiseStatusProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        proposalService.submit(request);

        assertEquals("UNDER_REVIEW", promise.getVerificationState());
        // Submitting a proposal must not change the public status.
        assertEquals("NOT_ASSESSED", promise.getStatus());
    }

    @Test
    void submitDisputesPreviouslyVerifiedPromise() {

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("COMPLETED");
        promise.setVerificationState("STATUS_VERIFIED");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("APPROVED");

        PromiseStatusProposalRequest request = new PromiseStatusProposalRequest();
        request.setPromiseId(1L);
        request.setProposedStatus("IN_PROGRESS");
        request.setEvidenceId(10L);

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(proposalRepository
                .findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(1L, "PENDING"))
                .thenReturn(Optional.empty());

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(proposalRepository.save(any(PromiseStatusProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        proposalService.submit(request);

        assertEquals("DISPUTED", promise.getVerificationState());
    }

    @Test
    void decideApprovesProposalAndVerifiesStatus() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("researcher@example.com");
        proposal.setProposedByName("Dr. Researcher");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("APPROVED");
        evidence.setClaimReviewStatus("SUPPORTED");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");
        decision.setComment("Completion certificate verified");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(proposalRepository.save(any(PromiseStatusProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(currentUser.email()).thenReturn("reviewer@example.com");
        when(currentUser.displayName()).thenReturn("Reviewer");
        when(currentUser.role()).thenReturn("REVIEWER");

        when(changeRepository.save(any(PromiseStatusChange.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseStatusProposalResponse response =
                proposalService.decide(3L, decision);

        assertEquals("APPROVED", response.getStatus());
        assertEquals("COMPLETED", promise.getStatus());
        assertEquals("STATUS_VERIFIED", promise.getVerificationState());
        verify(changeRepository).save(any(PromiseStatusChange.class));
    }

    @Test
    void decideRejectionFallsBackToEvidenceReviewed() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");
        promise.setVerificationState("UNDER_REVIEW");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("REJECTED");
        decision.setComment("Evidence does not show completion");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(proposalRepository.save(any(PromiseStatusProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.countByPromise_IdAndStatus(1L, "APPROVED"))
                .thenReturn(1L);

        proposalService.decide(3L, decision);

        assertEquals("IN_PROGRESS", promise.getStatus());
        assertEquals("EVIDENCE_REVIEWED", promise.getVerificationState());

        verify(changeRepository, never()).save(any(PromiseStatusChange.class));
    }

    @Test
    void decideRejectionWithoutApprovedEvidenceResetToUnreviewed() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");
        promise.setVerificationState("UNDER_REVIEW");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("REJECTED");
        decision.setComment("Evidence is not convincing");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(proposalRepository.save(any(PromiseStatusProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.countByPromise_IdAndStatus(1L, "APPROVED"))
                .thenReturn(0L);

        proposalService.decide(3L, decision);

        assertEquals("UNREVIEWED", promise.getVerificationState());
    }

    @Test
    void decideRejectsProposalWithoutComment() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setStatus("PENDING");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("REJECTED");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void pendingProposalsAreListedForReviewers() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setStatus("PENDING");
        proposal.setProposedByName("Dr. Researcher");

        when(proposalRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                .thenReturn(List.of(proposal));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(new Promise()));

        List<PromiseStatusProposalResponse> pending =
                proposalService.getPending();

        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).getStatus());
    }

    @Test
    void decideRevalidatesEvidenceThatWasRejectedAfterSubmission() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("researcher@example.com");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("REJECTED");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(currentUser.email()).thenReturn("reviewer@example.com");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");
        decision.setComment("Looks complete");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("PENDING", proposal.getStatus());
        assertEquals("IN_PROGRESS", promise.getStatus());

        verify(proposalRepository, never())
                .save(any(PromiseStatusProposal.class));
        verify(changeRepository, never()).save(any(PromiseStatusChange.class));
    }

    @Test
    void decideRejectsProposalWhoseEvidenceDisappeared() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("researcher@example.com");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.empty());

        when(currentUser.email()).thenReturn("reviewer@example.com");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("PENDING", proposal.getStatus());
    }

    @Test
    void decideBlocksProposerFromApprovingOwnProposal() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("admin@example.com");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(currentUser.email()).thenReturn("admin@example.com");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("PENDING", proposal.getStatus());

        verify(promiseRepository, never()).findById(any());
    }

    @Test
    void decideBlocksUploaderFromDecidingProposal() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("researcher@example.com");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("APPROVED");
        evidence.setUploadedByEmail("reviewer@example.com");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(currentUser.email()).thenReturn("reviewer@example.com");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("PENDING", proposal.getStatus());
    }

    @Test
    void decideRejectsEvidenceThatFailsTheClaimReview() {

        PromiseStatusProposal proposal = new PromiseStatusProposal();
        proposal.setId(3L);
        proposal.setPromiseId(1L);
        proposal.setProposedStatus("COMPLETED");
        proposal.setEvidenceId(10L);
        proposal.setStatus("PENDING");
        proposal.setProposedByEmail("researcher@example.com");

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");

        Evidence evidence = new Evidence();
        evidence.setPromise(promise);
        evidence.setStatus("APPROVED");
        evidence.setClaimReviewStatus("NOT_SUPPORTED");

        when(proposalRepository.findById(3L))
                .thenReturn(Optional.of(proposal));

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        when(currentUser.email()).thenReturn("reviewer@example.com");

        PromiseStatusDecisionRequest decision = new PromiseStatusDecisionRequest();
        decision.setDecision("APPROVED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.decide(3L, decision)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("PENDING", proposal.getStatus());
    }

    @Test
    void submitRejectsEvidenceFromAnotherPromise() {

        PromiseStatusProposalRequest request = new PromiseStatusProposalRequest();
        request.setPromiseId(1L);
        request.setProposedStatus("COMPLETED");
        request.setEvidenceId(10L);

        Promise promise = new Promise();
        promise.setId(1L);
        promise.setTitle("Build 100 hospitals");

        Promise otherPromise = new Promise();
        otherPromise.setId(2L);

        Evidence evidence = new Evidence();
        evidence.setPromise(otherPromise);
        evidence.setStatus("APPROVED");

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(proposalRepository
                .findFirstByPromiseIdAndStatusOrderByCreatedAtDesc(1L, "PENDING"))
                .thenReturn(Optional.empty());

        when(evidenceRepository.findById(10L))
                .thenReturn(Optional.of(evidence));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> proposalService.submit(request)
        );

        assertEquals(400, exception.getStatusCode().value());
    }
}
