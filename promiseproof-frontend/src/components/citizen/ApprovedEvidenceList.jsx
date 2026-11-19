import { useState } from "react";

import { downloadEvidence } from "../../services/evidenceApi";
import { formatDate, formatDateTime } from "./statusInfo";

function chronological(items) {
    return [...items].sort((a, b) => {
        const aTime = new Date(
            a.reviewedAt || a.uploadedAt || 0
        ).getTime();
        const bTime = new Date(
            b.reviewedAt || b.uploadedAt || 0
        ).getTime();
        return aTime - bTime;
    });
}

/**
 * Public, approved-evidence-only view. Pending, rejected and moderation
 * information never reaches this component because the backend strips it.
 */
function ApprovedEvidenceList({
    evidence = [],
    loading = false,
    error = "",
    compact = false
}) {
    const [downloadError, setDownloadError] = useState("");

    async function handleDownload(item) {
        try {
            setDownloadError("");
            await downloadEvidence(item.id, item.originalFileName);
        } catch (err) {
            setDownloadError(`Download failed: ${err.message}`);
        }
    }

    return (
        <div className={`approved-evidence${compact ? " compact" : ""}`}>
            <div className="approved-evidence-heading">
                <h4>Approved evidence</h4>
                <span className="public-count-chip">
                    {evidence.length} document{evidence.length === 1 ? "" : "s"}
                </span>
            </div>

            <p className="public-note">
                Only documents that passed independent review are listed here.
                Approving a document verifies the document itself; it does not
                change the implementation status of the promise.
            </p>

            {error && <p className="error-message">{error}</p>}
            {downloadError && <p className="error-message">{downloadError}</p>}

            {loading ? (
                <p className="info-message">Loading approved evidence...</p>
            ) : evidence.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No approved evidence available yet.</strong>
                    <p>
                        Evidence under review is not published, so no private or
                        pending documents are shown here.
                    </p>
                </div>
            ) : (
                <ol className="approved-evidence-list">
                    {chronological(evidence).map(item => (
                        <li key={item.id} className="approved-evidence-item">
                            <div className="approved-evidence-item-head">
                                <h5>{item.title}</h5>
                                <span className="evidence-status approved">
                                    APPROVED
                                </span>
                            </div>

                            {item.description && (
                                <p className="approved-evidence-description">
                                    {item.description}
                                </p>
                            )}

                            <dl className="approved-evidence-meta">
                                <div>
                                    <dt>Issuing department</dt>
                                    <dd>{item.issuingDepartment || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Reference number</dt>
                                    <dd>{item.documentReference || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Document date</dt>
                                    <dd>{formatDate(item.documentDate) || "Not specified"}</dd>
                                </div>
                                <div>
                                    <dt>Uploaded</dt>
                                    <dd>{formatDateTime(item.uploadedAt) || "—"}</dd>
                                </div>
                                <div>
                                    <dt>Reviewer approval</dt>
                                    <dd>{formatDateTime(item.reviewedAt) || "Not recorded"}</dd>
                                </div>
                                {item.claimScope && (
                                    <div className="wide">
                                        <dt>Claim scope</dt>
                                        <dd>{item.claimScope}</dd>
                                    </div>
                                )}
                            </dl>

                            {(item.sourceUrl || item.originalFileName) && (
                                <div className="approved-evidence-source">
                                    <span>Source: </span>
                                    {item.sourceUrl ? (
                                        <a
                                            href={item.sourceUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            {item.sourceUrl}
                                        </a>
                                    ) : (
                                        <span>{item.originalFileName}</span>
                                    )}
                                </div>
                            )}

                            <div className="approved-evidence-file">
                                <span>{item.originalFileName || "Document"}</span>
                                {item.id && (
                                    <button
                                        type="button"
                                        className="secondary-button"
                                        onClick={() => handleDownload(item)}
                                    >
                                        Download / view
                                    </button>
                                )}
                            </div>

                            {item.promiseTitle && (
                                <p className="approved-evidence-promise">
                                    Belongs to: {item.promiseTitle}
                                </p>
                            )}
                        </li>
                    ))}
                </ol>
            )}

            <p className="public-note muted">
                Evidence under review: pending and rejected documents are never
                published.
            </p>
        </div>
    );
}

export default ApprovedEvidenceList;
