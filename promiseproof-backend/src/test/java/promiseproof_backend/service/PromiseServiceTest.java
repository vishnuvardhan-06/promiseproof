package promiseproof_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.PromiseRequest;
import promiseproof_backend.dto.PromiseResponse;
import promiseproof_backend.dto.PromiseStatusCorrectionRequest;
import promiseproof_backend.dto.PublicationDecisionRequest;
import promiseproof_backend.dto.PublicPromiseResponse;
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
import promiseproof_backend.service.PromiseService;

@ExtendWith(MockitoExtension.class)
class PromiseServiceTest {

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

    @InjectMocks
    private PromiseService promiseService;

    @Test
    void createPromiseRejectsDirectImplementationStatus() {

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build 100 hospitals");
        request.setStatus("COMPLETED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.createPromise(request)
        );

        assertEquals(400, exception.getStatusCode().value());

        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    void createPromiseDefaultsToNotAssessed() {

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build 100 hospitals");

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseResponse response = promiseService.createPromise(request);

        assertEquals("NOT_ASSESSED", response.getStatus());
        assertEquals("UNREVIEWED", response.getVerificationState());
    }

    @Test
    void createPromiseAllowsNeutralInitialStatus() {

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build 100 hospitals");
        request.setStatus("not_assessed");

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseResponse response = promiseService.createPromise(request);

        assertEquals("NOT_ASSESSED", response.getStatus());
        assertEquals("UNREVIEWED", response.getVerificationState());
    }

    @Test
    void updatePromiseRejectsDirectStatusChange() {

        Promise existing = new Promise();
        existing.setId(3L);
        existing.setTitle("Build 100 hospitals");
        existing.setStatus("NOT_STARTED");
        existing.setCreatedByEmail("researcher@example.com");

        when(promiseRepository.findById(3L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build 100 hospitals");
        request.setStatus("COMPLETED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.updatePromise(3L, request)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("NOT_STARTED", existing.getStatus());

        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    void updatePromiseKeepsStatusWhenNotProvided() {

        Promise existing = new Promise();
        existing.setId(3L);
        existing.setTitle("Build 100 hospitals");
        existing.setStatus("IN_PROGRESS");
        existing.setCreatedByEmail("researcher@example.com");

        when(promiseRepository.findById(3L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Renovate 100 hospitals");

        PromiseResponse response = promiseService.updatePromise(3L, request);

        assertEquals("IN_PROGRESS", response.getStatus());
        assertEquals("Renovate 100 hospitals", response.getTitle());
    }

    @Test
    void updatePromiseAllowsEchoingCurrentStatus() {

        Promise existing = new Promise();
        existing.setId(3L);
        existing.setTitle("Build 100 hospitals");
        existing.setStatus("IN_PROGRESS");
        existing.setCreatedByEmail("researcher@example.com");

        when(promiseRepository.findById(3L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build 100 hospitals");
        request.setStatus("IN_PROGRESS");

        PromiseResponse response = promiseService.updatePromise(3L, request);

        assertEquals("IN_PROGRESS", response.getStatus());
    }

    @Test
    void correctStatusRequiresReason() {

        Promise promise = new Promise();
        promise.setId(9L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("NOT_STARTED");

        when(promiseRepository.findById(9L))
                .thenReturn(Optional.of(promise));

        PromiseStatusCorrectionRequest request =
                new PromiseStatusCorrectionRequest();
        request.setStatus("COMPLETED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.correctStatus(9L, request)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("NOT_STARTED", promise.getStatus());

        verify(promiseRepository, never()).save(any(Promise.class));
        verify(statusChangeRepository, never())
                .save(any(PromiseStatusChange.class));
    }

    @Test
    void correctStatusRejectsUnchangedStatus() {

        Promise promise = new Promise();
        promise.setId(9L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("COMPLETED");

        when(promiseRepository.findById(9L))
                .thenReturn(Optional.of(promise));

        PromiseStatusCorrectionRequest request =
                new PromiseStatusCorrectionRequest();
        request.setStatus("COMPLETED");
        request.setReason("No-op correction");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.correctStatus(9L, request)
        );

        assertEquals(400, exception.getStatusCode().value());

        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    void correctStatusRecordsHistoryAuditAndResetsVerification() {

        Promise promise = new Promise();
        promise.setId(9L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("NOT_STARTED");
        promise.setVerificationState("STATUS_VERIFIED");

        when(promiseRepository.findById(9L))
                .thenReturn(Optional.of(promise));

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(currentUser.email()).thenReturn("admin@example.com");
        when(currentUser.displayName()).thenReturn("Admin User");
        when(currentUser.role()).thenReturn("ADMIN");

        PromiseStatusCorrectionRequest request =
                new PromiseStatusCorrectionRequest();
        request.setStatus("completed");
        request.setReason("Audited court ruling confirms completion");

        PromiseResponse response = promiseService.correctStatus(9L, request);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("UNREVIEWED", response.getVerificationState());
        assertEquals("UNREVIEWED", promise.getVerificationState());

        ArgumentCaptor<PromiseStatusChange> changeCaptor =
                ArgumentCaptor.forClass(PromiseStatusChange.class);

        verify(statusChangeRepository).save(changeCaptor.capture());

        PromiseStatusChange change = changeCaptor.getValue();

        assertEquals(9L, change.getPromiseId());
        assertEquals("NOT_STARTED", change.getFromStatus());
        assertEquals("COMPLETED", change.getToStatus());
        assertEquals("admin@example.com", change.getChangedByEmail());
        assertEquals(
                "Audited court ruling confirms completion",
                change.getReason()
        );

        verify(auditService).record(
                eq("PROMISE_STATUS_ADMIN_CORRECTION"),
                eq("PROMISE"),
                eq(9L),
                eq("Build 100 hospitals"),
                eq("admin@example.com"),
                eq("ADMIN"),
                any(String.class)
        );
    }

    @Test
    void deletePromiseIsBlockedWhenEvidenceExists() {

        Promise promise = new Promise();
        promise.setTitle("Build 100 hospitals");
        promise.setCreatedByEmail("researcher@example.com");
        promise.setPublicationStatus(Promise.DRAFT);

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(evidenceRepository.countByPromise_Id(1L))
                .thenReturn(2L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.deletePromise(1L)
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(promiseRepository, never()).delete(any(Promise.class));
    }

    @Test
    void deletePromiseWorksWhenNoEvidenceExists() {

        Promise promise = new Promise();
        promise.setTitle("Build 100 hospitals");
        promise.setCreatedByEmail("researcher@example.com");
        promise.setPublicationStatus(Promise.DRAFT);

        when(promiseRepository.findById(1L))
                .thenReturn(Optional.of(promise));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(evidenceRepository.countByPromise_Id(1L))
                .thenReturn(0L);

        promiseService.deletePromise(1L);

        verify(promiseRepository).delete(promise);
    }

    @Test
    void getPromiseByIdReturnsPublicProjectionWithoutCreator() {

        Promise promise = new Promise();
        promise.setId(5L);
        promise.setTitle("Build 100 hospitals");
        promise.setStatus("IN_PROGRESS");
        promise.setCreatedByEmail("researcher@example.com");
        promise.setCreatedByName("Dr. Researcher");
        promise.setPublicationStatus(Promise.PUBLISHED);

        when(promiseRepository.findById(5L))
                .thenReturn(Optional.of(promise));

        when(evidenceRepository.countByPromise_IdAndStatus(5L, "APPROVED"))
                .thenReturn(1L);

        PublicPromiseResponse response = promiseService.getPromiseById(5L);

        assertEquals("Build 100 hospitals", response.title());
        assertEquals("IN_PROGRESS", response.status());
        assertEquals("UNREVIEWED", response.verificationState());
        assertTrue(response.hasApprovedEvidence());
    }

    @Test
    void myPromisesRetainCreatorAttributionForOwnerView() {

        Promise promise = new Promise();
        promise.setId(7L);
        promise.setTitle("Build 1 school");
        promise.setCreatedByEmail("researcher@example.com");
        promise.setCreatedByName("Dr. Researcher");

        when(promiseRepository
                .findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
                        "researcher@example.com"
                ))
                .thenReturn(List.of(promise));

        List<PromiseResponse> responses =
                promiseService.getMyPromises("researcher@example.com");

        assertEquals(1, responses.size());
        assertEquals("Build 1 school", responses.get(0).getTitle());
        assertEquals("researcher@example.com", responses.get(0).getCreatedByEmail());
    }

    @Test
    void researcherCannotUpdateAnotherResearchersPromise() {

        Promise existing = new Promise();
        existing.setId(3L);
        existing.setTitle("Build 100 hospitals");
        existing.setStatus("NOT_STARTED");
        existing.setCreatedByEmail("owner@example.com");

        when(promiseRepository.findById(3L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("intruder@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Hijacked wording");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.updatePromise(3L, request)
        );

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("Build 100 hospitals", existing.getTitle());

        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    void researcherCannotDeleteAnotherResearchersPromise() {

        Promise existing = new Promise();
        existing.setId(4L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("owner@example.com");
        existing.setPublicationStatus(Promise.DRAFT);

        when(promiseRepository.findById(4L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("intruder@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.deletePromise(4L)
        );

        assertEquals(403, exception.getStatusCode().value());

        verify(promiseRepository, never()).delete(any(Promise.class));
    }

    @Test
    void researcherCannotDeletePublishedPromise() {

        Promise existing = new Promise();
        existing.setId(5L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.PUBLISHED);

        when(promiseRepository.findById(5L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.deletePromise(5L)
        );

        assertEquals(403, exception.getStatusCode().value());

        verify(promiseRepository, never()).delete(any(Promise.class));
    }

    @Test
    void submitForPublicationRequiresOriginalSource() {

        Promise existing = new Promise();
        existing.setId(6L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.DRAFT);

        when(promiseRepository.findById(6L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(sourceRepository.countByPromise_Id(6L)).thenReturn(0L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.submitForPublication(6L)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals(Promise.DRAFT, existing.getPublicationStatus());

        verify(promiseRepository, never()).save(any(Promise.class));
    }

    @Test
    void submitForPublicationMovesDraftToSubmitted() {

        Promise existing = new Promise();
        existing.setId(6L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.DRAFT);

        when(promiseRepository.findById(6L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(sourceRepository.countByPromise_Id(6L)).thenReturn(1L);

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseResponse response = promiseService.submitForPublication(6L);

        assertEquals("SUBMITTED", response.getPublicationStatus());
        assertEquals("SUBMITTED", existing.getPublicationStatus());
    }

    @Test
    void creatorCannotReviewTheirOwnPromiseForPublication() {

        Promise existing = new Promise();
        existing.setId(7L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.SUBMITTED);

        when(promiseRepository.findById(7L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");

        PublicationDecisionRequest request =
                new PublicationDecisionRequest();
        request.setDecision("REVIEWED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.decidePublication(7L, request)
        );

        assertEquals(403, exception.getStatusCode().value());
        assertEquals(Promise.SUBMITTED, existing.getPublicationStatus());
    }

    @Test
    void publicationRequiresPriorReview() {

        Promise existing = new Promise();
        existing.setId(8L);
        existing.setTitle("Build a bridge");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.SUBMITTED);

        when(promiseRepository.findById(8L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("reviewer@example.com");

        PublicationDecisionRequest request =
                new PublicationDecisionRequest();
        request.setDecision("PUBLISHED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> promiseService.decidePublication(8L, request)
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(Promise.SUBMITTED, existing.getPublicationStatus());
    }

    @Test
    void publicListingExcludesUnpublishedPromises() {

        Promise draft = new Promise();
        draft.setId(1L);
        draft.setTitle("Draft promise");
        draft.setPublicationStatus(Promise.DRAFT);

        Promise published = new Promise();
        published.setId(2L);
        published.setTitle("Published promise");
        published.setPublicationStatus(Promise.PUBLISHED);

        when(promiseRepository.findAll())
                .thenReturn(List.of(draft, published));

        when(evidenceRepository.countByPromise_IdAndStatus(any(), eq("APPROVED")))
                .thenReturn(0L);

        List<PublicPromiseResponse> responses =
                promiseService.getAllPromises();

        assertEquals(1, responses.size());
        assertEquals("Published promise", responses.get(0).title());
    }

    @Test
    void editingPublishedPromiseReturnsItToDraft() {

        Promise existing = new Promise();
        existing.setId(9L);
        existing.setTitle("Build a bridge");
        existing.setStatus("NOT_ASSESSED");
        existing.setCreatedByEmail("researcher@example.com");
        existing.setPublicationStatus(Promise.PUBLISHED);

        when(promiseRepository.findById(9L))
                .thenReturn(Optional.of(existing));

        when(currentUser.email()).thenReturn("researcher@example.com");
        when(currentUser.role()).thenReturn("RESEARCHER");

        when(promiseRepository.save(any(Promise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromiseRequest request = new PromiseRequest();
        request.setTitle("Build a bigger bridge");

        PromiseResponse response = promiseService.updatePromise(9L, request);

        assertEquals(Promise.DRAFT, response.getPublicationStatus());
        assertEquals("Build a bigger bridge", response.getTitle());
    }
}
