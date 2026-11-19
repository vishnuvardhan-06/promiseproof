package promiseproof_backend.dto;

import java.time.LocalDateTime;

public record EvidenceAuditResponse(
        Long id,
        Long evidenceId,
        Long promiseId,
        String evidenceTitle,
        String action,
        String fromStatus,
        String toStatus,
        String actorEmail,
        String actorRole,
        String comment,
        LocalDateTime createdAt
) {
}
