package promiseproof_backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated public dashboard statistics.
 *
 * The response is intentionally free of internal reviewer or audit details:
 * those live behind the administrator-only audit endpoints.
 */
public record StatsOverviewResponse(
        long totalPromises,
        long totalParties,
        long totalElections,
        long totalUsers,
        long totalEvidence,
        long approvedEvidence,
        long pendingEvidence,
        long rejectedEvidence,
        long promisesWithApprovedEvidence,
        long promisesWithoutApprovedEvidence,
        long totalStates,
        Map<String, Long> statusCounts,
        Map<String, Long> verificationStateCounts,
        List<ProgressGroupResponse> partyProgress,
        List<ProgressGroupResponse> electionProgress,
        List<ProgressGroupResponse> stateProgress
) {
}
