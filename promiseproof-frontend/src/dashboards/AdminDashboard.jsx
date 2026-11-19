import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import { getUsers, updateUser } from "../services/userApi";
import { getParties } from "../services/partyApi";
import { getElections } from "../services/electionApi";
import { getRecentAudit, getStatsOverview } from "../services/statsApi";

const ROLES = ["CITIZEN", "RESEARCHER", "REVIEWER", "ADMIN"];

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "";
}

function AdminDashboard() {
    const { user } = useAuth();

    // Active view: "users" | "records" | "audit" | "overview"
    const [activeTab, setActiveTab] = useState("users");
    const [userSearch, setUserSearch] = useState("");
    const [roleFilter, setRoleFilter] = useState("ALL");

    const [users, setUsers] = useState([]);
    const [parties, setParties] = useState([]);
    const [elections, setElections] = useState([]);
    const [audit, setAudit] = useState([]);
    const [stats, setStats] = useState(null);

    const [drafts, setDrafts] = useState({});
    const [savingId, setSavingId] = useState(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const loadAll = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const [userData, partyData, electionData, auditData, statsData] =
                await Promise.all([
                    getUsers().catch(() => []),
                    getParties().catch(() => []),
                    getElections().catch(() => []),
                    getRecentAudit(50).catch(() => []),
                    getStatsOverview().catch(() => null)
                ]);

            setUsers(userData || []);
            setParties(partyData || []);
            setElections(electionData || []);
            setAudit(auditData || []);
            setStats(statsData);
        } catch (err) {
            setError("Could not load administration workspace: " + err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function refresh() {
            await loadAll();
            if (cancelled) return;
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadAll]);

    // Draft management for user row edits
    function draftFor(account) {
        return (
            drafts[account.id] || {
                role: account.role,
                enabled: account.enabled
            }
        );
    }

    function changeDraft(account, field, value) {
        setDrafts(prev => ({
            ...prev,
            [account.id]: {
                ...draftFor(account),
                [field]: value
            }
        }));
    }

    function isDirty(account) {
        const draft = drafts[account.id];
        return Boolean(
            draft &&
                (draft.role !== account.role || draft.enabled !== account.enabled)
        );
    }

    async function handleSaveUser(account) {
        const draft = draftFor(account);

        try {
            setSavingId(account.id);
            setError("");
            setSuccess("");

            await updateUser(account.id, {
                role: draft.role,
                enabled: draft.enabled
            });

            setSuccess(`Saved changes for ${account.email} (${draft.role}).`);

            setDrafts(prev => {
                const next = { ...prev };
                delete next[account.id];
                return next;
            });

            const updatedUsers = await getUsers();
            setUsers(updatedUsers);
        } catch (err) {
            setError("Could not update user: " + err.message);
        } finally {
            setSavingId(null);
        }
    }

    const filteredUsers = users.filter(acc => {
        if (roleFilter !== "ALL" && acc.role !== roleFilter) return false;
        if (userSearch.trim()) {
            const q = userSearch.toLowerCase();
            return (
                acc.name?.toLowerCase().includes(q) ||
                acc.email?.toLowerCase().includes(q)
            );
        }
        return true;
    });

    if (loading) {
        return (
            <section className="dashboard-page">
                <p className="info-message">Loading platform administration...</p>
            </section>
        );
    }

    return (
        <section className="dashboard-page">
            {/* Welcome Header */}
            <div className="page-heading">
                <div>
                    <p className="eyebrow">ADMINISTRATOR WORKSPACE</p>
                    <h2>Platform Oversight & Administration</h2>
                    <p>
                        Manage accounts and assign security roles, oversee election and political party
                        reference data, inspect platform audit trails, and maintain system integrity.
                    </p>
                </div>

                <div className="dashboard-identity">
                    <span className="nav-user-name">{user?.name}</span>
                    <span className="role-chip">ADMIN</span>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {/* Meaningful Summary Cards (Admin specific) */}
            <section className="summary-grid dashboard-summary">
                <div className="summary-card">
                    <span>User accounts</span>
                    <strong>{users.length || stats?.totalUsers || "—"}</strong>
                </div>

                <div className="summary-card">
                    <span>Political parties</span>
                    <strong>{parties.length || stats?.totalParties || "—"}</strong>
                </div>

                <div className="summary-card">
                    <span>Active elections</span>
                    <strong>{elections.length || stats?.totalElections || "—"}</strong>
                </div>

                <div className="summary-card">
                    <span>Platform promises</span>
                    <strong>{stats?.totalPromises ?? "—"}</strong>
                </div>
            </section>

            {/* Main Action Banner */}
            <section className="dashboard-banner">
                <div>
                    <strong>Platform management & oversight</strong>
                    <p>
                        Keep user roles updated, register newly announced elections, or review evidence audit logs.
                    </p>
                </div>
                <div style={{ display: "flex", gap: "10px" }}>
                    <button
                        type="button"
                        className="primary-button"
                        onClick={() => setActiveTab("users")}
                    >
                        ⚙ Manage platform
                    </button>
                    <Link to="/review" className="secondary-button" style={{ textDecoration: "none" }}>
                        Open review queue
                    </Link>
                </div>
            </section>

            {/* Layout: Sidebar + Main Area */}
            <div className="dashboard-layout">
                {/* Admin Sidebar Menu */}
                <aside className="dashboard-sidebar">
                    <div className="dashboard-sidebar-title">Admin Controls</div>
                    <ul className="dashboard-sidebar-nav">
                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "users" ? "active" : ""}`}
                                onClick={() => setActiveTab("users")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>👥</span> User Accounts
                                </span>
                                <span className="dashboard-nav-badge">{users.length}</span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "records" ? "active" : ""}`}
                                onClick={() => setActiveTab("records")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>🏛️</span> Elections & Parties
                                </span>
                                <span className="dashboard-nav-badge">
                                    {parties.length + elections.length}
                                </span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "audit" ? "active" : ""}`}
                                onClick={() => setActiveTab("audit")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📜</span> Audit Activity
                                </span>
                                <span className="dashboard-nav-badge">{audit.length}</span>
                            </button>
                        </li>

                        <li>
                            <button
                                type="button"
                                className={`dashboard-nav-btn ${activeTab === "overview" ? "active" : ""}`}
                                onClick={() => setActiveTab("overview")}
                            >
                                <span className="dashboard-nav-label">
                                    <span>📊</span> Platform Overview
                                </span>
                            </button>
                        </li>
                    </ul>

                    <div className="dashboard-sidebar-divider" />

                    <div className="dashboard-sidebar-actions">
                        <Link to="/parties" className="dashboard-sidebar-link">
                            <span>🏛️</span> Manage parties page
                        </Link>
                        <Link to="/elections" className="dashboard-sidebar-link">
                            <span>🗳️</span> Manage elections page
                        </Link>
                        <Link to="/admin" className="dashboard-sidebar-link">
                            <span>🛠️</span> Full admin page
                        </Link>
                    </div>
                </aside>

                {/* Main Task Area */}
                <main className="dashboard-main-area">
                    {/* TAB 1: USER MANAGEMENT */}
                    {activeTab === "users" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">ACCESS CONTROL</p>
                                    <h3>User accounts ({users.length})</h3>
                                </div>
                                <span className="panel-tag">
                                    {filteredUsers.length} shown
                                </span>
                            </div>

                            <p className="panel-note">
                                Adjust permissions and assign roles. New registrations start as Citizens.
                                You cannot change your own role, and the last active administrator cannot be disabled.
                            </p>

                            {/* Search & Filter */}
                            <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", marginBottom: "16px" }}>
                                <input
                                    type="text"
                                    placeholder="Search users by name or email..."
                                    value={userSearch}
                                    onChange={e => setUserSearch(e.target.value)}
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
                                        className={`filter-pill ${roleFilter === "ALL" ? "active" : ""}`}
                                        onClick={() => setRoleFilter("ALL")}
                                    >
                                        All ({users.length})
                                    </button>
                                    {ROLES.map(r => (
                                        <button
                                            key={r}
                                            type="button"
                                            className={`filter-pill ${roleFilter === r ? "active" : ""}`}
                                            onClick={() => setRoleFilter(r)}
                                        >
                                            {r} ({users.filter(u => u.role === r).length})
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="table-wrap">
                                <table className="admin-table">
                                    <thead>
                                        <tr>
                                            <th>Account</th>
                                            <th>Role</th>
                                            <th>Status</th>
                                            <th>Created</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredUsers.map(account => {
                                            const draft = draftFor(account);
                                            const isSelf = account.id === user?.id;

                                            return (
                                                <tr key={account.id}>
                                                    <td>
                                                        <strong>{account.name}</strong>
                                                        <small>{account.email}</small>
                                                        {isSelf && (
                                                            <span className="self-tag">You</span>
                                                        )}
                                                    </td>

                                                    <td>
                                                        <select
                                                            value={draft.role}
                                                            disabled={isSelf}
                                                            onChange={e =>
                                                                changeDraft(account, "role", e.target.value)
                                                            }
                                                        >
                                                            {ROLES.map(r => (
                                                                <option key={r} value={r}>
                                                                    {r}
                                                                </option>
                                                            ))}
                                                        </select>
                                                    </td>

                                                    <td>
                                                        <label className="table-checkbox">
                                                            <input
                                                                type="checkbox"
                                                                checked={draft.enabled}
                                                                disabled={isSelf}
                                                                onChange={e =>
                                                                    changeDraft(
                                                                        account,
                                                                        "enabled",
                                                                        e.target.checked
                                                                    )
                                                                }
                                                            />
                                                            {draft.enabled ? "Enabled" : "Disabled"}
                                                        </label>
                                                    </td>

                                                    <td>{formatDateTime(account.createdAt)}</td>

                                                    <td>
                                                        <button
                                                            type="button"
                                                            className="secondary-button"
                                                            disabled={
                                                                !isDirty(account) ||
                                                                savingId === account.id
                                                            }
                                                            onClick={() => handleSaveUser(account)}
                                                        >
                                                            {savingId === account.id
                                                                ? "Saving..."
                                                                : "Save"}
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </section>
                    )}

                    {/* TAB 2: ELECTIONS & PARTIES */}
                    {activeTab === "records" && (
                        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
                            {/* Parties Section */}
                            <section className="panel">
                                <div className="panel-heading">
                                    <div>
                                        <p className="eyebrow">POLITICAL ENTITIES</p>
                                        <h3>Registered political parties ({parties.length})</h3>
                                    </div>
                                    <Link to="/parties" className="primary-button" style={{ textDecoration: "none" }}>
                                        + Manage parties
                                    </Link>
                                </div>

                                <p className="panel-note">
                                    Parties form the political attribution for all commitments. Add party manifestos,
                                    logos, and abbreviations.
                                </p>

                                {parties.length === 0 ? (
                                    <p className="empty-note">No parties registered yet.</p>
                                ) : (
                                    <div className="table-wrap">
                                        <table className="admin-table">
                                            <thead>
                                                <tr>
                                                    <th>Party Name</th>
                                                    <th>Abbreviation</th>
                                                    <th>Leader / Head</th>
                                                    <th>Promises</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {parties.map(p => (
                                                    <tr key={p.id}>
                                                        <td>
                                                            <strong>{p.name}</strong>
                                                            {p.foundedYear && (
                                                                <small>Est. {p.foundedYear}</small>
                                                            )}
                                                        </td>
                                                        <td>
                                                            <span className="panel-tag">{p.abbreviation || "—"}</span>
                                                        </td>
                                                        <td>{p.currentLeader || "Unspecified"}</td>
                                                        <td>{p.promiseCount ?? "—"}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>

                            {/* Elections Section */}
                            <section className="panel">
                                <div className="panel-heading">
                                    <div>
                                        <p className="eyebrow">ELECTORAL CONTEXT</p>
                                        <h3>Elections ({elections.length})</h3>
                                    </div>
                                    <Link to="/elections" className="primary-button" style={{ textDecoration: "none" }}>
                                        + Manage elections
                                    </Link>
                                </div>

                                <p className="panel-note">
                                    Every promise is rooted in a specific election campaign and manifesto.
                                </p>

                                {elections.length === 0 ? (
                                    <p className="empty-note">No elections registered yet.</p>
                                ) : (
                                    <div className="table-wrap">
                                        <table className="admin-table">
                                            <thead>
                                                <tr>
                                                    <th>Election Name</th>
                                                    <th>Year</th>
                                                    <th>Level</th>
                                                    <th>Polling Date</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {elections.map(el => (
                                                    <tr key={el.id}>
                                                        <td>
                                                            <strong>{el.name}</strong>
                                                            {el.state && <small>State: {el.state}</small>}
                                                        </td>
                                                        <td>{el.year || "—"}</td>
                                                        <td>
                                                            <span className="status-chip">{el.level || "NATIONAL"}</span>
                                                        </td>
                                                        <td>{el.pollingDate || "—"}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>
                        </div>
                    )}

                    {/* TAB 3: AUDIT ACTIVITY */}
                    {activeTab === "audit" && (
                        <section className="panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">PLATFORM AUDIT LOGS</p>
                                    <h3>Recent evidence activity</h3>
                                </div>
                                <span className="panel-tag">Newest first</span>
                            </div>

                            <p className="panel-note">
                                Complete log of reviewer decisions, document approvals, status verifications,
                                and publication updates for platform transparency.
                            </p>

                            {audit.length === 0 ? (
                                <p className="empty-note">No evidence audit activity recorded yet.</p>
                            ) : (
                                <div className="table-wrap">
                                    <table className="admin-table">
                                        <thead>
                                            <tr>
                                                <th>Action</th>
                                                <th>Item / Promise</th>
                                                <th>Actor</th>
                                                <th>Date</th>
                                                <th>Notes / Comments</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {audit.map(item => (
                                                <tr key={item.id}>
                                                    <td>
                                                        <span className="status-chip">
                                                            {item.action || "UPDATE"}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <strong>{item.evidenceTitle || `Evidence #${item.evidenceId}`}</strong>
                                                        {item.promiseTitle && (
                                                            <small>Promise: {item.promiseTitle}</small>
                                                        )}
                                                    </td>
                                                    <td>
                                                        <span>{item.performedByName || item.performedByEmail || "System"}</span>
                                                    </td>
                                                    <td>{formatDateTime(item.createdAt || item.timestamp)}</td>
                                                    <td>
                                                        {item.comment || item.verificationNotes || "—"}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </section>
                    )}

                    {/* TAB 4: PLATFORM OVERVIEW */}
                    {activeTab === "overview" && (
                        <div>
                            {/* Shortcuts Grid */}
                            <div className="admin-shortcuts-grid">
                                <Link to="/parties" className="admin-shortcut-card">
                                    <strong>Manage Parties</strong>
                                    <span>Create, edit, or archive registered political parties.</span>
                                </Link>

                                <Link to="/elections" className="admin-shortcut-card">
                                    <strong>Manage Elections</strong>
                                    <span>Configure election cycles, manifesto dates, and polling periods.</span>
                                </Link>

                                <Link to="/review" className="admin-shortcut-card">
                                    <strong>Review Queue</strong>
                                    <span>Process pending evidence submissions and status proposals.</span>
                                </Link>

                                <Link to="/admin" className="admin-shortcut-card">
                                    <strong>Administration Page</strong>
                                    <span>Open the dedicated administration console.</span>
                                </Link>
                            </div>

                            <section className="panel">
                                <div className="panel-heading">
                                    <div>
                                        <p className="eyebrow">METRICS</p>
                                        <h3>Platform health summary</h3>
                                    </div>
                                </div>

                                <div className="summary-grid" style={{ marginBottom: 0 }}>
                                    <div className="summary-card">
                                        <span>Total users</span>
                                        <strong>{users.length}</strong>
                                    </div>
                                    <div className="summary-card">
                                        <span>Pending evidence</span>
                                        <strong>{stats?.pendingEvidence ?? "—"}</strong>
                                    </div>
                                    <div className="summary-card">
                                        <span>Parties</span>
                                        <strong>{parties.length}</strong>
                                    </div>
                                    <div className="summary-card">
                                        <span>Elections</span>
                                        <strong>{elections.length}</strong>
                                    </div>
                                </div>
                            </section>
                        </div>
                    )}
                </main>
            </div>
        </section>
    );
}

export default AdminDashboard;
