import StatusBadge, { VerificationBadge } from "../StatusBadge";
import { formatDate, statusDescription } from "./statusInfo";

/**
 * A published promise as seen by a citizen. Every field shown here comes from
 * the public API; no internal publication comment, creator identity or review
 * moderation detail is displayed.
 */
function PublicPromiseCard({
    promise,
    onViewDetails,
    onViewEvidence,
    onTrackProgress
}) {
    const approvedCount = Number(promise.approvedEvidenceCount || 0);
    const election = [promise.electionName, promise.electionYear]
        .filter(Boolean)
        .join(" · ");

    return (
        <article className="public-promise-card">
            <div className="public-card-head">
                <span className="public-card-id">PROMISE #{promise.id}</span>
                <div className="public-card-badges">
                    <StatusBadge status={promise.status} />
                    <VerificationBadge verificationState={promise.verificationState} />
                </div>
            </div>

            <h3 className="public-card-title">{promise.title}</h3>

            {promise.description && (
                <p className="public-card-description">{promise.description}</p>
            )}

            <p className="public-card-status-note">
                {statusDescription(promise.status)}
            </p>

            <dl className="public-card-meta">
                <div>
                    <dt>Party</dt>
                    <dd>{promise.partyName || "Not specified"}</dd>
                </div>
                <div>
                    <dt>Election</dt>
                    <dd>{election || "Not specified"}</dd>
                </div>
                <div>
                    <dt>State / scope</dt>
                    <dd>
                        {promise.state || promise.geographicScope || "Not specified"}
                    </dd>
                </div>
                <div>
                    <dt>Category</dt>
                    <dd>{promise.category || "Not specified"}</dd>
                </div>
                <div>
                    <dt>Target date</dt>
                    <dd>
                        {promise.targetDate
                            ? formatDate(promise.targetDate)
                            : promise.target || "Not available"}
                    </dd>
                </div>
                <div>
                    <dt>Approved evidence</dt>
                    <dd>
                        {approvedCount === 0
                            ? "No approved evidence yet"
                            : `${approvedCount} document${approvedCount === 1 ? "" : "s"}`}
                    </dd>
                </div>
            </dl>

            <div className="public-card-footer">
                <span>Updated {formatDate(promise.updatedAt) || "—"}</span>
                <div className="public-card-actions">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => onViewDetails?.(promise)}
                    >
                        View Details
                    </button>
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => onViewEvidence?.(promise)}
                    >
                        View Approved Evidence
                    </button>
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() => onTrackProgress?.(promise)}
                    >
                        Track Progress
                    </button>
                </div>
            </div>
        </article>
    );
}

export default PublicPromiseCard;
