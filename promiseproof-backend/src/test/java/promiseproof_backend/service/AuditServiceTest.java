package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import promiseproof_backend.entity.AuditRecord;
import promiseproof_backend.repository.AuditRecordRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRecordRepository auditRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void recordsPlatformAuditEvents() {

        auditService.record(
                "PROMISE_CREATED",
                "PROMISE",
                1L,
                "Build 100 hospitals",
                "researcher@example.com",
                "RESEARCHER",
                null
        );

        verify(auditRepository).save(any(AuditRecord.class));
    }

    @Test
    void recentAuditUsesDatabaseLevelPagination() {

        when(auditRepository.findByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(Page.empty());

        assertTrue(auditService.getRecent(50).isEmpty());

        verify(auditRepository)
                .findByOrderByCreatedAtDesc(any(Pageable.class));
    }
}
