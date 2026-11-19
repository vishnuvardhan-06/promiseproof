import { useCallback, useEffect, useState } from "react";

import {
    getPendingEvidence,
    reviewEvidence,
    downloadEvidence
} from "../services/evidenceApi";
import {
    decideStatusProposal,
    getPendingStatusProposals
} from "../services/statusApi";
import {
    decidePublication,
    getPublicationQueue
} from "../services/api";
import EvidenceReviewForm from "../components/EvidenceReviewForm";

function formatDateTime(value) {

    if (!value) {
        return "";
    }

    return new Date(value).toLocaleString();
}

function ReviewQueuePage() {

    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [statusProposals, setStatusProposals] = useState([]);
    const [statusLoading, setStatusLoading] = useState(true);
    const [statusDecidingId, setStatusDecidingId] = useState(null);
    const [statusError, setStatusError] = useState("");
    const [statusSuccess, setStatusSuccess] = useState("");

    const [publicationQueue, setPublicationQueue] = useState([]);
    const [publicationLoading, setPublicationLoading] = useState(true);
    const [publicationBusyId, setPublicationBusyId] = useState(null);
    const [publicationError, setPublicationError] = useState("");
    const [publicationSuccess, setPublicationSuccess] = useState("");

    const loadQueue = useCallback(async () => {

        try {
            setLoading(true);
            setError("");
            setItems(await getPendingEvidence());
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    const loadStatusProposals = useCallback(async () => {

        try {
            setStatusLoading(true);
            setStatusError("");

            setStatusProposals(await getPendingStatusProposals());
        } catch (err) {
            setStatusError(err.message);
        } finally {
            setStatusLoading(false);
        }
    }, []);

    const loadPublicationQueue = useCallback(async () => {

        try {
            setPublicationLoading(true);
            setPublicationError("");
            setPublicationQueue(await getPublicationQueue());
        } catch (err) {
            setPublicationError(err.message);
        } finally {
            setPublicationLoading(false);
        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await Promise.all([
                loadQueue(),
                loadStatusProposals(),
                loadPublicationQueue()
            ]);

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadQueue, loadStatusProposals, loadPublicationQueue]);

    async function handleReview(item, review) {

        try {

            setError("");
            setSuccess("");

            await reviewEvidence(item.id, review);

            setSuccess(
                "Review recorded for \"" + item.title + "\"."
            );

            await loadQueue();

        } catch (err) {

            setError(err.message);

        }
    }

    async function handleStatusDecision(item, decision) {

        const comment = decision === "REJECTED"
            ? window.prompt("Explain why this status change is being rejected:")
            : window.prompt("Optional review comment:");

        if (comment === null) {
            return;
        }

        if (decision === "REJECTED" && !comment.trim()) {
            setStatusError("Please provide a reason for rejecting the proposal.");
            return;
        }

        try {

            setStatusDecidingId(item.id);
            setStatusError("");
            setStatusSuccess("");

            await decideStatusProposal(item.id, decision, comment);

            setStatusSuccess(
                `The status change proposal for "${item.promiseTitle}" was ${decision.toLowerCase()}.`
            );

            await loadStatusProposals();

        } catch (err) {

            setStatusError(err.message);

        } finally {

            setStatusDecidingId(null);

        }
    }

    async function handlePublicationDecision(item, decision) {

        let comment;

        if (decision === "REJECTED") {
            comment = window.prompt(
                "Explain why this promise is being returned to draft:"
            );

            if (comment === null) {
                return;
            }

            if (!comment.trim()) {
                setPublicationError(
                    "Please provide a reason for returning the promise to draft."
                );
                return;
            }
        } else {
            comment = window.prompt("Optional review comment:") || "";
        }

        try {

            setPublicationBusyId(item.id);
            setPublicationError("");
            setPublicationSuccess("");

            await decidePublication(item.id, decision, comment);

            setPublicationSuccess(
                `"${item.title}" was marked ${decision.toLowerCase()}.`
            );

            await loadPublicationQueue();

        } catch (err) {

            setPublicationError(err.message);

        } finally {

            setPublicationBusyId(null);

        }
    }

    async function handleDownload(id, fileName) {

        try {
            setError("");
            setPublicationError("");
            await downloadEvidence(id, fileName);
        } catch (err) {
            setError(err.message);
        }
    }

    return (

        <section className="review-page">

            <div className="page-heading">

                <div>
                    <p className="eyebrow">QUALITY CONTROL</p>
                    <h2>Review queue</h2>
                    <p>
                        Record three separate decisions for every document:
                        whether it may be published, whether it is traceable to
                        the claimed issuer, and whether it supports the specific
                        claim. Approval to publish is not proof of completion.
                    </p>
                </div>

                <span className="promise-count">
                    {items.length} awaiting review
                </span>

            </div>

            <div className="page-actions">
                <button
                    type="button"
                    className="secondary-button"
                    onClick={loadQueue}
                    disabled={loading}
                >
                    {loading ? "Refreshing..." : "Refresh queue"}
                </button>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {loading ? (

                <p className="info-message">Loading pending evidence...</p>

            ) : items.length === 0 ? (

                <div className="empty-state">
                    <h3>Nothing awaiting evidence review</h3>
                    <p>
                        New submissions will appear here as soon as citizens or
                        researchers upload them.
                    </p>
                </div>

            ) : (

                <div className="review-list">

                    {items.map(item => (

                        <article className="review-card" key={item.id}>

                            <div className="review-card-top">
                                <span className="review-type">
                                    {item.evidenceType?.replaceAll("_", " ")}
                                </span>
                                <span className="status-chip pending">
                                    {item.status}
                                </span>
                            </div>

                            <h3>{item.title}</h3>

                            <p className="review-promise">
                                Promise: <strong>{item.promiseTitle}</strong>
                            </p>

                            {item.description && (
                                <p className="review-description">
                                    {item.description}
                                </p>
                            )}

                            <dl className="review-meta">
                                <div>
                                    <dt>Submitted by</dt>
                                    <dd>
                                        {item.uploadedByName || item.uploadedByEmail || "Unknown"}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Submitted</dt>
                                    <dd>{formatDateTime(item.uploadedAt)}</dd>
                                </div>
                                <div>
                                    <dt>File</dt>
                                    <dd>
                                        {item.originalFileName} •{" "}
                                        {((item.fileSize || 0) / 1024).toFixed(1)} KB
                                    </dd>
                                </div>
                                {item.issuingDepartment && (
                                    <div>
                                        <dt>Issuing department</dt>
                                        <dd>{item.issuingDepartment}</dd>
                                    </div>
                                )}
                                {item.documentReference && (
                                    <div>
                                        <dt>Document reference</dt>
                                        <dd>{item.documentReference}</dd>
                                    </div>
                                )}
                                {item.documentDate && (
                                    <div>
                                        <dt>Document date</dt>
                                        <dd>{item.documentDate}</dd>
                                    </div>
                                )}
                                {item.claimScope && (
                                    <div>
                                        <dt>Claim scope</dt>
                                        <dd>{item.claimScope}</dd>
                                    </div>
                                )}
                                {item.sha256Hash && (
                                    <div>
                                        <dt>SHA-256</dt>
                                        <dd className="hash-value">
                                            {item.sha256Hash}
                                        </dd>
                                    </div>
                                )}
                                {item.sourceUrl && (
                                    <div>
                                        <dt>Source</dt>
                                        <dd>
                                            <a
                                                href={item.sourceUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                            >
                                                Open source
                                            </a>
                                        </dd>
                                    </div>
                                )}
                            </dl>

                            <div className="review-actions">

                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() =>
                                        handleDownload(
                                            item.id,
                                            item.originalFileName
                                        )
                                    }
                                >
                                    Download document
                                </button>

                            </div>

                            <EvidenceReviewForm
                                onReview={review =>
                                    handleReview(item, review)}
                            />

                        </article>
                    ))}

                </div>
            )}

            <section className="dashboard-section">

                <div className="section-heading">
                    <div>
                        <p className="eyebrow">PUBLICATION CONTROL</p>
                        <h2>Promise publication queue</h2>
                    </div>
                    <span className="promise-count">
                        {publicationQueue.length} awaiting publication decision
                    </span>
                </div>

                <p className="panel-note">
                    Check the original manifesto source and the exact wording
                    before publication. A promise must be marked reviewed before
                    it can be published, and it never appears publicly while it
                    is a draft, submitted or merely reviewed.
                </p>

                {publicationError && (
                    <p className="error-message">{publicationError}</p>
                )}

                {publicationSuccess && (
                    <p className="success-message">{publicationSuccess}</p>
                )}

                {publicationLoading ? (
                    <p className="info-message">Loading publication queue...</p>
                ) : publicationQueue.length === 0 ? (
                    <div className="empty-state">
                        <h3>No promises awaiting publication</h3>
                        <p>
                            Drafts submitted by researchers will appear here.
                        </p>
                    </div>
                ) : (
                    <div className="review-list">
                        {publicationQueue.map(item => (
                            <article className="review-card" key={item.id}>
                                <div className="review-card-top">
                                    <span className="review-type">
                                        {item.publicationStatus}
                                    </span>
                                    <span className="status-chip pending">
                                        {item.status}
                                    </span>
                                </div>

                                <h3>{item.title}</h3>

                                <p className="review-description">
                                    {item.description}
                                </p>

                                <dl className="review-meta">
                                    <div>
                                        <dt>Party</dt>
                                        <dd>{item.partyName || "Not specified"}</dd>
                                    </div>
                                    <div>
                                        <dt>Election</dt>
                                        <dd>{item.electionName || "Not specified"}</dd>
                                    </div>
                                    <div>
                                        <dt>State</dt>
                                        <dd>{item.state || "Not specified"}</dd>
                                    </div>
                                    <div>
                                        <dt>Created by</dt>
                                        <dd>
                                            {item.createdByName ||
                                                item.createdByEmail ||
                                                "Unknown"}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Submitted</dt>
                                        <dd>{formatDateTime(item.submittedAt)}</dd>
                                    </div>
                                </dl>

                                <div className="promise-sources">
                                    <h4>Original sources</h4>

                                    {(item.sources || []).length === 0 ? (
                                        <p className="source-empty">
                                            No source attached. Do not publish
                                            this promise.
                                        </p>
                                    ) : (
                                        <ul className="source-list">
                                            {item.sources.map(source => (
                                                <li key={source.id}>
                                                    <div className="source-line">
                                                        <strong>
                                                            {source.manifestoName}
                                                        </strong>
                                                        <span>
                                                            {String(
                                                                source.sourceType ||
                                                                "MANIFESTO"
                                                            ).replaceAll("_", " ")}
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
                                    )}
                                </div>

                                {item.publicationComment && (
                                    <p className="publication-comment">
                                        Previous note: {item.publicationComment}
                                    </p>
                                )}

                                <div className="review-actions">
                                    {item.publicationStatus === "SUBMITTED" && (
                                        <button
                                            type="button"
                                            className="primary-button"
                                            disabled={
                                                publicationBusyId === item.id
                                            }
                                            onClick={() =>
                                                handlePublicationDecision(
                                                    item,
                                                    "REVIEWED"
                                                )
                                            }
                                        >
                                            Mark source and wording reviewed
                                        </button>
                                    )}

                                    {item.publicationStatus === "REVIEWED" && (
                                        <button
                                            type="button"
                                            className="approve-button"
                                            disabled={
                                                publicationBusyId === item.id
                                            }
                                            onClick={() =>
                                                handlePublicationDecision(
                                                    item,
                                                    "PUBLISHED"
                                                )
                                            }
                                        >
                                            Publish
                                        </button>
                                    )}

                                    <button
                                        type="button"
                                        className="reject-button"
                                        disabled={publicationBusyId === item.id}
                                        onClick={() =>
                                            handlePublicationDecision(
                                                item,
                                                "REJECTED"
                                            )
                                        }
                                    >
                                        Return to draft
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}

            </section>

            <section className="dashboard-section">

                <div className="section-heading">
                    <div>
                        <p className="eyebrow">STATUS VERIFICATION</p>
                        <h2>Promise status proposals</h2>
                    </div>
                    <span className="promise-count">
                        {statusProposals.length} awaiting decision
                    </span>
                </div>

                {statusError && (
                    <p className="error-message">{statusError}</p>
                )}

                {statusSuccess && (
                    <p className="success-message">{statusSuccess}</p>
                )}

                {statusLoading ? (
                    <p className="info-message">Loading status proposals...</p>
                ) : statusProposals.length === 0 ? (
                    <div className="empty-state">
                        <h3>No status proposals awaiting decision</h3>
                        <p>
                            Researchers can propose a promise status change when
                            they hold approved evidence. Decisions here are what
                            make a status publicly verifiable.
                        </p>
                    </div>
                ) : (
                    <div className="review-list">
                        {statusProposals.map(item => (
                            <article className="review-card" key={item.id}>
                                <div className="review-card-top">
                                    <span className="review-type">
                                        {item.currentStatus} → {item.proposedStatus}
                                    </span>
                                    <span className="status-chip pending">
                                        {item.status}
                                    </span>
                                </div>

                                <h3>{item.promiseTitle}</h3>

                                {item.promisePartyName && (
                                    <p className="review-promise">
                                        {item.promisePartyName}
                                        {item.promiseElectionName
                                            ? ` • ${item.promiseElectionName}`
                                            : ""}
                                    </p>
                                )}

                                <div className="proposal-section">
                                    <h4>Original promise wording</h4>
                                    <p>
                                        {item.promiseDescription ||
                                            "No description recorded."}
                                    </p>
                                </div>

                                <div className="proposal-section">
                                    <h4>Researcher's explanation</h4>
                                    <p>
                                        {item.reason ||
                                            "No explanation provided."}
                                    </p>
                                </div>

                                <div className="proposal-section">
                                    <h4>Current and proposed status</h4>
                                    <p>
                                        <strong>{item.currentStatus}</strong>
                                        {" → "}
                                        <strong>{item.proposedStatus}</strong>
                                    </p>
                                </div>

                                <div className="proposal-section">
                                    <h4>Supporting evidence</h4>

                                    <dl className="review-meta">
                                        <div>
                                            <dt>Evidence status</dt>
                                            <dd>
                                                {item.evidenceStatus ||
                                                    "No longer available"}
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Title</dt>
                                            <dd>
                                                {item.evidenceTitle ||
                                                    "Not available"}
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Type</dt>
                                            <dd>
                                                {item.evidenceType
                                                    ? item.evidenceType.replaceAll(
                                                        "_",
                                                        " "
                                                    )
                                                    : "Not available"}
                                            </dd>
                                        </div>
                                        <div>
                                            <dt>Uploaded by</dt>
                                            <dd>
                                                {item.evidenceUploadedByName ||
                                                    item.evidenceUploadedByEmail ||
                                                    "Unknown"}
                                            </dd>
                                        </div>
                                        {item.evidenceSourceUrl && (
                                            <div>
                                                <dt>Source</dt>
                                                <dd>
                                                    <a
                                                        href={
                                                            item.evidenceSourceUrl
                                                        }
                                                        target="_blank"
                                                        rel="noreferrer"
                                                    >
                                                        Open source
                                                    </a>
                                                </dd>
                                            </div>
                                        )}
                                    </dl>

                                    {item.evidenceId && (
                                        <button
                                            type="button"
                                            className="secondary-button"
                                            onClick={() =>
                                                handleDownload(
                                                    item.evidenceId,
                                                    item.evidenceFileName
                                                )
                                            }
                                        >
                                            Download supporting document
                                        </button>
                                    )}
                                </div>

                                <div className="proposal-section">
                                    <h4>Previous verification history</h4>

                                    {(item.statusHistory || []).length === 0 ? (
                                        <p className="empty-note">
                                            No previous status transitions
                                            recorded.
                                        </p>
                                    ) : (
                                        <ol className="audit-list">
                                            {item.statusHistory.map(entry => (
                                                <li key={entry.id}>
                                                    <div className="audit-line">
                                                        <span className="audit-action">
                                                            {entry.fromStatus} →{" "}
                                                            {entry.toStatus}
                                                        </span>
                                                        <span className="audit-date">
                                                            {formatDateTime(
                                                                entry.createdAt
                                                            )}
                                                        </span>
                                                    </div>
                                                    <p className="audit-actor">
                                                        {entry.changedByName ||
                                                            entry.changedByEmail ||
                                                            "system"}
                                                    </p>
                                                    {entry.reason && (
                                                        <p className="audit-comment">
                                                            {entry.reason}
                                                        </p>
                                                    )}
                                                </li>
                                            ))}
                                        </ol>
                                    )}
                                </div>

                                <dl className="review-meta">
                                    <div>
                                        <dt>Proposed by</dt>
                                        <dd>
                                            {item.proposedByName ||
                                                item.proposedByEmail ||
                                                "Unknown"}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Evidence checked again at decision</dt>
                                        <dd>
                                            The server revalidates that the
                                            document still exists, still belongs
                                            to this promise and is still
                                            approved.
                                        </dd>
                                    </div>
                                </dl>

                                <div className="review-actions">
                                    <button
                                        type="button"
                                        className="approve-button"
                                        disabled={statusDecidingId === item.id}
                                        onClick={() => handleStatusDecision(item, "APPROVED")}
                                    >
                                        Approve status change
                                    </button>

                                    <button
                                        type="button"
                                        className="reject-button"
                                        disabled={statusDecidingId === item.id}
                                        onClick={() => handleStatusDecision(item, "REJECTED")}
                                    >
                                        Reject status change
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}

            </section>

        </section>
    );
}

export default ReviewQueuePage;
