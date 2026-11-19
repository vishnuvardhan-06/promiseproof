import { useCallback, useEffect, useState } from "react";

import PromiseCard from "../components/PromiseCard";
import { useAuth } from "../auth/useAuth";
import { getParties } from "../services/partyApi";
import { getElections } from "../services/electionApi";
import { getStatsOverview } from "../services/statsApi";
import { searchPromises } from "../services/api";

const EMPTY_FILTERS = {
    q: "",
    status: "",
    partyId: "",
    electionId: "",
    state: "",
    withApprovedEvidence: false
};

function HomePage() {

    const { user } = useAuth();

    const canReviewEvidence = Boolean(
        user && (user.role === "REVIEWER" || user.role === "ADMIN")
    );

    const [promises, setPromises] = useState([]);
    const [parties, setParties] = useState([]);
    const [elections, setElections] = useState([]);
    const [stats, setStats] = useState(null);

    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [page, setPage] = useState(0);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [pageInfo, setPageInfo] = useState({
        totalElements: 0,
        totalPages: 0
    });

    const loadReferenceData = useCallback(async () => {

        try {
            const [partyData, electionData] = await Promise.all([
                getParties(),
                getElections()
            ]);

            setParties(partyData);
            setElections(electionData);

        } catch (err) {
            setError("Could not load party or election records: " + err.message);
        }
    }, []);

    const loadStats = useCallback(async () => {

        try {
            setStats(await getStatsOverview());
        } catch {
            // The promise list still works if statistics fail.
        }
    }, []);

    const loadPromises = useCallback(async (pageToLoad) => {

        try {

            setLoading(true);
            setError("");

            const data = await searchPromises({
                ...filters,
                page: pageToLoad,
                size: 9
            });

            setPromises(data.content || []);

            setPageInfo({
                totalElements: data.totalElements || 0,
                totalPages: data.totalPages || 0
            });

        } catch (err) {

            setError(err.message);

        } finally {

            setLoading(false);

        }
    }, [filters]);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await loadReferenceData();

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadReferenceData]);

    useEffect(() => {

        let cancelled = false;

        async function refreshStats() {

            await loadStats();

            if (cancelled) {
                return;
            }
        }

        refreshStats();

        return () => {
            cancelled = true;
        };
    }, [loadStats]);

    useEffect(() => {

        let cancelled = false;

        async function refreshPromises() {

            await loadPromises(page);

            if (cancelled) {
                return;
            }
        }

        const timer = setTimeout(() => refreshPromises(), 250);

        return () => {

            cancelled = true;
            clearTimeout(timer);
        };

    }, [loadPromises, page]);

    function handleFilterChange(event) {

        const { name, value, type, checked } = event.target;

        setPage(0);

        setFilters(previous => ({
            ...previous,
            [name]: type === "checkbox" ? checked : value
        }));
    }

    function resetFilters() {

        setPage(0);
        setFilters(EMPTY_FILTERS);
    }

    return (
        <>

            <section className="hero">

                <p className="eyebrow">
                    TRANSPARENCY • ACCOUNTABILITY • EVIDENCE
                </p>

                <h1>
                    Every Promise Deserves <span>Proof.</span>
                </h1>

                <p>
                    Track public commitments, monitor implementation
                    and explore evidence-backed progress.
                </p>

            </section>

            <section className="summary-grid">

                <div className="summary-card">
                    <span>Total Promises</span>
                    <strong>
                        {stats ? stats.totalPromises : pageInfo.totalElements}
                    </strong>
                </div>

                <div className="summary-card evidence-card">
                    <span>With Approved Evidence</span>
                    <strong>
                        {stats ? stats.promisesWithApprovedEvidence : 0}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Not yet assessed</span>
                    <strong>{stats?.statusCounts?.NOT_ASSESSED ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>Not Started</span>
                    <strong>{stats?.statusCounts?.NOT_STARTED ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>In Progress</span>
                    <strong>{stats?.statusCounts?.IN_PROGRESS ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>Completed</span>
                    <strong>{stats?.statusCounts?.COMPLETED ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>Partially Completed</span>
                    <strong>
                        {stats?.statusCounts?.PARTIALLY_COMPLETED ?? 0}
                    </strong>
                </div>

                <div className="summary-card">
                    <span>Delayed</span>
                    <strong>{stats?.statusCounts?.DELAYED ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>Cancelled</span>
                    <strong>{stats?.statusCounts?.CANCELLED ?? 0}</strong>
                </div>

                <div className="summary-card">
                    <span>Status Verified</span>
                    <strong>
                        {stats?.verificationStateCounts?.STATUS_VERIFIED ?? 0}
                    </strong>
                </div>

            </section>

            <section className="platform-stats">

                <div className="platform-stat">
                    <span>Evidence Documents</span>
                    <strong>{stats ? stats.totalEvidence : 0}</strong>
                </div>

                <div className="platform-stat">
                    <span>Approved Evidence</span>
                    <strong>{stats ? stats.approvedEvidence : 0}</strong>
                </div>

                <div className="platform-stat">
                    <span>Pending Review</span>
                    <strong>{stats ? stats.pendingEvidence : 0}</strong>
                </div>

                <div className="platform-stat">
                    <span>Rejected Evidence</span>
                    <strong>{stats ? stats.rejectedEvidence : 0}</strong>
                </div>

                <div className="platform-stat">
                    <span>Political Parties</span>
                    <strong>{stats ? stats.totalParties : parties.length}</strong>
                </div>

                <div className="platform-stat">
                    <span>Elections</span>
                    <strong>{stats ? stats.totalElections : elections.length}</strong>
                </div>

                <div className="platform-stat">
                    <span>States Covered</span>
                    <strong>{stats ? stats.totalStates : 0}</strong>
                </div>

            </section>

            {stats && (
                <section className="progress-section">

                    <div className="progress-column">
                        <h3>Progress by Party</h3>
                        {stats.partyProgress.length === 0 ? (
                            <p className="progress-empty">No party data yet.</p>
                        ) : (
                            stats.partyProgress.slice(0, 6).map(group => (
                                <div
                                    className="progress-row"
                                    key={"party-" + (group.id ?? group.name)}
                                >
                                    <div className="progress-row-top">
                                        <span>{group.name}</span>
                                        <strong>{group.total}</strong>
                                    </div>
                                    <div className="progress-bar">
                                        <span
                                            className="progress-fill completed"
                                            style={{
                                                width: `${group.total ? (group.statusCounts.COMPLETED || 0) * 100 / group.total : 0}%`
                                            }}
                                        />
                                    </div>
                                    <small>
                                        {group.statusCounts.COMPLETED || 0} completed •{" "}
                                        {group.statusCounts.IN_PROGRESS || 0} in progress
                                    </small>
                                </div>
                            ))
                        )}
                    </div>

                    <div className="progress-column">
                        <h3>Progress by Election</h3>
                        {stats.electionProgress.length === 0 ? (
                            <p className="progress-empty">No election data yet.</p>
                        ) : (
                            stats.electionProgress.slice(0, 6).map(group => (
                                <div
                                    className="progress-row"
                                    key={"election-" + (group.id ?? group.name)}
                                >
                                    <div className="progress-row-top">
                                        <span>{group.name}</span>
                                        <strong>{group.total}</strong>
                                    </div>
                                    <div className="progress-bar">
                                        <span
                                            className="progress-fill completed"
                                            style={{
                                                width: `${group.total ? (group.statusCounts.COMPLETED || 0) * 100 / group.total : 0}%`
                                            }}
                                        />
                                    </div>
                                    <small>
                                        {group.statusCounts.COMPLETED || 0} completed •{" "}
                                        {group.statusCounts.IN_PROGRESS || 0} in progress
                                    </small>
                                </div>
                            ))
                        )}
                    </div>

                    <div className="progress-column">
                        <h3>Progress by State</h3>
                        {stats.stateProgress.length === 0 ? (
                            <p className="progress-empty">No state data yet.</p>
                        ) : (
                            stats.stateProgress.slice(0, 6).map(group => (
                                <div
                                    className="progress-row"
                                    key={"state-" + group.name}
                                >
                                    <div className="progress-row-top">
                                        <span>{group.name}</span>
                                        <strong>{group.total}</strong>
                                    </div>
                                    <div className="progress-bar">
                                        <span
                                            className="progress-fill completed"
                                            style={{
                                                width: `${group.total ? (group.statusCounts.COMPLETED || 0) * 100 / group.total : 0}%`
                                            }}
                                        />
                                    </div>
                                    <small>
                                        {group.statusCounts.COMPLETED || 0} completed •{" "}
                                        {group.statusCounts.IN_PROGRESS || 0} in progress
                                    </small>
                                </div>
                            ))
                        )}
                    </div>

                </section>
            )}

            {error && (
                <p className="error-message">{error}</p>
            )}

            <section className="promises-section">

                <div className="section-heading">

                    <div>
                        <p className="eyebrow">PUBLIC RECORD</p>
                        <h2>Tracked Promises</h2>
                    </div>

                    <span className="promise-count">
                        {pageInfo.totalElements} records
                    </span>

                </div>

                <div className="filters-bar">

                    <div className="filter-field filter-search">
                        <label>Search</label>
                        <input
                            type="search"
                            name="q"
                            value={filters.q}
                            onChange={handleFilterChange}
                            placeholder="Title, description or party..."
                        />
                    </div>

                    <div className="filter-field">
                        <label>Status</label>
                        <select
                            name="status"
                            value={filters.status}
                            onChange={handleFilterChange}
                        >
                            <option value="">All statuses</option>
                            <option value="NOT_ASSESSED">Not yet assessed</option>
                            <option value="NOT_STARTED">Not Started</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="PARTIALLY_COMPLETED">Partially Completed</option>
                            <option value="DELAYED">Delayed</option>
                            <option value="CANCELLED">Cancelled</option>
                        </select>
                    </div>

                    <div className="filter-field">
                        <label>Party</label>
                        <select
                            name="partyId"
                            value={filters.partyId}
                            onChange={handleFilterChange}
                        >
                            <option value="">All parties</option>
                            {parties.map(party => (
                                <option key={party.id} value={party.id}>
                                    {party.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="filter-field">
                        <label>Election</label>
                        <select
                            name="electionId"
                            value={filters.electionId}
                            onChange={handleFilterChange}
                        >
                            <option value="">All elections</option>
                            {elections.map(election => (
                                <option key={election.id} value={election.id}>
                                    {election.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="filter-field">
                        <label>State</label>
                        <input
                            type="text"
                            name="state"
                            value={filters.state}
                            onChange={handleFilterChange}
                            placeholder="Any state"
                        />
                    </div>

                    <label className="filter-checkbox">
                        <input
                            type="checkbox"
                            name="withApprovedEvidence"
                            checked={filters.withApprovedEvidence}
                            onChange={handleFilterChange}
                        />
                        Approved evidence only
                    </label>

                    <button
                        type="button"
                        className="filter-reset"
                        onClick={resetFilters}
                    >
                        Reset
                    </button>

                </div>

                {loading ? (

                    <p className="info-message">
                        Loading promises...
                    </p>

                ) : promises.length === 0 ? (

                    <div className="empty-state">
                        <h3>No promises match these filters</h3>
                        <p>
                            Try a different search term or reset the filters.
                        </p>
                    </div>

                ) : (

                    <>
                        <div className="promise-grid">

                            {promises.map(promise => (

                                <PromiseCard
                                    key={promise.id}
                                    promise={promise}
                                    canEdit={false}
                                    canSubmitEvidence={Boolean(user)}
                                    canReviewEvidence={canReviewEvidence}
                                />

                            ))}

                        </div>

                        {pageInfo.totalPages > 1 && (
                            <div className="pagination">
                                <button
                                    type="button"
                                    disabled={page === 0}
                                    onClick={() =>
                                        setPage(current => Math.max(current - 1, 0))
                                    }
                                >
                                    Previous
                                </button>

                                <span>
                                    Page {page + 1} of {pageInfo.totalPages}
                                </span>

                                <button
                                    type="button"
                                    disabled={page + 1 >= pageInfo.totalPages}
                                    onClick={() => setPage(current => current + 1)}
                                >
                                    Next
                                </button>
                            </div>
                        )}
                    </>

                )}

            </section>

        </>
    );
}

export default HomePage;
