import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../auth/useAuth";
import { getUsers, updateUser } from "../services/userApi";
import { getRecentAudit, getStatsOverview } from "../services/statsApi";

const ROLES = ["CITIZEN", "RESEARCHER", "REVIEWER", "ADMIN"];

function AdminPage() {

    const { user } = useAuth();

    const [users, setUsers] = useState([]);
    const [audit, setAudit] = useState([]);
    const [stats, setStats] = useState(null);

    const [drafts, setDrafts] = useState({});
    const [loading, setLoading] = useState(true);
    const [savingId, setSavingId] = useState(null);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const load = useCallback(async () => {

        try {

            setLoading(true);
            setError("");

            const [userData, auditData, statsData] = await Promise.all([
                getUsers(),
                getRecentAudit(50),
                getStatsOverview()
            ]);

            setUsers(userData);
            setAudit(auditData);
            setStats(statsData);

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await load();

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [load]);

    function draftFor(account) {
        return drafts[account.id] || {
            role: account.role,
            enabled: account.enabled
        };
    }

    function changeDraft(account, field, value) {

        setDrafts(previous => ({
            ...previous,
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

    async function save(account) {

        const draft = draftFor(account);

        try {

            setSavingId(account.id);
            setError("");
            setSuccess("");

            await updateUser(account.id, {
                role: draft.role,
                enabled: draft.enabled
            });

            setSuccess(`Saved changes for ${account.email}.`);

            setDrafts(previous => {
                const next = { ...previous };
                delete next[account.id];
                return next;
            });

            await load();

        } catch (err) {

            setError(err.message);

        } finally {

            setSavingId(null);

        }
    }

    function formatDateTime(value) {
        return value ? new Date(value).toLocaleString() : "";
    }

    return (

        <section className="admin-page">

            <div className="page-heading">

                <div>
                    <p className="eyebrow">PLATFORM ADMINISTRATION</p>
                    <h2>Administration</h2>
                    <p>
                        Manage accounts and roles, review the platform audit
                        trail, and keep party and election records accurate.
                    </p>
                </div>
            </div>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {stats && (
                <section className="platform-stats">
                    <div className="platform-stat">
                        <span>Accounts</span>
                        <strong>{stats.totalUsers}</strong>
                    </div>
                    <div className="platform-stat">
                        <span>Promises</span>
                        <strong>{stats.totalPromises}</strong>
                    </div>
                    <div className="platform-stat">
                        <span>Pending Evidence</span>
                        <strong>{stats.pendingEvidence}</strong>
                    </div>
                    <div className="platform-stat">
                        <span>Parties</span>
                        <strong>{stats.totalParties}</strong>
                    </div>
                    <div className="platform-stat">
                        <span>Elections</span>
                        <strong>{stats.totalElections}</strong>
                    </div>
                </section>
            )}

            <div className="admin-links">
                <Link className="secondary-button" to="/parties">
                    Manage parties
                </Link>
                <Link className="secondary-button" to="/elections">
                    Manage elections
                </Link>
                <Link className="secondary-button" to="/review">
                    Review queue
                </Link>
            </div>

            <section className="panel">

                <div className="panel-heading">
                    <div>
                        <p className="eyebrow">ACCESS CONTROL</p>
                        <h3>User accounts</h3>
                    </div>
                    <span className="panel-tag">
                        {users.length}{" "}
                        {users.length === 1 ? "account" : "accounts"}
                    </span>
                </div>

                <p className="panel-note">
                    New registrations start as citizens. You cannot change your
                    own role, and the last active administrator cannot be
                    demoted or disabled. Role changes and account disabling
                    take effect on the affected user's next request.
                </p>

                {loading ? (

                    <p className="info-message">Loading accounts...</p>

                ) : (

                    <div className="table-wrap">

                        <table className="admin-table">

                            <thead>
                                <tr>
                                    <th>Account</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Created</th>
                                    <th></th>
                                </tr>
                            </thead>

                            <tbody>

                                {users.map(account => {

                                    const draft = draftFor(account);
                                    const self = account.id === user?.id;

                                    return (
                                        <tr key={account.id}>

                                            <td>
                                                <strong>{account.name}</strong>
                                                <small>{account.email}</small>
                                                {self && (
                                                    <span className="self-tag">
                                                        you
                                                    </span>
                                                )}
                                            </td>

                                            <td>
                                                <select
                                                    value={draft.role}
                                                    disabled={self}
                                                    onChange={event =>
                                                        changeDraft(
                                                            account,
                                                            "role",
                                                            event.target.value
                                                        )
                                                    }
                                                >
                                                    {ROLES.map(role => (
                                                        <option
                                                            key={role}
                                                            value={role}
                                                        >
                                                            {role}
                                                        </option>
                                                    ))}
                                                </select>
                                            </td>

                                            <td>
                                                <label className="table-checkbox">
                                                    <input
                                                        type="checkbox"
                                                        checked={draft.enabled}
                                                        disabled={self}
                                                        onChange={event =>
                                                            changeDraft(
                                                                account,
                                                                "enabled",
                                                                event.target.checked
                                                            )
                                                        }
                                                    />
                                                    Enabled
                                                </label>
                                            </td>

                                            <td>
                                                {formatDateTime(account.createdAt)}
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className="secondary-button"
                                                    disabled={
                                                        !isDirty(account) ||
                                                        savingId === account.id
                                                    }
                                                    onClick={() => save(account)}
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
                )}

            </section>

            <section className="panel">

                <div className="panel-heading">
                    <div>
                        <p className="eyebrow">ACCOUNTABILITY</p>
                        <h3>Recent evidence activity</h3>
                    </div>
                    <span className="panel-tag">Newest first</span>
                </div>

                {loading ? (

                    <p className="info-message">Loading audit trail...</p>

                ) : audit.length === 0 ? (

                    <p className="empty-note">
                        No evidence activity has been recorded yet.
                    </p>

                ) : (

                    <ul className="audit-list admin-audit">

                        {audit.map(entry => (
                            <li key={entry.id}>

                                <div className="audit-line">
                                    <span
                                        className={
                                            "audit-action " +
                                            (entry.action || "").toLowerCase()
                                        }
                                    >
                                        {entry.action}
                                    </span>
                                    <span className="audit-date">
                                        {formatDateTime(entry.createdAt)}
                                    </span>
                                </div>

                                <p className="audit-actor">
                                    {entry.actorEmail || "system"}
                                    {entry.actorRole ? ` • ${entry.actorRole}` : ""}
                                </p>

                                <p className="audit-transition">
                                    Evidence #{entry.evidenceId}: {entry.evidenceTitle}
                                </p>

                                {entry.fromStatus && (
                                    <p className="audit-transition">
                                        {entry.fromStatus} → {entry.toStatus}
                                    </p>
                                )}

                                {entry.comment && (
                                    <p className="audit-comment">{entry.comment}</p>
                                )}

                            </li>
                        ))}

                    </ul>
                )}

            </section>

        </section>
    );
}

export default AdminPage;
