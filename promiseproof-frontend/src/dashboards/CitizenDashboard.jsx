import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import StatusBadge, { VerificationBadge } from "../components/StatusBadge";
import ApprovedEvidenceList from "../components/citizen/ApprovedEvidenceList";
import PromiseTrackingTimeline from "../components/citizen/PromiseTrackingTimeline";
import PublicPromiseList from "../components/citizen/PublicPromiseList";
import { statusDescription } from "../components/citizen/statusInfo";
import {
    downloadEvidence,
    getApprovedEvidenceForPromise,
    getMyEvidence,
    resubmitEvidence,
    uploadEvidence
} from "../services/evidenceApi";
import { getPromiseHistory } from "../services/api";
import {
    getNotifications,
    markAllNotificationsRead,
    markNotificationRead
} from "../services/notificationApi";
import { getPromises } from "../services/api";
import { getPublicStats } from "../services/statsApi";
import "../styles/citizen.css";

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "";
}

function formatRelative(value) {
    if (!value) return "";
    const then = new Date(value).getTime();
    const seconds = Math.max(0, Math.round((Date.now() - then) / 1000));
    if (seconds < 60) return "just now";
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.round(hours / 24);
    if (days < 7) return `${days}d ago`;
    return new Date(value).toLocaleDateString();
}

function statusClass(status) {
    return String(status || "").toLowerCase();
}

function displayCount(value) {
    return value === null || value === undefined ? "—" : value;
}

const DASHBOARD_TABS = [
    "browse", "submissions", "progress", "notifications",
    "submit", "tracking", "account"
];

function CitizenDashboard() {
    const { user } = useAuth();
    const [searchParams, setSearchParams] = useSearchParams();

    // The URL is the source of truth so top-nav deep links and sidebar clicks
    // always agree. Unknown tabs fall back to the published-promise listing.
    const requestedTab = searchParams.get("tab");
    const activeTab = DASHBOARD_TABS.includes(requestedTab)
        ? requestedTab
        : "browse";

    function selectTab(tab) {
        setSearchParams(tab === "browse" ? {} : { tab });
    }

    const [statusFilter, setStatusFilter] = useState("ALL");

    const [publicQuery, setPublicQuery] = useState("");
    const [publicStats, setPublicStats] = useState(null);

    const [selectedPromiseId, setSelectedPromiseId] = useState("");
    const [trackingEvidence, setTrackingEvidence] = useState([]);
    const [trackingHistory, setTrackingHistory] = useState([]);
    const [trackingLoading, setTrackingLoading] = useState(false);
    const [trackingError, setTrackingError] = useState("");

    const [myEvidence, setMyEvidence] = useState([]);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [availablePromises, setAvailablePromises] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Submit evidence form state
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState("");
    const [submitSuccess, setSubmitSuccess] = useState("");
    const [evidenceForm, setEvidenceForm] = useState({
        promiseId: "",
        title: "",
        evidenceType: "GOVERNMENT_ORDER",
        description: "",
        sourceUrl: "",
        issuingDepartment: "",
        documentReference: "",
        documentDate: "",
        claimScope: "",
        file: null
    });

    // Derive the tracked promise from the id and the already-loaded public
    // list. The first published promise is used by default on the tracking tab.
    const selectedPromise = selectedPromiseId
        ? availablePromises.find(
              item => String(item.id) === String(selectedPromiseId)
          ) || null
        : activeTab === "tracking" && availablePromises.length > 0
        ? availablePromises[0]
        : null;

    const loadData = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const [evidenceData, notifData, promisesData, statsData] =
                await Promise.all([
                    getMyEvidence(),
                    getNotifications().catch(() => ({ items: [], unreadCount: 0 })),
                    getPromises().catch(() => []),
                    getPublicStats().catch(() => null)
                ]);

            setMyEvidence(evidenceData || []);
            setNotifications(notifData.items || []);
            setUnreadCount(notifData.unreadCount || 0);
            setAvailablePromises(promisesData || []);
            setPublicStats(statsData);
        } catch (err) {
            setError("Could not load your citizen dashboard: " + err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function refresh() {
            await loadData();
            if (cancelled) return;
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadData]);

    // Load the public evidence and reviewed history for the tracked promise.
    useEffect(() => {
        if (!selectedPromise) return;

        let cancelled = false;

        async function loadTracking() {
            try {
                setTrackingLoading(true);
                setTrackingError("");

                const [evidenceData, historyData] = await Promise.all([
                    getApprovedEvidenceForPromise(selectedPromise.id)
                        .catch(() => []),
                    getPromiseHistory(selectedPromise.id).catch(() => [])
                ]);

                if (!cancelled) {
                    setTrackingEvidence(
                        Array.isArray(evidenceData) ? evidenceData : []
                    );
                    setTrackingHistory(
                        Array.isArray(historyData) ? historyData : []
                    );
                }
            } catch (err) {
                if (!cancelled) {
                    setTrackingError(`Could not load tracking data: ${err.message}`);
                }
            } finally {
                if (!cancelled) setTrackingLoading(false);
            }
        }

        loadTracking();
        return () => { cancelled = true; };
    }, [selectedPromise]);

    // Download evidence file
    async function handleDownload(item) {
        try {
            setError("");
            await downloadEvidence(item.id, item.originalFileName);
        } catch (err) {
            setError("Download failed: " + err.message);
        }
    }

    // Resubmit rejected evidence
    async function handleResubmit(item) {
        if (!window.confirm("Resubmit this rejected document for a fresh review?")) {
            return;
        }

        try {
            setError("");
            setSuccess("");
            await resubmitEvidence(item.id);
            setSuccess(`"${item.title}" resubmitted for review.`);
            setMyEvidence(await getMyEvidence());
        } catch (err) {
            setError("Could not resubmit: " + err.message);
        }
    }

    // Mark single notification read
    async function handleNotificationClick(item) {
        if (item.read) return;
        try {
            await markNotificationRead(item.id);
            setNotifications(prev =>
                prev.map(n => (n.id === item.id ? { ...n, read: true } : n))
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch {
            // Non-critical
        }
    }

    // Mark all notifications read
    async function handleMarkAllNotificationsRead() {
        try {
            await markAllNotificationsRead();
            setNotifications(prev => prev.map(n => ({ ...n, read: true })));
            setUnreadCount(0);
        } catch (err) {
            setError("Failed to mark notifications read: " + err.message);
        }
    }

    // Handle form change
    function handleFormChange(event) {
        const { name, value } = event.target;
        setEvidenceForm(prev => ({
            ...prev,
            [name]: value
        }));
    }

    function handleFileChange(event) {
        const file = event.target.files?.[0] || null;
        setEvidenceForm(prev => ({
            ...prev,
            file
        }));
    }

    // Submit new evidence
    async function handleFormSubmit(event) {
        event.preventDefault();

        if (!evidenceForm.promiseId) {
            setSubmitError("Please select which public promise this document supports.");
            return;
        }
        if (!evidenceForm.title.trim()) {
            setSubmitError("Please provide a title for the evidence document.");
            return;
        }
        if (!evidenceForm.file) {
            setSubmitError("Please attach the evidence document.");
            return;
        }

        try {
            setSubmitting(true);
            setSubmitError("");
            setSubmitSuccess("");

            await uploadEvidence(evidenceForm);

            setSubmitSuccess("Evidence submitted successfully! It is now in the review queue.");
            setEvidenceForm({
                promiseId: "",
                title: "",
                evidenceType: "GOVERNMENT_ORDER",
                description: "",
                sourceUrl: "",
                issuingDepartment: "",
                documentReference: "",
                documentDate: "",
                claimScope: "",
                file: null
            });

            // Reload evidence & switch to submissions after a short moment
            const updated = await getMyEvidence();
            setMyEvidence(updated);
            setTimeout(() => {
                selectTab("submissions");
                setSuccess("Your new document was added to the review queue.");
            }, 1000);
        } catch (err) {
            setSubmitError(err.message || "Failed to submit evidence.");
        } finally {
            setSubmitting(false);
        }
    }

    // Calculate metrics
    const counts = myEvidence.reduce(
        (acc, item) => {
            const key = statusClass(item.status);
            acc[key] = (acc[key] || 0) + 1;
            return acc;
        },
        { approved: 0, pending: 0, rejected: 0 }
    );

    const filteredEvidence = myEvidence.filter(item => {
        if (statusFilter === "ALL") return true;
        return item.status === statusFilter;
    });

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading your citizen dashboard...</p>
            </section>
        );
    }

    return (
        <section className="dashboard-page">
            {/* Welcome Header */}
            <div className="page-heading">
                <div>
                    <p className="eyebrow">CITIZEN WORKSPACE</p>
                    <h2>Welcome back, {user?.name || "Citizen"}</h2>
                    <p>
                        Track your supporting evidence documents, follow independent review progress,
                        and submit new public records to hold commitments accountable.
                    </p>

                    <form
                        className="citizen-search"
                        onSubmit={event => {
                            event.preventDefault();
                            selectTab("browse");
                        }}
                    >
                        <input
                            type="search"
                            value={publicQuery}
                            placeholder="Search published promises by title or keyword"
                            aria-label="Search published promises"
                            onChange={event => setPublicQuery(event.target.value)}
                        />
                        <button type="submit" className="primary-button">
                            Search public record
                        </button>
                    </form>
                </div>

                <div className="dashboard-identity">
                    <span className="nav-user-name">{user?.name}</span>
                    <span className="role-chip">CITIZEN</span>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {/* Meaningful Summary Cards (Citizen specific) */}
            <section className="summary-grid dashboard-summary">
                <div className="summary-card">
                    <span>My submissions</span>
                    <strong>{myEvidence.length}</strong>
                </div>

                <div className="summary-card">
                    <span>Approved evidence</span>
                    <strong style={{ color: "#166534" }}>{counts.approved}</strong>
                </div>

                <div className="summary-card">
                    <span>Under review</span>
                    <strong style={{ color: "#b45309" }}>{counts.pending}</strong>
                </div>

                <div className="summary-card">
                    <span>Unread alerts</span>
                    <strong style={{ color: unreadCount > 0 ? "#b91c1c" : "#1746a2" }}>
                        {unreadCount}
                    </strong>
                </div>
            </section>

            {/* Public figures: published promises and approved evidence only. */}
            <section className="summary-grid dashboard-summary public-summary">
                <div className="summary-card">
                    <span>Published promises</span>
                    <strong>
                        {displayCount(publicStats?.totalPromises)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Approved evidence</span>
                    <strong style={{ color: "#166534" }}>
                        {displayCount(publicStats?.approvedEvidence)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Promises with proof</span>
                    <strong>
                        {displayCount(publicStats?.promisesWithApprovedEvidence)}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>States covered</span>
                    <strong>
                        {displayCount(publicStats?.totalStates)}
                    </strong>
                </div>
            </section>

            {/* Main Action Banner */}
            <section className="dashboard-banner">
                <div>
                    <strong>Have a document that verifies or challenges a promise?</strong>
                    <p>
                        Attach official gazettes, government orders, budget reports or audit papers
                        to any public promise.
                    </p>
                </div>
                <button
                    type="button"
                    className="primary-button"
                    onClick={() => selectTab("submit")}
                >
                    + Submit evidence
                </button>
            </section>

            {/* Dashboard Layout: Sidebar + Main Content Area */}
            <div className="dashboard-layout">
                {/* Dedicated Sidebar Navigation */}
                <aside className="dashboard-sidebar">
                    <div className="dashboard-sidebar-title">Citizen Menu</div>
                    <ul className="dashboard-sidebar-nav">
                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "browse" ? "active" : ""}`}
                                onClick={() => selectTab("browse")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🏛️</span> Browse Promises
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "tracking" ? "active" : ""}`}
                                onClick={() => selectTab("tracking")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📈</span> Track Progress
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "account" ? "active" : ""}`}
                                onClick={() => selectTab("account")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>👤</span> My Account
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "submissions" ? "active" : ""}`}
                                onClick={() => selectTab("submissions")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📄</span> My Submissions
                                </span>
                                <span className="dashboard-nav-badge">{myEvidence.length}</span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "progress" ? "active" : ""}`}
                                onClick={() => selectTab("progress")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>⏱️</span> Review Progress
                                </span>
                                {counts.pending > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {counts.pending}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "notifications" ? "active" : ""}`}
                                onClick={() => selectTab("notifications")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🔔</span> Notifications
                                </span>
                                {unreadCount > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {unreadCount}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "submit" ? "active" : ""}`}
                                onClick={() => selectTab("submit")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📤</span> Submit Evidence
                                </span>
                            </button>
                        </li>
                    </ul>

                    <div className="dashboard-sidebar-divider" />

                    <div className="dashboard-sidebar-actions">
                        <Link to="/promises" className="dashboard-sidebar-link">
                            <span>🔍</span> Browse public promises
                        </Link>
                    </div>
                </aside>

                {/* Main Task Area */}
                <main className="dashboard-main-area">
                    {/* TAB: BROWSE PUBLISHED PROMISES */}
                    {activeTab === "browse" && (
                        <section className="panel">
                            <PublicPromiseList
                                query={publicQuery}
                                onQueryChange={setPublicQuery}
                            />
                        </section>
                    )}

                    {/* TAB: TRACK PROGRESS / REVIEWED EVIDENCE */}
                    {activeTab === "tracking" && (
                        <section className="panel tracking-panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">PUBLIC TRACKING</p>
                                    <h3>Promise tracking</h3>
                                </div>
                                {selectedPromise && (
                                    <Link
                                        to={`/promises/${selectedPromise.id}?tab=tracking`}
                                        className="secondary-button"
                                    >
                                        Full detail page
                                    </Link>
                                )}
                            </div>

                            <p className="panel-note">
                                Reviewed evidence and independently approved status
                                changes are shown below. Approving a document does
                                not by itself mark a promise complete.
                            </p>

                            {availablePromises.length === 0 ? (
                                <p className="empty-note">
                                    No published promises are available to track yet.
                                </p>
                            ) : (
                                <>
                                    <label className="tracking-promise-picker">
                                        <span>Choose a published promise</span>
                                        <select
                                            value={selectedPromise?.id || ""}
                                            onChange={event =>
                                                setSelectedPromiseId(
                                                    event.target.value
                                                )
                                            }
                                        >
                                            <option value="">
                                                Select a promise...
                                            </option>
                                            {availablePromises.map(item => (
                                                <option
                                                    key={item.id}
                                                    value={item.id}
                                                >
                                                    #{item.id} — {item.title}
                                                </option>
                                            ))}
                                        </select>
                                    </label>

                                    {selectedPromise && (
                                        <div className="tracking-selected-summary">
                                            <div>
                                                <strong>
                                                    {selectedPromise.title}
                                                </strong>
                                                <p className="public-note">
                                                    {statusDescription(
                                                        selectedPromise.status
                                                    )}
                                                </p>
                                            </div>
                                            <div className="public-card-badges">
                                                <StatusBadge
                                                    status={selectedPromise.status}
                                                />
                                                <VerificationBadge
                                                    verificationState={
                                                        selectedPromise.verificationState
                                                    }
                                                />
                                            </div>
                                        </div>
                                    )}

                                    {selectedPromise ? (
                                        <div className="tracking-public-sections">
                                            <ApprovedEvidenceList
                                                evidence={trackingEvidence}
                                                loading={trackingLoading}
                                                error={trackingError}
                                                compact
                                            />
                                            <PromiseTrackingTimeline
                                                promise={selectedPromise}
                                                history={trackingHistory}
                                                evidence={trackingEvidence}
                                                loading={trackingLoading}
                                                error={trackingError}
                                            />
                                        </div>
                                    ) : (
                                        <p className="empty-note">
                                            Select a promise to see its reviewed
                                            evidence and verified status history.
                                        </p>
                                    )}
                                </>
                            )}
                        </section>
                    )}

                    {/* TAB: MY ACCOUNT */}
                    {activeTab === "account" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">MY ACCOUNT</p>
                                    <h3>Your PromiseProof account</h3>
                                </div>
                                <span className="role-chip">
                                    {user?.role || "CITIZEN"}
                                </span>
                            </div>

                            <p className="panel-note">
                                Your account identity is private. Published promises
                                and approved evidence are shown without contributor
                                names or contact details.
                            </p>

                            <dl className="public-card-meta detailed account-details">
                                <div>
                                    <dt>Name</dt>
                                    <dd>{user?.name || "Citizen"}</dd>
                                </div>
                                <div>
                                    <dt>Email</dt>
                                    <dd>{user?.email || "—"}</dd>
                                </div>
                                <div>
                                    <dt>Role</dt>
                                    <dd>{user?.role || "CITIZEN"}</dd>
                                </div>
                                <div>
                                    <dt>My submissions</dt>
                                    <dd>{myEvidence.length}</dd>
                                </div>
                                <div>
                                    <dt>Approved evidence</dt>
                                    <dd>{counts.approved}</dd>
                                </div>
                                <div>
                                    <dt>Unread alerts</dt>
                                    <dd>{unreadCount}</dd>
                                </div>
                            </dl>

                            <div className="account-actions">
                                <Link
                                    to="/dashboard?tab=submissions"
                                    className="secondary-button"
                                >
                                    My evidence
                                </Link>
                                <Link
                                    to="/dashboard?tab=tracking"
                                    className="primary-button"
                                >
                                    Track promises
                                </Link>
                            </div>
                        </section>
                    )}

                    {/* TAB 1: SUBMIT EVIDENCE (MAIN ACTION) */}
                    {activeTab === "submit" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">CONTRIBUTE PROOF</p>
                                    <h3>Submit supporting evidence</h3>
                                </div>
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => selectTab("submissions")}
                                >
                                    Back to submissions
                                </button>
                            </div>

                            <p className="panel-note">
                                Select a public promise and attach official records, gazettes, budget allocations,
                                or verified audit reports. Your document will be reviewed independently before
                                appearing on the public record.
                            </p>

                            {submitError && <p className="error-message">{submitError}</p>}
                            {submitSuccess && <p className="success-message">{submitSuccess}</p>}

                            <form className="promise-form" onSubmit={handleFormSubmit}>
                                <div className="form-group">
                                    <label htmlFor="promiseId">Select promise *</label>
                                    <select
                                        id="promiseId"
                                        name="promiseId"
                                        value={evidenceForm.promiseId}
                                        onChange={handleFormChange}
                                        required
                                    >
                                        <option value="">-- Choose a promise to support --</option>
                                        {availablePromises.map(p => (
                                            <option key={p.id} value={p.id}>
                                                #{p.id} - {p.title} ({p.partyName || "Party"})
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label htmlFor="title">Document title *</label>
                                        <input
                                            id="title"
                                            name="title"
                                            type="text"
                                            value={evidenceForm.title}
                                            onChange={handleFormChange}
                                            placeholder="e.g. Ministry Gazette Notification #402"
                                            required
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="evidenceType">Evidence type *</label>
                                        <select
                                            id="evidenceType"
                                            name="evidenceType"
                                            value={evidenceForm.evidenceType}
                                            onChange={handleFormChange}
                                        >
                                            <option value="GOVERNMENT_ORDER">Government Order</option>
                                            <option value="OFFICIAL_REPORT">Official Report</option>
                                            <option value="BUDGET_DOCUMENT">Budget Document</option>
                                            <option value="AUDIT_REPORT">Audit Report</option>
                                            <option value="PARLIAMENTARY_RECORD">Parliamentary Record</option>
                                            <option value="OTHER">Other Official Source</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="form-row">
                                    <div className="form-group">
                                        <label htmlFor="issuingDepartment">Issuing department</label>
                                        <input
                                            id="issuingDepartment"
                                            name="issuingDepartment"
                                            type="text"
                                            value={evidenceForm.issuingDepartment}
                                            onChange={handleFormChange}
                                            placeholder="e.g. Ministry of Finance"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="documentReference">Document reference / number</label>
                                        <input
                                            id="documentReference"
                                            name="documentReference"
                                            type="text"
                                            value={evidenceForm.documentReference}
                                            onChange={handleFormChange}
                                            placeholder="e.g. F.No. 12/2026-P"
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="documentDate">Document date</label>
                                        <input
                                            id="documentDate"
                                            name="documentDate"
                                            type="date"
                                            value={evidenceForm.documentDate}
                                            onChange={handleFormChange}
                                        />
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label htmlFor="sourceUrl">Source link / Public URL</label>
                                    <input
                                        id="sourceUrl"
                                        name="sourceUrl"
                                        type="url"
                                        value={evidenceForm.sourceUrl}
                                        onChange={handleFormChange}
                                        placeholder="https://official-portal.gov/records/..."
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="description">Description & context</label>
                                    <textarea
                                        id="description"
                                        name="description"
                                        rows="3"
                                        value={evidenceForm.description}
                                        onChange={handleFormChange}
                                        placeholder="Explain what this document shows and how it relates to the commitment..."
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="claimScope">Claim scope</label>
                                    <input
                                        id="claimScope"
                                        name="claimScope"
                                        type="text"
                                        value={evidenceForm.claimScope}
                                        onChange={handleFormChange}
                                        placeholder="e.g. Sanctioned funds for Phase 1 construction"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="file">Attach document file *</label>
                                    <input
                                        id="file"
                                        name="file"
                                        type="file"
                                        accept=".pdf,.jpg,.jpeg,.png"
                                        onChange={handleFileChange}
                                        required
                                    />
                                    <small style={{ color: "#64748b", marginTop: "4px" }}>
                                        Attach a clear copy of the official document.
                                    </small>
                                </div>

                                <div className="form-actions">
                                    <button
                                        type="submit"
                                        className="primary-button"
                                        disabled={submitting}
                                    >
                                        {submitting ? "Uploading evidence..." : "Submit for review"}
                                    </button>
                                </div>
                            </form>
                        </section>
                    )}

                    {/* TAB 2: MY SUBMISSIONS */}
                    {activeTab === "submissions" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">MY SUBMISSIONS</p>
                                    <h3>Evidence I submitted</h3>
                                </div>
                                <span className="panel-tag">
                                    {filteredEvidence.length}{" "}
                                    {filteredEvidence.length === 1 ? "document" : "documents"}
                                </span>
                            </div>

                            <p className="panel-note">
                                Documents you upload are verified by reviewers before becoming public.
                                If a document is rejected, review comments explain what is missing so you can resubmit.
                            </p>

                            {/* Status Filter Bar */}
                            <div className="dashboard-filter-bar">
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "ALL" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("ALL")}
                                >
                                    All ({myEvidence.length})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "PENDING" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("PENDING")}
                                >
                                    Pending ({counts.pending})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "APPROVED" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("APPROVED")}
                                >
                                    Approved ({counts.approved})
                                </button>
                                <button
                                    type="button"
                                    className={`filter-pill ${statusFilter === "REJECTED" ? "active" : ""}`}
                                    onClick={() => setStatusFilter("REJECTED")}
                                >
                                    Rejected ({counts.rejected})
                                </button>
                            </div>

                            {filteredEvidence.length === 0 ? (
                                <p className="empty-note">
                                    {myEvidence.length === 0
                                        ? "You have not submitted any evidence yet. Click 'Submit evidence' to get started."
                                        : `No submissions with status ${statusFilter}.`}
                                </p>
                            ) : (
                                <div className="table-wrap">
                                    <table className="admin-table">
                                        <thead>
                                            <tr>
                                                <th>Document</th>
                                                <th>Promise</th>
                                                <th>Status</th>
                                                <th>Submitted</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filteredEvidence.map(item => (
                                                <tr key={item.id}>
                                                    <td>
                                                        <strong>{item.title}</strong>
                                                        <small>{item.evidenceType?.replaceAll("_", " ")}</small>
                                                        {item.originalFileName && (
                                                            <small style={{ color: "#94a3b8" }}>
                                                                File: {item.originalFileName}
                                                            </small>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <strong>{item.promiseTitle || `Promise #${item.promiseId}`}</strong>
                                                    </td>
                                                    <td>
                                                        <span className={`status-chip ${statusClass(item.status)}`}>
                                                            {item.status}
                                                        </span>
                                                        {item.reviewComment && (
                                                            <small style={{ color: "#991b1b" }}>
                                                                Note: {item.reviewComment}
                                                            </small>
                                                        )}
                                                    </td>
                                                    <td>{formatDateTime(item.uploadedAt)}</td>
                                                    <td>
                                                        <div style={{ display: "flex", gap: "6px" }}>
                                                            <button
                                                                type="button"
                                                                className="secondary-button"
                                                                onClick={() => handleDownload(item)}
                                                            >
                                                                Download
                                                            </button>

                                                            {item.status === "REJECTED" && (
                                                                <button
                                                                    type="button"
                                                                    className="primary-button"
                                                                    onClick={() => handleResubmit(item)}
                                                                >
                                                                    Resubmit
                                                                </button>
                                                            )}
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 3: REVIEW PROGRESS */}
                    {activeTab === "progress" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">QUALITY ASSURANCE</p>
                                    <h3>Submission review progress</h3>
                                </div>
                                <span className="panel-tag">
                                    {myEvidence.length} items tracked
                                </span>
                            </div>

                            <p className="panel-note">
                                Reviewers evaluate evidence across three criteria: document authenticity,
                                source traceability, and claim relevance. Track the stage of each submitted document below.
                            </p>

                            {myEvidence.length === 0 ? (
                                <p className="empty-note">
                                    No submissions to track yet. Once you submit evidence, its step-by-step review
                                    progress will appear here.
                                </p>
                            ) : (
                                <div>
                                    {myEvidence.map(item => {
                                        const isApproved = item.status === "APPROVED";
                                        const isRejected = item.status === "REJECTED";
                                        const isPending = item.status === "PENDING";

                                        return (
                                            <div className="progress-card" key={item.id}>
                                                <div className="progress-header">
                                                    <div>
                                                        <h4>{item.title}</h4>
                                                        <p>
                                                            Supporting: <strong>{item.promiseTitle || `Promise #${item.promiseId}`}</strong>
                                                        </p>
                                                    </div>
                                                    <span className={`status-chip ${statusClass(item.status)}`}>
                                                        {item.status}
                                                    </span>
                                                </div>

                                                {/* Visual Review Timeline */}
                                                <div className="progress-timeline">
                                                    <div className="timeline-step completed">
                                                        <div className="timeline-dot">✓</div>
                                                        <span className="timeline-label">Submitted</span>
                                                    </div>

                                                    <div className={`timeline-connector ${!isPending ? "completed" : ""}`} />

                                                    <div className={`timeline-step ${isPending ? "active" : "completed"}`}>
                                                        <div className="timeline-dot">{isPending ? "●" : "✓"}</div>
                                                        <span className="timeline-label">Under Review</span>
                                                    </div>

                                                    <div className={`timeline-connector ${isApproved ? "completed" : ""}`} />

                                                    <div
                                                        className={`timeline-step ${
                                                            isApproved
                                                                ? "completed"
                                                                : isRejected
                                                                ? "rejected"
                                                                : ""
                                                        }`}
                                                    >
                                                        <div className="timeline-dot">
                                                            {isApproved ? "✓" : isRejected ? "✕" : "○"}
                                                        </div>
                                                        <span className="timeline-label">
                                                            {isApproved ? "Approved" : isRejected ? "Rejected" : "Decision"}
                                                        </span>
                                                    </div>
                                                </div>

                                                {/* Reviewer Feedback / Comments */}
                                                {item.reviewComment && (
                                                    <div className="publication-comment">
                                                        <strong>Reviewer note:</strong> {item.reviewComment}
                                                    </div>
                                                )}

                                                {/* Recorded review dimensions */}
                                                {(item.documentReviewStatus ||
                                                    item.sourceReviewStatus ||
                                                    item.claimReviewStatus) && (

                                                    <div className="progress-review-details">
                                                        <div>
                                                            <strong>Document review:</strong>{" "}
                                                            {item.documentReviewStatus || "PENDING"}
                                                        </div>

                                                        <div>
                                                            <strong>Source review:</strong>{" "}
                                                            {item.sourceReviewStatus || "PENDING"}
                                                        </div>

                                                        <div>
                                                            <strong>Claim review:</strong>{" "}
                                                            {item.claimReviewStatus || "PENDING"}
                                                        </div>

                                                        {item.verificationNotes && (
                                                            <div>
                                                                <strong>Verification notes:</strong>{" "}
                                                                {item.verificationNotes}
                                                            </div>
                                                        )}
                                                    </div>
                                                )}

                                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "12px" }}>
                                                    <small style={{ color: "#64748b" }}>
                                                        Submitted on {formatDateTime(item.uploadedAt)}
                                                    </small>

                                                    <div style={{ display: "flex", gap: "8px" }}>
                                                        <button
                                                            type="button"
                                                            className="secondary-button"
                                                            onClick={() => handleDownload(item)}
                                                        >
                                                            Download
                                                        </button>
                                                        {isRejected && (
                                                            <button
                                                                type="button"
                                                                className="primary-button"
                                                                onClick={() => handleResubmit(item)}
                                                            >
                                                                Resubmit
                                                            </button>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 4: NOTIFICATIONS */}
                    {activeTab === "notifications" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">UPDATES & ALERTS</p>
                                    <h3>System notifications</h3>
                                </div>
                                {notifications.length > 0 && (
                                    <button
                                        type="button"
                                        className="secondary-button"
                                        onClick={handleMarkAllNotificationsRead}
                                        disabled={unreadCount === 0}
                                    >
                                        Mark all as read
                                    </button>
                                )}
                            </div>

                            <p className="panel-note">
                                Real-time notifications about reviewer decisions, publication approvals,
                                or comments on your submitted evidence.
                            </p>

                            {notifications.length === 0 ? (
                                <p className="empty-note">
                                    You have no notifications right now.
                                </p>
                            ) : (
                                <ul className="notification-list" style={{ maxHeight: "none", border: "1px solid #edf0f5", borderRadius: "10px" }}>
                                    {notifications.map(item => (
                                        <li key={item.id}>
                                            <button
                                                type="button"
                                                className={`notification-item ${!item.read ? "unread" : ""}`}
                                                onClick={() => handleNotificationClick(item)}
                                            >
                                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                                    <span className="notification-title">{item.title}</span>
                                                    <span className="notification-time">{formatRelative(item.createdAt)}</span>
                                                </div>
                                                <span className="notification-message">{item.message}</span>
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </section>
                    )}
                </main>
            </div>
        </section>
    );
}

export default CitizenDashboard;
