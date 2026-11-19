const STATUS_LABELS = {
    NOT_ASSESSED: "Not yet assessed",
    NOT_STARTED: "Not Started",
    IN_PROGRESS: "In Progress",
    COMPLETED: "Completed",
    DELAYED: "Delayed",
    PARTIALLY_COMPLETED: "Partially Completed",
    CANCELLED: "Cancelled"
};

const VERIFICATION_LABELS = {
    UNREVIEWED: "Unreviewed",
    UNDER_REVIEW: "Under review",
    EVIDENCE_REVIEWED: "Evidence reviewed",
    STATUS_VERIFIED: "Status verified",
    DISPUTED: "Verification disputed"
};

function StatusBadge({ status }) {

    return (
        <span className={`status-badge ${status?.toLowerCase()}`}>
            {STATUS_LABELS[status] || status}
        </span>
    );
}

export function VerificationBadge({ verificationState }) {

    const state = verificationState || "UNREVIEWED";

    return (
        <span className={`verification-chip ${state.toLowerCase()}`}>
            {VERIFICATION_LABELS[state] || state}
        </span>
    );
}

export default StatusBadge;
