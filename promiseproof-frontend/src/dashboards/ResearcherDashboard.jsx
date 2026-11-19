import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import PromiseCard from "../components/PromiseCard";
import PromiseForm from "../components/PromiseForm";
import ManifestoImport from "../components/ManifestoImport";
import PromiseSourceManager from "../components/PromiseSourceManager";
import { useAuth } from "../auth/useAuth";
import {
    createPromise,
    deletePromise,
    getMyPromises,
    submitForPublication,
    updatePromise
} from "../services/api";
import { getParties } from "../services/partyApi";
import { getElections } from "../services/electionApi";
import { getMyEvidence } from "../services/evidenceApi";
import { proposeStatus } from "../services/statusApi";

function ResearcherDashboard() {
    const { user } = useAuth();

    // Active view: "promises" | "drafts" | "sources" | "create" | "manifesto"
    const [activeTab, setActiveTab] = useState("promises");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [searchQuery, setSearchQuery] = useState("");

    const [myPromises, setMyPromises] = useState([]);
    const [myEvidence, setMyEvidence] = useState([]);
    const [parties, setParties] = useState([]);
    const [elections, setElections] = useState([]);

    const [editingPromise, setEditingPromise] = useState(null);

    // Status proposal state
    const [proposing, setProposing] = useState(null);
    const [proposalStatus, setProposalStatus] = useState("IN_PROGRESS");
    const [proposalReason, setProposalReason] = useState("");
    const [proposalBusy, setProposalBusy] = useState(false);
    const [proposalError, setProposalError] = useState("");
    const [proposalSuccess, setProposalSuccess] = useState("");

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const loadData = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const [promisesData, evidenceData, partyData, electionData] =
                await Promise.all([
                    getMyPromises().catch(() => []),
                    getMyEvidence().catch(() => []),
                    getParties().catch(() => []),
                    getElections().catch(() => [])
                ]);

            setMyPromises(promisesData || []);
            setMyEvidence(evidenceData || []);
            setParties(partyData || []);
            setElections(electionData || []);
        } catch (err) {
            setError("Could not load researcher dashboard: " + err.message);
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

    // Save promise (create or edit)
    async function handleSave(request) {
        try {
            setError("");
            setSuccess("");

            if (editingPromise) {
                await updatePromise(editingPromise.id, request);
                setSuccess(`Promise #${editingPromise.id} updated successfully.`);
                setEditingPromise(null);
            } else {
                await createPromise(request);
                setSuccess("New promise created and saved as a draft.");
            }

            const updated = await getMyPromises();
            setMyPromises(updated);
            setActiveTab("promises");
            return true;
        } catch (err) {
            setError(err.message || "Failed to save promise.");
            return false;
        }
    }

    // Delete promise
    async function handleDelete(id) {
        if (!window.confirm("Delete this promise? This removes it from the record.")) {
            return;
        }

        try {
            setError("");
            setSuccess("");
            await deletePromise(id);
            setSuccess("Promise deleted.");
            setMyPromises(await getMyPromises());
        } catch (err) {
            setError("Delete failed: " + err.message);
        }
    }

    // Submit draft promise for independent publication review
    async function handleSubmitForPublication(promise) {
        if (
            !window.confirm(
                "Submit this promise for independent publication review? A reviewer will " +
                "verify the original source quotation and exact wording before it becomes public."
            )
        ) {
            return;
        }

        try {
            setError("");
            setSuccess("");
            await submitForPublication(promise.id);
            setSuccess(
                `Promise #${promise.id} submitted for publication review. It stays out of ` +
                "the public record until approved by a reviewer."
            );
            setMyPromises(await getMyPromises());
        } catch (err) {
            setError("Publication submission failed: " + err.message);
        }
    }

    // Initiate status change proposal
    function startProposal(promise) {
        const proposable = [
            "NOT_STARTED",
            "IN_PROGRESS",
            "COMPLETED",
            "DELAYED",
            "PARTIALLY_COMPLETED",
            "CANCELLED"
        ];

        setProposing(promise);
        setProposalStatus(
            proposable.includes(promise.status) ? promise.status : "IN_PROGRESS"
        );
        setProposalReason("");
        setProposalError("");
        setProposalSuccess("");
    }

    // Submit status change proposal
    async function handleSubmitProposal(event) {
        event.preventDefault();
        if (!proposing) return;

        const approvedEvidence = myEvidence.find(
            item => item.promiseId === proposing.id && item.status === "APPROVED"
        );

        if (!approvedEvidence) {
            setProposalError(
                "This promise does not have an approved document yet. " +
                "Submit and get approved evidence before proposing a status change."
            );
            return;
        }

        try {
            setProposalBusy(true);
            setProposalError("");
            setProposalSuccess("");

            await proposeStatus(proposing.id, {
                proposedStatus: proposalStatus,
                reason: proposalReason,
                evidenceId: approvedEvidence.id
            });

            setProposalSuccess("Status proposal submitted for reviewer decision.");
            setSuccess("Status proposal submitted successfully.");
            setProposing(null);
            setMyPromises(await getMyPromises());
        } catch (err) {
            setProposalError(err.message || "Failed to submit proposal.");
        } finally {
            setProposalBusy(false);
        }
    }

    // Meaningful metrics calculation
    const drafts = myPromises.filter(p => (p.publicationStatus || "PUBLISHED") === "DRAFT");
    const inPublicationReview = myPromises.filter(
        p => (p.publicationStatus || "") === "SUBMITTED" || (p.publicationStatus || "") === "REVIEWED"
    );
    const withSources = myPromises.filter(p => (p.sources || []).length > 0);
    const sourceCompletenessPct = myPromises.length > 0
        ? Math.round((withSources.length / myPromises.length) * 100)
        : 100;

    const filteredPromises = myPromises.filter(p => {
        if (statusFilter !== "ALL") {
            if (statusFilter === "DRAFT" && p.publicationStatus !== "DRAFT") return false;
            if (statusFilter !== "DRAFT" && p.status !== statusFilter) return false;
        }
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            return (
                p.title?.toLowerCase().includes(query) ||
                p.description?.toLowerCase().includes(query) ||
                p.partyName?.toLowerCase().includes(query)
            );
        }
        return true;
    });

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading researcher workspace...</p>
            </section>
        );
    }

    return (
        <section className="dashboard-page">
            {/* Welcome Header */}
            <div className="page-heading">
                <div>
                    <p className="eyebrow">RESEARCHER WORKSPACE</p>
                    <h2>Welcome, {user?.name || "Researcher"}</h2>
                    <p>
                        Document political commitments from manifestos and gazettes, attach original
                        quotations and source citations, and submit drafts for publication review.
                    </p>
                </div>

                <div className="dashboard-identity">
                    <span className="nav-user-name">{user?.name}</span>
                    <span className="role-chip">RESEARCHER</span>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {/* Meaningful Summary Cards (Researcher specific) */}
            <section className="summary-grid dashboard-summary">
                <div className="summary-card">
                    <span>My promises</span>
                    <strong>{myPromises.length}</strong>
                </div>

                <div className="summary-card">
                    <span>Drafts</span>
                    <strong style={{ color: "#d97706" }}>{drafts.length}</strong>
                </div>

                <div className="summary-card">
                    <span>In publication review</span>
                    <strong style={{ color: "#2563eb" }}>{inPublicationReview.length}</strong>
                </div>

                <div className="summary-card">
                    <span>Source completeness</span>
                    <strong style={{ color: sourceCompletenessPct === 100 ? "#166534" : "#1746a2" }}>
                        {sourceCompletenessPct}%
                    </strong>
                </div>
            </section>

            {/* Main Action Banner */}
            <section className="dashboard-banner">
                <div>
                    <strong>Record a new commitment or extract from a manifesto</strong>
                    <p>
                        Every promise is attributed to your account and requires source verification
                        before publication.
                    </p>
                </div>
                <div style={{ display: "flex", gap: "10px" }}>
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() => {
                            setEditingPromise(null);
                            setActiveTab("create");
                        }}
                    >
                        + Create promise
                    </button>
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => setActiveTab("manifesto")}
                    >
                        Import manifesto
                    </button>
                </div>
            </section>

            {/* Layout: Sidebar + Main Area */}
            <div className="dashboard-layout">
                {/* Sidebar Menu */}
                <aside className="dashboard-sidebar">
                    <div className="dashboard-sidebar-title">Researcher Menu</div>
                    <ul className="dashboard-sidebar-nav">
                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "promises" ? "active" : ""}`}
                                onClick={() => setActiveTab("promises")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📋</span> My Promises
                                </span>
                                <span className="dashboard-nav-badge">{myPromises.length}</span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "drafts" ? "active" : ""}`}
                                onClick={() => setActiveTab("drafts")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🚀</span> Drafts & Publication
                                </span>
                                {(drafts.length + inPublicationReview.length) > 0 && (
                                    <span className="dashboard-nav-badge alert-badge">
                                        {drafts.length + inPublicationReview.length}
                                    </span>
                                )}
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "sources" ? "active" : ""}`}
                                onClick={() => setActiveTab("sources")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📚</span> Source Completeness
                                </span>
                                <span className="dashboard-nav-badge">
                                    {withSources.length}/{myPromises.length}
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "create" ? "active" : ""}`}
                                onClick={() => {
                                    setEditingPromise(null);
                                    setActiveTab("create");
                                }}
                            >
                                <span className="dashboard-nav-label">
                                    <span>➕</span> Create Promise
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "manifesto" ? "active" : ""}`}
                                onClick={() => setActiveTab("manifesto")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📥</span> Import Manifesto
                                </span>
                            </button>
                        </li>
                    </ul>

                    <div className="dashboard-sidebar-divider" />

                    <div className="dashboard-sidebar-actions">
                        <Link to="/promises" className="dashboard-sidebar-link">
                            <span>🔍</span> Browse public records
                        </Link>
                        <Link to="/parties" className="dashboard-sidebar-link">
                            <span>🏛️</span> Party records
                        </Link>
                        <Link to="/elections" className="dashboard-sidebar-link">
                            <span>🗳️</span> Election records
                        </Link>
                    </div>
                </aside>

                {/* Main Task Area */}
                <main className="dashboard-main-area">
                    {/* Status Proposal Modal/Panel */}
                    {proposing && (
                        <section className="panel" style={{ border: "2px solid #3b82f6" }}>
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">STATUS VERIFICATION</p>
                                    <h3>Propose status change for #{proposing.id}</h3>
                                </div>
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => setProposing(null)}
                                >
                                    Cancel proposal
                                </button>
                            </div>

                            <p className="panel-note">
                                Status changes require an approved evidence document.
                                The proposed status is reviewed before it can become public.
                            </p>

                            {proposalError && <p className="error-message">{proposalError}</p>}
                            {proposalSuccess && <p className="success-message">{proposalSuccess}</p>}

                            <form className="promise-form" onSubmit={handleSubmitProposal}>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label>Proposed status</label>
                                        <select
                                            value={proposalStatus}
                                            onChange={e => setProposalStatus(e.target.value)}
                                        >
                                            <option value="NOT_STARTED">Not Started</option>
                                            <option value="IN_PROGRESS">In Progress</option>
                                            <option value="COMPLETED">Completed</option>
                                            <option value="DELAYED">Delayed</option>
                                            <option value="PARTIALLY_COMPLETED">Partially Completed</option>
                                            <option value="CANCELLED">Cancelled</option>
                                        </select>
                                    </div>
                                </div>

                                <div className="form-group">
                                    <label>Reason for status change</label>
                                    <textarea
                                        rows="3"
                                        value={proposalReason}
                                        onChange={e => setProposalReason(e.target.value)}
                                        placeholder="Explain what has been accomplished or delayed, referencing verified documents..."
                                    />
                                </div>

                                <div className="form-actions">
                                    <button
                                        type="submit"
                                        className="primary-button"
                                        disabled={proposalBusy}
                                    >
                                        {proposalBusy ? "Submitting proposal..." : "Submit status proposal"}
                                    </button>
                                </div>
                            </form>
                        </section>
                    )}

                    {/* TAB 1: CREATE / EDIT PROMISE (MAIN ACTION) */}
                    {activeTab === "create" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">
                                        {editingPromise ? "EDIT RECORD" : "NEW RECORD"}
                                    </p>
                                    <h3>
                                        {editingPromise
                                            ? `Edit promise #${editingPromise.id}`
                                            : "Create a new promise"}
                                    </h3>
                                </div>
                                {editingPromise && (
                                    <button
                                        type="button"
                                        className="secondary-button"
                                        onClick={() => {
                                            setEditingPromise(null);
                                            setActiveTab("promises");
                                        }}
                                    >
                                        Cancel edit
                                    </button>
                                )}
                            </div>

                            <p className="panel-note">
                                Specify the verbatim commitment, owning political party, election context,
                                and geographic scope. New promises are created as drafts and can be submitted
                                for publication review.
                            </p>

                            <PromiseForm
                                key={editingPromise ? editingPromise.id : "new"}
                                onSave={handleSave}
                                editingPromise={editingPromise}
                                onCancel={() => {
                                    setEditingPromise(null);
                                    setActiveTab("promises");
                                }}
                                parties={parties}
                                elections={elections}
                            />
                        </section>
                    )}

                    {/* TAB 2: MY PROMISES */}
                    {activeTab === "promises" && (
                        <section className="dashboard-section">
                            <div className="panel" style={{ marginBottom: "20px" }}>
                                <div className="panel-heading">
                                    <div>
                                        <p className="eyebrow">MY RESEARCH RECORDS</p>
                                        <h3>Promises I created ({myPromises.length})</h3>
                                    </div>
                                    <button
                                        type="button"
                                        className="primary-button"
                                        onClick={() => {
                                            setEditingPromise(null);
                                            setActiveTab("create");
                                        }}
                                    >
                                        + Add promise
                                    </button>
                                </div>

                                {/* Filters and Search */}
                                <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
                                    <input
                                        type="text"
                                        placeholder="Search promises by title or description..."
                                        value={searchQuery}
                                        onChange={e => setSearchQuery(e.target.value)}
                                        style={{
                                            flex: "1",
                                            minWidth: "220px",
                                            padding: "8px 12px",
                                            borderRadius: "8px",
                                            border: "1px solid #d6dfec"
                                        }}
                                    />
                                    <div className="dashboard-filter-bar" style={{ margin: 0 }}>
                                        <button
                                            type="button"
                                            className={`filter-pill ${statusFilter === "ALL" ? "active" : ""}`}
                                            onClick={() => setStatusFilter("ALL")}
                                        >
                                            All
                                        </button>
                                        <button
                                            type="button"
                                            className={`filter-pill ${statusFilter === "DRAFT" ? "active" : ""}`}
                                            onClick={() => setStatusFilter("DRAFT")}
                                        >
                                            Drafts ({drafts.length})
                                        </button>
                                        <button
                                            type="button"
                                            className={`filter-pill ${statusFilter === "IN_PROGRESS" ? "active" : ""}`}
                                            onClick={() => setStatusFilter("IN_PROGRESS")}
                                        >
                                            In Progress
                                        </button>
                                        <button
                                            type="button"
                                            className={`filter-pill ${statusFilter === "COMPLETED" ? "active" : ""}`}
                                            onClick={() => setStatusFilter("COMPLETED")}
                                        >
                                            Completed
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {filteredPromises.length === 0 ? (
                                <div className="empty-state">
                                    <h3>No promises found</h3>
                                    <p>
                                        {myPromises.length === 0
                                            ? "You have not created any promises yet. Click 'Create promise' to start."
                                            : "No promises match your search or filter."}
                                    </p>
                                </div>
                            ) : (
                                <div className="promise-grid">
                                    {filteredPromises.map(promise => (
                                        <PromiseCard
                                            key={promise.id}
                                            promise={promise}
                                            canEdit
                                            canSubmitEvidence
                                            canReviewEvidence={false}
                                            canProposeStatus
                                            onProposeStatus={startProposal}
                                            onSubmitForPublication={handleSubmitForPublication}
                                            onDataChanged={async () => {
                                                setMyPromises(await getMyPromises());
                                            }}
                                            onEdit={p => {
                                                setEditingPromise(p);
                                                setActiveTab("create");
                                            }}
                                            onDelete={handleDelete}
                                        />
                                    ))}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 3: DRAFTS & PUBLICATION QUEUE */}
                    {activeTab === "drafts" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">PUBLICATION LIFECYCLE</p>
                                    <h3>Drafts & publication status</h3>
                                </div>
                                <span className="panel-tag">
                                    {drafts.length} drafts • {inPublicationReview.length} in review
                                </span>
                            </div>

                            <p className="panel-note">
                                Draft promises are private to you. Once you have attached the manifesto quote and
                                verified details, submit the draft for independent publication review.
                            </p>

                            {drafts.length === 0 && inPublicationReview.length === 0 ? (
                                <p className="empty-note">
                                    All your promises are currently published! Create a new promise draft anytime.
                                </p>
                            ) : (
                                <div className="table-wrap">
                                    <table className="admin-table">
                                        <thead>
                                            <tr>
                                                <th>Promise</th>
                                                <th>Party</th>
                                                <th>Publication Stage</th>
                                                <th>Sources Attached</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {[...drafts, ...inPublicationReview].map(promise => {
                                                const pubStatus = promise.publicationStatus || "DRAFT";
                                                const sourceCount = (promise.sources || []).length;

                                                return (
                                                    <tr key={promise.id}>
                                                        <td>
                                                            <strong>{promise.title}</strong>
                                                            <small>ID: #{promise.id}</small>
                                                            {promise.publicationComment && (
                                                                <small style={{ color: "#c2410c" }}>
                                                                    Review note: {promise.publicationComment}
                                                                </small>
                                                            )}
                                                        </td>
                                                        <td>{promise.partyName || "—"}</td>
                                                        <td>
                                                            <span
                                                                className={`publication-chip ${pubStatus.toLowerCase()}`}
                                                            >
                                                                {pubStatus}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <span
                                                                className={`source-status-badge ${
                                                                    sourceCount > 0 ? "has-source" : "needs-source"
                                                                }`}
                                                            >
                                                                {sourceCount > 0
                                                                    ? `${sourceCount} source(s)`
                                                                    : "No source attached"}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <div style={{ display: "flex", gap: "6px" }}>
                                                                {pubStatus === "DRAFT" && (
                                                                    <button
                                                                        type="button"
                                                                        className="primary-button"
                                                                        onClick={() =>
                                                                            handleSubmitForPublication(promise)
                                                                        }
                                                                    >
                                                                        Submit for review
                                                                    </button>
                                                                )}

                                                                <button
                                                                    type="button"
                                                                    className="secondary-button"
                                                                    onClick={() => {
                                                                        setEditingPromise(promise);
                                                                        setActiveTab("create");
                                                                    }}
                                                                >
                                                                    Edit
                                                                </button>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 4: SOURCE COMPLETENESS */}
                    {activeTab === "sources" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">QUALITY AUDIT</p>
                                    <h3>Source completeness audit</h3>
                                </div>
                                <span className="panel-tag">
                                    {sourceCompletenessPct}% documented
                                </span>
                            </div>

                            <p className="panel-note">
                                Every promise should cite an original manifesto, gazette, or election speech.
                                Review which of your promises have verified sources and add missing citations.
                            </p>

                            {/* Completeness Progress Bar */}
                            <div className="completeness-card">
                                <div className="completeness-header">
                                    <strong>Source coverage metric</strong>
                                    <span>
                                        {withSources.length} of {myPromises.length} promises documented ({sourceCompletenessPct}%)
                                    </span>
                                </div>
                                <div className="completeness-bar-bg">
                                    <div
                                        className="completeness-bar-fill"
                                        style={{ width: `${sourceCompletenessPct}%` }}
                                    />
                                </div>
                            </div>

                            {myPromises.length === 0 ? (
                                <p className="empty-note">No promises to audit yet.</p>
                            ) : (
                                <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                                    {myPromises.map(promise => {
                                        const sources = promise.sources || [];
                                        const hasSources = sources.length > 0;

                                        return (
                                            <div
                                                key={promise.id}
                                                style={{
                                                    background: "#ffffff",
                                                    border: `1px solid ${hasSources ? "#e2e8f0" : "#fca5a5"}`,
                                                    borderRadius: "12px",
                                                    padding: "18px"
                                                }}
                                            >
                                                <div
                                                    style={{
                                                        display: "flex",
                                                        justifyContent: "space-between",
                                                        alignItems: "flex-start",
                                                        gap: "12px",
                                                        marginBottom: "12px"
                                                    }}
                                                >
                                                    <div>
                                                        <h4 style={{ margin: "0 0 4px", fontSize: "16px" }}>
                                                            #{promise.id}: {promise.title}
                                                        </h4>
                                                        <small style={{ color: "#64748b" }}>
                                                            Party: {promise.partyName || "Unspecified"} • Election:{" "}
                                                            {promise.electionName || "Unspecified"}
                                                        </small>
                                                    </div>

                                                    <span
                                                        className={`source-status-badge ${
                                                            hasSources ? "has-source" : "needs-source"
                                                        }`}
                                                    >
                                                        {hasSources
                                                            ? `✓ ${sources.length} source(s)`
                                                            : "⚠️ Needs source"}
                                                    </span>
                                                </div>

                                                {/* Embedded Source Manager */}
                                                <PromiseSourceManager
                                                    promise={promise}
                                                    onChanged={async () => {
                                                        setMyPromises(await getMyPromises());
                                                    }}
                                                />
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 5: MANIFESTO IMPORT */}
                    {activeTab === "manifesto" && (
                        <ManifestoImport
                            parties={parties}
                            elections={elections}
                            onCreated={async () => {
                                setMyPromises(await getMyPromises());
                                setSuccess("Promises extracted and imported from manifesto text.");
                                setActiveTab("promises");
                            }}
                        />
                    )}
                </main>
            </div>
        </section>
    );
}

export default ResearcherDashboard;
