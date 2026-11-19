package promiseproof_backend.dto;

/**
 * Administrator-only status correction.
 *
 * This is the audited exception path for fixing a demonstrably wrong status.
 * It never bypasses the trail: every correction records the actor, the reason,
 * a status-change history row, and a platform audit entry, and resets the
 * promise verification state to UNREVIEWED.
 */
public class PromiseStatusCorrectionRequest {

    private String status;
    private String reason;

    public PromiseStatusCorrectionRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
