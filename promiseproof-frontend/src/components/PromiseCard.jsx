import StatusBadge, { VerificationBadge } from "./StatusBadge";
import EvidencePanel from "./EvidencePanel";
import PromiseSourceManager from "./PromiseSourceManager";

const PUBLICATION_LABELS = {
    DRAFT: "Draft — not public",
    SUBMITTED: "Awaiting publication review",
    REVIEWED: "Reviewed — awaiting publication",
    PUBLISHED: "Published"
};

function formatDate(value) {
    return value ? new Date(value).toLocaleDateString() : "";
}

function PromiseCard({
    promise,
    onEdit,
    onDelete,
    canEdit = false,
    canSubmitEvidence = false,
    canReviewEvidence = false,
    canProposeStatus = false,
    onProposeStatus,
    onSubmitForPublication,
    onDataChanged
}) {

    const publicationStatus = promise.publicationStatus || "PUBLISHED";

    return (
        <div className="promise-card">

            <div className="card-top">

                <span className="promise-id">
                    PROMISE #{promise.id}
                </span>

                <span className="card-badges">
                    <StatusBadge status={promise.status} />
                    <VerificationBadge
                        verificationState={promise.verificationState}
                    />
                    <span
                        className={
                            "publication-chip " +
                            publicationStatus.toLowerCase()
                        }
                    >
                        {PUBLICATION_LABELS[publicationStatus] ||
                            publicationStatus}
                    </span>
                </span>

            </div>

            <h3>{promise.title}</h3>

            <p className="promise-description">
                {promise.description}
            </p>

            <div className="promise-details">

                <p>
                    <strong>Party:</strong> {promise.partyName || "Not specified"}
                </p>

                <p>
                    <strong>Election:</strong> {promise.electionName || "Not specified"}
                </p>

                <p>
                    <strong>State:</strong> {promise.state || "Not specified"}
                </p>

                {promise.category && (
                    <p>
                        <strong>Category:</strong> {promise.category}
                    </p>
                )}

                {promise.target && (
                    <p>
                        <strong>Target:</strong> {promise.target}
                        {promise.targetDate
                            ? ` by ${formatDate(promise.targetDate)}`
                            : ""}
                    </p>
                )}

                {promise.geographicScope && (
                    <p>
                        <strong>Geographic scope:</strong>{" "}
                        {promise.geographicScope}
                    </p>
                )}

            </div>

            {promise.publicationComment && (
                <p className="publication-comment">
                    Publication note: {promise.publicationComment}
                </p>
            )}

            {canEdit && (
                <PromiseSourceManager
                    promise={promise}
                    onChanged={onDataChanged}
                />
            )}

            {!canEdit && (promise.sources || []).length > 0 && (
                <details className="promise-sources">
                    <summary>
                        Original source{(promise.sources || []).length === 1
                            ? ""
                            : "s"} ({promise.sources.length})
                    </summary>

                    <ul className="source-list">
                        {promise.sources.map(source => (
                            <li key={source.id}>
                                <div className="source-line">
                                    <strong>{source.manifestoName}</strong>
                                    <span>
                                        {String(source.sourceType || "MANIFESTO")
                                            .replaceAll("_", " ")}
                                        {source.pageNumber
                                            ? ` • page ${source.pageNumber}`
                                            : ""}
                                    </span>
                                </div>

                                <blockquote>
                                    {source.exactQuotation}
                                </blockquote>

                                {source.sourceUrl && (
                                    <a
                                        href={source.sourceUrl}
                                        target="_blank"
                                        rel="noreferrer"
                                    >
                                        Open source document
                                    </a>
                                )}
                            </li>
                        ))}
                    </ul>
                </details>
            )}

            {canEdit && (
                <div className="card-actions">

                    <button
                        className="edit-button"
                        onClick={() => onEdit(promise)}
                    >
                        Edit
                    </button>

                    {(publicationStatus === "DRAFT" ||
                        publicationStatus === "PUBLISHED") && (
                        <button
                            className="primary-button"
                            onClick={() => onSubmitForPublication(promise)}
                        >
                            {publicationStatus === "DRAFT"
                                ? "Submit for publication"
                                : "Resubmit for publication"}
                        </button>
                    )}

                    <button
                        className="delete-button"
                        onClick={() => onDelete(promise.id)}
                    >
                        Delete
                    </button>

                </div>
            )}

            {canProposeStatus && promise.hasApprovedEvidence && (
                <div className="card-actions">

                    <button
                        className="secondary-button"
                        onClick={() => onProposeStatus(promise)}
                    >
                        Propose status change
                    </button>

                </div>
            )}

            <EvidencePanel
                promise={promise}
                canSubmit={canSubmitEvidence}
                canReview={canReviewEvidence}
            />

        </div>
    );
}

export default PromiseCard;
