package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public projection of a Promise.
 *
 * Creator identity (email and display name) is deliberately omitted: public
 * promise records are unattributed so that researcher contact details are never
 * exposed through public endpoints. Internal responses keep the creator fields.
 *
 * The implementation status and the verification state are reported
 * separately: a status of NOT_ASSESSED means the record has not been assessed
 * yet, which is not the same thing as evidence showing that nothing happened.
 */
public record PublicPromiseResponse(

        Long id,
        String title,
        String description,
        Long partyId,
        String partyName,
        Long electionId,
        String electionName,
        Integer electionYear,
        String state,
        String status,
        String verificationState,
        String category,
        String target,
        LocalDate targetDate,
        String geographicScope,
        List<PromiseSourceResponse> sources,
        boolean hasApprovedEvidence,
        long approvedEvidenceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {

    public PublicPromiseResponse {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (verificationState == null) {
            verificationState = "UNREVIEWED";
        }
    }
}
