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
