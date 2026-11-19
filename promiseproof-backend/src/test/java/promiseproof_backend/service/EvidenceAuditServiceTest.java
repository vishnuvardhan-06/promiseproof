package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.EvidenceAudit;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.entity.Role;
import promiseproof_backend.entity.User;
import promiseproof_backend.repository.EvidenceAuditRepository;
import promiseproof_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EvidenceAuditServiceTest {

    @Mock
    private EvidenceAuditRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EvidenceAuditService auditService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordCapturesReviewerIdentityAndTransition() {

        User reviewer = new User();
        reviewer.setName("Reviewer One");
        reviewer.setEmail("reviewer@promiseproof.local");
        reviewer.setRole(Role.REVIEWER);

        when(userRepository.findByEmailIgnoreCase("reviewer@promiseproof.local"))
                .thenReturn(Optional.of(reviewer));

        // Authenticated session tokens carry authorities, so build the token
        // the same way Spring Security does after a successful login.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "reviewer@promiseproof.local",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                )
        );

        Promise promise = new Promise();
        setId(promise, 7L);
        promise.setTitle("Build 100 hospitals");

        Evidence evidence = new Evidence();
        setId(evidence, 31L);
        evidence.setPromise(promise);
        evidence.setTitle("Government Order 42");
        evidence.setStatus("APPROVED");

        when(auditRepository.save(any(EvidenceAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record(
                evidence,
                "APPROVED",
                "PENDING",
                "APPROVED",
                "Document supports the claim"
        );

        ArgumentCaptor<EvidenceAudit> captor =
                ArgumentCaptor.forClass(EvidenceAudit.class);

        verify(auditRepository).save(captor.capture());

        EvidenceAudit saved = captor.getValue();

        assertEquals("APPROVED", saved.getAction());
        assertEquals("PENDING", saved.getFromStatus());
        assertEquals("APPROVED", saved.getToStatus());
        assertEquals("reviewer@promiseproof.local", saved.getActorEmail());
        assertEquals("REVIEWER", saved.getActorRole());
        assertEquals("Government Order 42", saved.getEvidenceTitle());
        assertEquals(31L, saved.getEvidenceId());
        assertEquals(7L, saved.getPromiseId());
        assertEquals("Document supports the claim", saved.getComment());
    }

    /**
     * Ids are database generated, so the test sets them directly to prove the
     * audit row copies the right references.
     */
    private static void setId(Object target, Long id) {

        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
