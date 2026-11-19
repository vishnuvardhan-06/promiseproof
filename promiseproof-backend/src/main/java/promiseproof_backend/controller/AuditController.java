package promiseproof_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import promiseproof_backend.dto.AuditRecordResponse;
import promiseproof_backend.dto.EvidenceAuditResponse;
import promiseproof_backend.service.AuditService;
import promiseproof_backend.service.EvidenceAuditService;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final EvidenceAuditService evidenceAuditService;
    private final AuditService auditService;

    public AuditController(
            EvidenceAuditService evidenceAuditService,
            AuditService auditService) {

        this.evidenceAuditService = evidenceAuditService;
        this.auditService = auditService;
    }

    // Platform-wide evidence audit trail (administrators only).
    @GetMapping("/evidence/recent")
    public ResponseEntity<List<EvidenceAuditResponse>> getRecentEvidence(
            @RequestParam(defaultValue = "50") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 200);

        return ResponseEntity.ok(evidenceAuditService.getRecent(safeLimit));
    }

    // Platform-wide action audit trail for promises, status verifications and
    // administration changes (administrators only).
    @GetMapping("/recent")
    public ResponseEntity<List<AuditRecordResponse>> getRecent(
            @RequestParam(defaultValue = "50") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 200);

        return ResponseEntity.ok(auditService.getRecent(safeLimit));
    }
}
