import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";

import StatusBadge, { VerificationBadge } from "../components/StatusBadge";
import ApprovedEvidenceList from "../components/citizen/ApprovedEvidenceList";
import PromiseTrackingTimeline from "../components/citizen/PromiseTrackingTimeline";
import {
    formatDate,
    statusDescription,
    statusLabel
} from "../components/citizen/statusInfo";
import { getPromise, getPromiseHistory } from "../services/api";
import { getApprovedEvidenceForPromise } from "../services/evidenceApi";
import "../styles/citizen.css";

const TABS = [
    { key: "details", label: "Promise Details" },
    { key: "sources", label: "Original Sources" },
    { key: "evidence", label: "Approved Evidence" },
    { key: "tracking", label: "Implementation Tracking" },
    { key: "history", label: "Status History" }
];

function PromiseDetails() {
    const { id } = useParams();
    const [searchParams, setSearchParams] = useSearchParams();

    const requestedTab = searchParams.get("tab");
    const activeTab = TABS.some(tab => tab.key === requestedTab)
        ? requestedTab
        : "details";

    const [promise, setPromise] = useState(null);
    const [evidence, setEvidence] = useState([]);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError("");

                const [promiseData, evidenceData, historyData] =
                    await Promise.all([
                        getPromise(id),
                        getApprovedEvidenceForPromise(id).catch(() => []),
                        getPromiseHistory(id).catch(() => [])
                    ]);

                if (!cancelled) {
                    setPromise(promiseData);
                    setEvidence(Array.isArray(evidenceData) ? evidenceData : []);
                    setHistory(Array.isArray(historyData) ? historyData : []);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(
                        `This promise is not publicly available: ${err.message}`
                    );
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [id]);

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading promise details...</p>
            </section>
        );
    }

    if (error || !promise) {
        return (
            <section className="dashboard-page">
                <div className="panel">
                    <h3>Promise unavailable</h3>
                    <p className="error-message">
                        {error || "This promise is not publicly available."}
                    </p>
                    <Link to="/promises" className="secondary-button">
                        Back to published promises
                    </Link>
                </div>
            </section>
        );
    }

    return (
        <section className="dashboard-page promise-detail-page">
            <Link to="/promises" className="dashboard-sidebar-link">
                ← Back to published promises
            </Link>

            <div className="page-heading promise-detail-heading">
                <div>
                    <p className="eyebrow">PROMISE #{promise.id}</p>
                    <h2>{promise.title}</h2>
                    <div className="public-card-badges">
                        <StatusBadge status={promise.status} />
                        <VerificationBadge
                            verificationState={promise.verificationState}
                        />
                    </div>
                </div>
            </div>

            <p className="public-note">
                {statusDescription(promise.status)}
            </p>

            <div className="promise-detail-tabs" role="tablist">
                {TABS.map(tab => (
                    <button
                        key={tab.key}
                        type="button"
                        role="tab"
                        aria-selected={activeTab === tab.key}
                        className={`promise-detail-tab ${
                            activeTab === tab.key ? "active" : ""
                        }`}
                        onClick={() => setSearchParams({ tab: tab.key })}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="panel promise-detail-panel">
                {activeTab === "details" && (
                    <div>
                        <p className="panel-note">{promise.description}</p>
                        <dl className="public-card-meta detailed">
                            <div>
                                <dt>Political party</dt>
                                <dd>{promise.partyName || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Election</dt>
                                <dd>
                                    {promise.electionName || "Not specified"}
                                    {promise.electionYear
                                        ? ` (${promise.electionYear})`
                                        : ""}
                                </dd>
                            </div>
                            <div>
                                <dt>State / scope</dt>
                                <dd>
                                    {promise.state ||
                                        promise.geographicScope ||
                                        "Not specified"}
                                </dd>
                            </div>
                            <div>
                                <dt>Category</dt>
                                <dd>{promise.category || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Target</dt>
                                <dd>{promise.target || "Not specified"}</dd>
                            </div>
                            <div>
                                <dt>Target completion date</dt>
                                <dd>
                                    {formatDate(promise.targetDate) ||
                                        "Not available"}
                                </dd>
                            </div>
                            <div>
                                <dt>Implementation status</dt>
                                <dd>{statusLabel(promise.status)}</dd>
                            </div>
                            <div>
                                <dt>Last updated</dt>
                                <dd>{formatDate(promise.updatedAt) || "—"}</dd>
                            </div>
                            <div>
                                <dt>Originally published</dt>
                                <dd>
                                    {formatDate(promise.publishedAt) || "—"}
                                </dd>
                            </div>
                            <div>
                                <dt>Approved evidence</dt>
                                <dd>
                                    {promise.approvedEvidenceCount || 0}
                                </dd>
                            </div>
                        </dl>
                    </div>
                )}

                {activeTab === "sources" && (
                    <div>
                        <h3>Original sources</h3>
                        <p className="panel-note">
                            Sources preserve the manifesto or document name,
                            location, exact quotation and publication date used
                            when the promise was recorded.
                        </p>
                        {(promise.sources || []).length === 0 ? (
                            <p className="empty-note">
                                No original sources are attached to this promise.
                            </p>
                        ) : (
                            <ol className="public-source-list">
                                {promise.sources.map(source => (
                                    <li key={source.id}>
                                        <strong>{source.manifestoName}</strong>
                                        <span>
                                            {(source.sourceType || "MANIFESTO")
                                                .replaceAll("_", " ")}
                                            {source.pageNumber
                                                ? ` · page ${source.pageNumber}`
                                                : ""}
                                            {source.publishedDate
                                                ? ` · ${formatDate(source.publishedDate)}`
                                                : ""}
                                        </span>
                                        <blockquote>
                                            {source.exactQuotation}
                                        </blockquote>
                                        {source.sourceUrl && (
                                            <a
                                                href={source.sourceUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                            >
                                                View original source
                                            </a>
                                        )}
                                    </li>
                                ))}
                            </ol>
                        )}
                    </div>
                )}

                {activeTab === "evidence" && (
                    <ApprovedEvidenceList evidence={evidence} />
                )}

                {activeTab === "tracking" && (
                    <PromiseTrackingTimeline
                        promise={promise}
                        history={history}
                        evidence={evidence}
                    />
                )}

                {activeTab === "history" && (
                    <div>
                        <h3>Status history</h3>
                        <p className="panel-note">
                            Only independently approved status changes are shown.
                            Internal corrections and unapproved proposals are
                            not public facts.
                        </p>
                        {history.length === 0 ? (
                            <p className="empty-note">
                                No independently reviewed status changes have
                                been recorded for this promise.
                            </p>
                        ) : (
                            <div className="table-wrap">
                                <table className="admin-table">
                                    <thead>
                                        <tr>
                                            <th>Previous status</th>
                                            <th>New status</th>
                                            <th>Date</th>
                                            <th>Review note</th>
                                            <th>Decision</th>
                                            <th>Supporting evidence</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {history.map(change => (
                                            <tr key={change.id}>
                                                <td>{statusLabel(change.fromStatus)}</td>
                                                <td>{statusLabel(change.toStatus)}</td>
                                                <td>{formatDate(change.changedAt)}</td>
                                                <td>{change.reason || "—"}</td>
                                                <td>{change.reviewerDecision || "—"}</td>
                                                <td>
                                                    {(change.supportingEvidence || [])
                                                        .map(item => item.title)
                                                        .join(", ") || "—"}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </section>
    );
}

export default PromiseDetails;
