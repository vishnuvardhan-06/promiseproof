package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A safe public reference to an approved evidence document that supports an
 * independently reviewed status change.
 *
 * Reviewer notes, submitter identity and moderation details are deliberately
 * not part of this projection. Only metadata that helps a citizen audit the
 * status decision is exposed.
 */
public record PublicSupportingEvidenceResponse(
        Long id,
        String title,
        String evidenceType,
        String issuingDepartment,
        String documentReference,
        LocalDate documentDate,
        LocalDateTime reviewedAt
) {
}
