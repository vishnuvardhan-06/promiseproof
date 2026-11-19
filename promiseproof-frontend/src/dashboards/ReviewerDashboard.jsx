import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import {
    downloadEvidence,
    getPendingEvidence,
    reviewEvidence
} from "../services/evidenceApi";
import {
    decidePublication,
    getPublicationQueue
} from "../services/api";
import {
    decideStatusProposal,
    getPendingStatusProposals
} from "../services/statusApi";
import EvidenceReviewForm from "../components/EvidenceReviewForm";

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "";
}

function ReviewerDashboard() {
    const { user } = useAuth();

    // Active view: "evidence" | "publication" | "status"
    const [activeTab, setActiveTab] = useState("evidence");

    // Queues
    const [evidenceItems, setEvidenceItems] = useState([]);
    const [publicationItems, setPublicationItems] = useState([]);
    const [statusProposals, setStatusProposals] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Busy states for individual cards
    const [publicationBusyId, setPublicationBusyId] = useState(null);
    const [statusBusyId, setStatusBusyId] = useState(null);
    const [commentDrafts, setCommentDrafts] = useState({});

    // Ref to highlight / scroll to first item
    const firstItemRef = useRef(null);

    const loadAllQueues = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const [evidenceData, publicationData, statusData] = await Promise.all([
                getPendingEvidence().catch(() => []),
                getPublicationQueue().catch(() => []),
                getPendingStatusProposals().catch(() => [])
            ]);

            setEvidenceItems(evidenceData || []);
            setPublicationItems(publicationData || []);
            setStatusProposals(statusData || []);
        } catch (err) {
            setError("Could not load reviewer queues: " + err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function refresh() {
            await loadAllQueues();
            if (cancelled) return;
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadAllQueues]);

    // Action: Review next item
    function handleReviewNextItem() {
        if (evidenceItems.length > 0) {
            setActiveTab("evidence");
        } else if (publicationItems.length > 0) {
            setActiveTab("publication");
        } else if (statusProposals.length > 0) {
            setActiveTab("status");
        } else {
            setSuccess("Review queue is clear! All items have been processed.");
            return;
        }

        setTimeout(() => {
            firstItemRef.current?.scrollIntoView({ behavior: "smooth", block: "center" });
        }, 150);
    }

    // Evidence review decision
    async function handleEvidenceReview(item, reviewData) {
        try {
            setError("");
            setSuccess("");

            await reviewEvidence(item.id, reviewData);
            setSuccess(
                `Decision recorded for "${item.title}". It has been marked ${reviewData.documentReview.toLowerCase()}.`
            );

            const updated = await getPendingEvidence();
            setEvidenceItems(updated);
        } catch (err) {
            setError("Review failed: " + err.message);
        }
    }

    // Download evidence
    async function handleDownload(item) {
        try {
            setError("");
            await downloadEvidence(item.id, item.originalFileName);
        } catch (err) {
            setError("Download failed: " + err.message);
        }
    }

    // Publication review decision
    async function handlePublicationDecision(item, decision) {
        const comment = commentDrafts[`pub_${item.id}`] || "";

        if (decision === "REJECTED" && !comment.trim()) {
            setError("Please provide a reviewer comment explaining why publication is rejected.");
            return;
        }

        try {
            setPublicationBusyId(item.id);
            setError("");
            setSuccess("");

            await decidePublication(item.id, decision, comment);
            setSuccess(`Promise "${item.title}" marked as ${decision.toLowerCase()}.`);

            setCommentDrafts(prev => {
                const next = { ...prev };
                delete next[`pub_${item.id}`];
                return next;
            });

            const updated = await getPublicationQueue();
            setPublicationItems(updated);
        } catch (err) {
            setError("Publication decision failed: " + err.message);
        } finally {
            setPublicationBusyId(null);
        }
    }

    // Status proposal decision
    async function handleStatusDecision(proposal, decision) {
        const comment = commentDrafts[`stat_${proposal.id}`] || "";

        try {
            setStatusBusyId(proposal.id);
            setError("");
            setSuccess("");

            await decideStatusProposal(proposal.id, decision, comment);
            setSuccess(`Status proposal for Promise #${proposal.promiseId} was ${decision.toLowerCase()}.`);

            setCommentDrafts(prev => {
                const next = { ...prev };
                delete next[`stat_${proposal.id}`];
                return next;
            });

            const updated = await getPendingStatusProposals();
            setStatusProposals(updated);
        } catch (err) {
            setError("Status decision failed: " + err.message);
        } finally {
            setStatusBusyId(null);
        }
    }

    const totalPending = evidenceItems.length + publicationItems.length + statusProposals.length;

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading reviewer workspace...</p>
            </section>
        );
    }

    return (
        <section className="dashboard-page">
            {/* Welcome Header */}
            <div className="page-heading">
                <div>
                    <p className="eyebrow">REVIEWER WORKSPACE</p>
                    <h2>Reviewer Queue & Quality Control</h2>
                    <p>
                        Independent quality control for evidence documents, researcher publication
                        requests, and status proposals. Verify evidence before it becomes public.
                    </p>
                </div>

                <div className="dashboard-identity">
                    <span className="nav-user-name">{user?.name}</span>
                    <span className="role-chip">REVIEWER</span>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {/* Meaningful Summary Cards (Reviewer specific) */}
            <section className="summary-grid dashboard-summary">
                <div className="summary-card">
                    <span>Pending evidence</span>
                    <strong style={{ color: evidenceItems.length > 0 ? "#b45309" : "#166534" }}>
                        {evidenceItems.length}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Pending publication</span>
                    <strong style={{ color: publicationItems.length > 0 ? "#2563eb" : "#166534" }}>
                        {publicationItems.length}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Status proposals</span>
                    <strong style={{ color: statusProposals.length > 0 ? "#7c3aed" : "#166534" }}>
                        {statusProposals.length}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Total awaiting review</span>
                    <strong style={{ color: totalPending > 0 ? "#b91c1c" : "#166534" }}>
                        {totalPending}
                    </strong>
                </div>
            </section>

            {/* Main Action Banner */}
            <section className="dashboard-banner">
                <div>
                    <strong>{totalPending} item{totalPending === 1 ? "" : "s"} waiting for decision</strong>
                    <p>
                        {totalPending === 0
                            ? "All queues are clear! Everything has been independently reviewed."
                            : "Click 'Review next item' to jump straight to the highest priority pending record."}
                    </p>
                </div>
                <button
                    type="button"
                    className="primary-button"
                    onClick={handleReviewNextItem}
                    disabled={totalPending === 0}
                >
                    ⚡ Review next item
                </button>
            </section>

            {/* Layout: Sidebar + Main Area */}
            <div className="dashboard-layout">
                {/* Reviewer Sidebar Menu */}
                <aside className="dashboard-sidebar">
                    <div className="dashboard-sidebar-title">Review Queues</div>
                    <ul className="dashboard-sidebar-nav">
                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "evidence" ? "active" : ""}`}
                                onClick={() => setActiveTab("evidence")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📑</span> Pending Evidence
                                </span>
                                {evidenceItems.length > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {evidenceItems.length}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "publication" ? "active" : ""}`}
                                onClick={() => setActiveTab("publication")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📰</span> Publication Queue
                                </span>
                                {publicationItems.length > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {publicationItems.length}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "status" ? "active" : ""}`}
                                onClick={() => setActiveTab("status")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>⚖️</span> Status Proposals
                                </span>
                                {statusProposals.length > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {statusProposals.length}
                                    </span>
                                )}
                            </button>
                        </li>
                    </ul>

                    <div className="dashboard-sidebar-divider" />

                    <div className="dashboard-sidebar-actions">
                        <button
                            type="button"
                            className="secondary-button"
                            style={{ width: "100%", textAlign: "center" }}
                            onClick={loadAllQueues}
                        >
                            🔄 Refresh queues
                        </button>
                        <Link to="/review" className="dashboard-sidebar-link">
                            <span>🔍</span> Full review page
                        </Link>
                        <Link to="/promises" className="dashboard-sidebar-link">
                            <span>📋</span> Browse public promises
                        </Link>
                    </div>
                </aside>

                {/* Main Task Area */}
                <main className="dashboard-main-area">
                    {/* TAB 1: EVIDENCE REVIEW QUEUE */}
                    {activeTab === "evidence" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">EVIDENCE QUALITY CONTROL</p>
                                    <h3>Pending evidence documents ({evidenceItems.length})</h3>
                                </div>
                                <span className="panel-tag">
                                    {evidenceItems.length} awaiting decision
                                </span>
                            </div>

                            <p className="panel-note">
                                Evaluate each document across 3 independent criteria: document authenticity,
                                source traceability, and claim relevance. Accepted documents are published on the public record.
                            </p>

                            {evidenceItems.length === 0 ? (
                                <div className="empty-state">
                                    <h3>No evidence awaiting review</h3>
                                    <p>Submissions from citizens and researchers will appear here automatically.</p>
                                </div>
                            ) : (
                                <div className="review-list">
                                    {evidenceItems.map((item, index) => (
                                        <article
                                            className="review-card"
                                            key={item.id}
                                            ref={index === 0 ? firstItemRef : null}
                                        >
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
                                                <p className="review-description">{item.description}</p>
                                            )}

                                            <dl className="review-meta">
                                                <div>
                                                    <dt>Submitted by</dt>
                                                    <dd>{item.uploadedByName || item.uploadedByEmail || "Citizen"}</dd>
                                                </div>
                                                <div>
                                                    <dt>Submitted date</dt>
                                                    <dd>{formatDateTime(item.uploadedAt)}</dd>
                                                </div>
                                                <div>
                                                    <dt>File details</dt>
                                                    <dd>
                                                        {item.originalFileName} • {((item.fileSize || 0) / 1024).toFixed(1)} KB
                                                    </dd>
                                                </div>
                                                {item.issuingDepartment && (
                                                    <div>
                                                        <dt>Department</dt>
                                                        <dd>{item.issuingDepartment}</dd>
                                                    </div>
                                                )}
                                                {item.documentReference && (
                                                    <div>
                                                        <dt>Document reference</dt>
                                                        <dd>{item.documentReference}</dd>
                                                    </div>
                                                )}
                                                {item.sourceUrl && (
                                                    <div>
                                                        <dt>Source link</dt>
                                                        <dd>
                                                            <a href={item.sourceUrl} target="_blank" rel="noreferrer">
                                                                Open public URL
                                                            </a>
                                                        </dd>
                                                    </div>
                                                )}
                                            </dl>

                                            <div className="review-actions">
                                                <button
                                                    type="button"
                                                    className="secondary-button"
                                                    onClick={() => handleDownload(item)}
                                                >
                                                    Download document
                                                </button>
                                            </div>

                                            {/* 3-Dimension Evidence Review Form */}
                                            <EvidenceReviewForm
                                                onReview={review => handleEvidenceReview(item, review)}
                                            />
                                        </article>
                                    ))}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 2: PUBLICATION QUEUE */}
                    {activeTab === "publication" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">PUBLICATION VERIFICATION</p>
                                    <h3>Promise publication queue ({publicationItems.length})</h3>
                                </div>
                                <span className="panel-tag">
                                    {publicationItems.length} awaiting decision
                                </span>
                            </div>

                            <p className="panel-note">
                                Researchers submit promise drafts for publication review. Verify that the verbatim
                                manifesto quotation matches the source, verify party attribution, and decide publication.
                            </p>

                            {publicationItems.length === 0 ? (
                                <div className="empty-state">
                                    <h3>No promises awaiting publication</h3>
                                    <p>Drafts submitted by researchers will appear here for verification.</p>
                                </div>
                            ) : (
                                <div className="review-list">
                                    {publicationItems.map((item, index) => (
                                        <article
                                            className="review-card"
                                            key={item.id}
                                            ref={index === 0 ? firstItemRef : null}
                                        >
                                            <div className="review-card-top">
                                                <span className="review-type">
                                                    {item.publicationStatus}
                                                </span>
                                                <span className="status-chip pending">
                                                    {item.status}
                                                </span>
                                            </div>

                                            <h3>{item.title}</h3>

                                            <p className="review-promise">
                                                Party: <strong>{item.partyName || "Unspecified"}</strong> • Election:{" "}
                                                <strong>{item.electionName || "Unspecified"}</strong>
                                            </p>

                                            <p className="review-description">{item.description}</p>

                                            {/* Attached Sources */}
                                            {(item.sources || []).length > 0 && (
                                                <div className="proposal-section">
                                                    <h4>Attached Original Sources ({item.sources.length})</h4>
                                                    {item.sources.map(src => (
                                                        <blockquote key={src.id} style={{ margin: "6px 0", fontSize: "13px" }}>
                                                            <strong>{src.manifestoName}:</strong> "{src.exactQuotation}"
                                                            {src.pageNumber && ` (p. ${src.pageNumber})`}
                                                        </blockquote>
                                                    ))}
                                                </div>
                                            )}

                                            {/* Review Decision Form */}
                                            <div style={{ marginTop: "16px" }}>
                                                <textarea
                                                    rows="2"
                                                    placeholder="Reviewer comment or rejection reason (required for rejection)..."
                                                    value={commentDrafts[`pub_${item.id}`] || ""}
                                                    onChange={e =>
                                                        setCommentDrafts(prev => ({
                                                            ...prev,
                                                            [`pub_${item.id}`]: e.target.value
                                                        }))
                                                    }
                                                    style={{ width: "100%", marginBottom: "10px" }}
                                                />

                                                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                                                    <button
                                                        type="button"
                                                        className="approve-button"
                                                        disabled={publicationBusyId === item.id}
                                                        onClick={() =>
                                                            handlePublicationDecision(item, "PUBLISHED")
                                                        }
                                                    >
                                                        Approve & Publish
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="secondary-button"
                                                        disabled={publicationBusyId === item.id}
                                                        onClick={() =>
                                                            handlePublicationDecision(item, "REVIEWED")
                                                        }
                                                    >
                                                        Mark Reviewed
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="reject-button"
                                                        disabled={publicationBusyId === item.id}
                                                        onClick={() =>
                                                            handlePublicationDecision(item, "REJECTED")
                                                        }
                                                    >
                                                        Reject Draft
                                                    </button>
                                                </div>
                                            </div>
                                        </article>
                                    ))}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 3: STATUS PROPOSALS QUEUE */}
                    {activeTab === "status" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">STATUS OVERSIGHT</p>
                                    <h3>Pending status proposals ({statusProposals.length})</h3>
                                </div>
                                <span className="panel-tag">
                                    {statusProposals.length} awaiting decision
                                </span>
                            </div>

                            <p className="panel-note">
                                Changes to a promise's implementation status (e.g. from In Progress to Completed)
                                must be backed by an approved evidence document and verified here.
                            </p>

                            {statusProposals.length === 0 ? (
                                <div className="empty-state">
                                    <h3>No status change proposals pending</h3>
                                    <p>Proposals submitted by researchers will appear here for verification.</p>
                                </div>
                            ) : (
                                <div className="review-list">
                                    {statusProposals.map((proposal, index) => (
                                        <article
                                            className="review-card"
                                            key={proposal.id}
                                            ref={index === 0 ? firstItemRef : null}
                                        >
                                            <div className="review-card-top">
                                                <span className="review-type">
                                                    PROPOSAL #{proposal.id}
                                                </span>
                                                <span className="status-chip pending">
                                                    PENDING
                                                </span>
                                            </div>

                                            <h3>Promise #{proposal.promiseId}: {proposal.promiseTitle}</h3>

                                            <div className="proposal-section">
                                                <h4>Status Change Details</h4>
                                                <p>
                                                    Current status: <strong>{proposal.currentStatus || "IN_PROGRESS"}</strong> →{" "}
                                                    Proposed status: <strong style={{ color: "#2563eb" }}>{proposal.proposedStatus}</strong>
                                                </p>
                                                {proposal.reason && (
                                                    <p style={{ marginTop: "6px" }}>
                                                        <strong>Researcher explanation:</strong> {proposal.reason}
                                                    </p>
                                                )}
                                                {proposal.evidenceTitle && (
                                                    <p style={{ marginTop: "6px", color: "#64748b" }}>
                                                        Backed by approved document: <em>{proposal.evidenceTitle}</em>
                                                    </p>
                                                )}
                                            </div>

                                            {/* Decision Form */}
                                            <div style={{ marginTop: "16px" }}>
                                                <textarea
                                                    rows="2"
                                                    placeholder="Optional comment on this status verification decision..."
                                                    value={commentDrafts[`stat_${proposal.id}`] || ""}
                                                    onChange={e =>
                                                        setCommentDrafts(prev => ({
                                                            ...prev,
                                                            [`stat_${proposal.id}`]: e.target.value
                                                        }))
                                                    }
                                                    style={{ width: "100%", marginBottom: "10px" }}
                                                />

                                                <div style={{ display: "flex", gap: "10px" }}>
                                                    <button
                                                        type="button"
                                                        className="approve-button"
                                                        disabled={statusBusyId === proposal.id}
                                                        onClick={() =>
                                                            handleStatusDecision(proposal, "APPROVED")
                                                        }
                                                    >
                                                        Approve Status Change
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="reject-button"
                                                        disabled={statusBusyId === proposal.id}
                                                        onClick={() =>
                                                            handleStatusDecision(proposal, "REJECTED")
                                                        }
                                                    >
                                                        Reject Proposal
                                                    </button>
                                                </div>
                                            </div>
                                        </article>
                                    ))}
                                </div>
                            )}
                        </section>
                    )}
                </main>
            </div>
        </section>
    );
}

export default ReviewerDashboard;
