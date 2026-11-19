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
