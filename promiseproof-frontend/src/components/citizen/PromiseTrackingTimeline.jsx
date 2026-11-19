import {
    formatDate,
    formatDateTime,
    statusDescription,
    statusLabel
} from "./statusInfo";

/**
 * Builds a public timeline from recorded events only. Nothing is inferred:
 * there are no invented progress percentages and no unapproved status changes.
 */
function buildEvents(promise, history, evidence) {
    const events = [];

    if (promise?.publishedAt) {
        events.push({
            key: `published-${promise.id}`,
            time: promise.publishedAt,
            kind: "publication",
            title: "Promise originally published",
            detail: "The promise passed independent publication review and became public."
        });
    }

    (evidence || []).forEach(item => {
        if (item.uploadedAt) {
            events.push({
                key: `submitted-${item.id}`,
                time: item.uploadedAt,
                kind: "evidence",
                title: "Evidence submitted",
                detail: `"${item.title}" was submitted for independent review.`
            });
        }

        if (item.reviewedAt) {
            events.push({
                key: `approved-${item.id}`,
                time: item.reviewedAt,
                kind: "evidence-approved",
                title: "Evidence approved",
                detail: `"${item.title}" passed document, source and claim review.`
            });
        }
    });

    (history || []).forEach(change => {
        events.push({
            key: `status-${change.id}`,
            time: change.changedAt,
            kind: "status",
            title: "Implementation status independently reviewed",
            fromStatus: change.fromStatus,
            toStatus: change.toStatus,
            detail: change.reason || "No review note was recorded.",
            reviewerDecision: change.reviewerDecision,
            reviewedBy: change.reviewedBy,
            supportingEvidence: change.supportingEvidence || []
        });
    });

    if (
        promise?.status === "COMPLETED" &&
        promise?.verificationState === "STATUS_VERIFIED"
    ) {
        events.push({
            key: `completed-${promise.id}`,
            time: null,
            kind: "completed",
            title: "Promise marked completed",
            detail: "Completion was independently verified through the status review workflow."
        });
    }

    return events.sort((a, b) => {
        if (!a.time && !b.time) return 0;
        if (!a.time) return 1;
        if (!b.time) return -1;
        return new Date(a.time).getTime() - new Date(b.time).getTime();
    });
}

function PromiseTrackingTimeline({
    promise,
    history = [],
    evidence = [],
    loading = false,
    error = ""
}) {
    const events = buildEvents(promise, history, evidence);

    return (
        <div className="tracking-timeline-section">
            <div className="approved-evidence-heading">
                <h4>Implementation tracking</h4>
                <span className="public-count-chip">
                    {history.length} verified status change
                    {history.length === 1 ? "" : "s"}
                </span>
            </div>

            <p className="public-note">
                This timeline contains only recorded, publicly reviewable events.
                Evidence approval and implementation-status verification are
                separate processes: approving a document never marks a promise
                complete by itself.
            </p>

            {error && <p className="error-message">{error}</p>}

            {loading ? (
                <p className="info-message">Loading tracking history...</p>
            ) : events.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No verified tracking events yet.</strong>
                    <p>
                        Published promises, approved evidence and independently
                        reviewed status changes will appear here as they are
                        recorded.
                    </p>
                </div>
            ) : (
                <ol className="tracking-timeline">
                    {events.map(event => (
                        <li key={event.key} className={`tracking-event ${event.kind}`}>
                            <span className="tracking-dot" aria-hidden="true" />
                            <div className="tracking-event-body">
                                <div className="tracking-event-head">
                                    <strong>{event.title}</strong>
                                    <time>
                                        {event.time
                                            ? formatDateTime(event.time)
                                            : "Verified"}
                                    </time>
                                </div>

                                {event.fromStatus && (
                                    <div className="tracking-status-change">
                                        <span>{statusLabel(event.fromStatus)}</span>
                                        <span aria-hidden="true">→</span>
                                        <strong>{statusLabel(event.toStatus)}</strong>
                                    </div>
                                )}

                                <p>{event.detail}</p>

                                {event.reviewerDecision && (
                                    <p className="tracking-decision">
                                        Reviewer decision: {event.reviewerDecision}
                                        {event.reviewedBy
                                            ? ` · ${event.reviewedBy}`
                                            : ""}
                                    </p>
                                )}

                                {event.supportingEvidence.length > 0 && (
                                    <div className="tracking-supporting-evidence">
                                        <strong>Supporting approved evidence:</strong>
                                        <ul>
                                            {event.supportingEvidence.map(item => (
                                                <li key={item.id}>
                                                    {item.title}
                                                    {item.issuingDepartment
                                                        ? ` — ${item.issuingDepartment}`
                                                        : ""}
                                                    {item.reviewedAt
                                                        ? ` (approved ${formatDate(item.reviewedAt)})`
                                                        : ""}
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                )}
                            </div>
                        </li>
                    ))}
                </ol>
            )}

            <div className="tracking-legend">
                <h5>What the statuses mean</h5>
                <ul>
                    <li><strong>NOT_ASSESSED</strong> — {statusDescription("NOT_ASSESSED")}</li>
                    <li><strong>NOT_STARTED</strong> — {statusDescription("NOT_STARTED")}</li>
                    <li><strong>IN_PROGRESS</strong> — {statusDescription("IN_PROGRESS")}</li>
                    <li><strong>DELAYED</strong> — {statusDescription("DELAYED")}</li>
                    <li><strong>COMPLETED</strong> — {statusDescription("COMPLETED")}</li>
                    <li><strong>DISPUTED</strong> — {statusDescription("DISPUTED")}</li>
                </ul>
                <p className="public-note muted">
                    No progress percentage is displayed because no reliable,
                    independently verified progress values have been published.
                </p>
            </div>
        </div>
    );
}

export default PromiseTrackingTimeline;
