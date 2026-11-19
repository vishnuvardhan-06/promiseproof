import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getPublicPromises } from "../../services/api";
import { getElections } from "../../services/electionApi";
import { getParties } from "../../services/partyApi";
import PublicPromiseCard from "./PublicPromiseCard";

const STATUS_OPTIONS = [
    "NOT_ASSESSED",
    "NOT_STARTED",
    "IN_PROGRESS",
    "DELAYED",
    "COMPLETED",
    "PARTIALLY_COMPLETED",
    "DISPUTED"
];

const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0 };

/**
 * Section A of the citizen dashboard: published promises with search,
 * filtering, sorting and server-side pagination.
 */
function PublicPromiseList({ query = "", onQueryChange }) {
    const navigate = useNavigate();

    const [filters, setFilters] = useState({
        status: "",
        partyId: "",
        electionId: "",
        state: "",
        category: "",
        sort: "recent"
    });

    const [parties, setParties] = useState([]);
    const [elections, setElections] = useState([]);
    const [page, setPage] = useState(0);
    const [result, setResult] = useState(EMPTY_PAGE);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [debouncedQuery, setDebouncedQuery] = useState(query);

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedQuery(query), 250);
        return () => clearTimeout(timer);
    }, [query]);

    useEffect(() => {
        let cancelled = false;

        async function loadOptions() {
            try {
                const [partyData, electionData] = await Promise.all([
                    getParties().catch(() => []),
                    getElections().catch(() => [])
                ]);
                if (!cancelled) {
                    setParties(Array.isArray(partyData) ? partyData : []);
                    setElections(Array.isArray(electionData) ? electionData : []);
                }
            } catch {
                // Filter dropdowns are optional; the promise list still works.
            }
        }

        loadOptions();
        return () => { cancelled = true; };
    }, []);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError("");

                const data = await getPublicPromises({
                    q: debouncedQuery,
                    status: filters.status,
                    partyId: filters.partyId,
                    electionId: filters.electionId,
                    state: filters.state,
                    category: filters.category,
                    sort: filters.sort,
                    page,
                    size: 9
                });

                if (!cancelled) {
                    setResult(data || EMPTY_PAGE);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(`Could not load published promises: ${err.message}`);
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [debouncedQuery, filters.status, filters.partyId, filters.electionId,
        filters.state, filters.category, filters.sort, page]);

    function updateFilter(name, value) {
        setFilters(previous => ({ ...previous, [name]: value }));
        setPage(0);
    }

    function resetFilters() {
        setFilters({
            status: "",
            partyId: "",
            electionId: "",
            state: "",
            category: "",
            sort: "recent"
        });
        onQueryChange?.("");
        setPage(0);
    }

    const promises = Array.isArray(result.content) ? result.content : [];

    return (
        <div className="public-promise-list">
            <div className="panel-heading">
                <div>
                    <p className="eyebrow">PUBLIC RECORD</p>
                    <h3>Browse published promises</h3>
                </div>
                <span className="panel-tag">
                    {result.totalElements || 0} published
                </span>
            </div>

            <p className="panel-note">
                Only promises that passed independent publication review appear
                here. Drafts and unpublished records remain private.
            </p>

            <div className="public-filter-grid">
                <label>
                    <span>Search</span>
                    <input
                        type="search"
                        value={query}
                        placeholder="Title or keyword"
                        onChange={event => {
                            onQueryChange?.(event.target.value);
                            setPage(0);
                        }}
                    />
                </label>

                <label>
                    <span>Party</span>
                    <select
                        value={filters.partyId}
                        onChange={event => updateFilter("partyId", event.target.value)}
                    >
                        <option value="">All parties</option>
                        {parties.map(party => (
                            <option key={party.id} value={party.id}>
                                {party.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>Election</span>
                    <select
                        value={filters.electionId}
                        onChange={event => updateFilter("electionId", event.target.value)}
                    >
                        <option value="">All elections</option>
                        {elections.map(election => (
                            <option key={election.id} value={election.id}>
                                {election.name}
                                {election.year ? ` (${election.year})` : ""}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>State</span>
                    <input
                        type="text"
                        value={filters.state}
                        placeholder="Any state"
                        onChange={event => updateFilter("state", event.target.value)}
                    />
                </label>

                <label>
                    <span>Category</span>
                    <input
                        type="text"
                        value={filters.category}
                        placeholder="Any category"
                        onChange={event => updateFilter("category", event.target.value)}
                    />
                </label>

                <label>
                    <span>Implementation status</span>
                    <select
                        value={filters.status}
                        onChange={event => updateFilter("status", event.target.value)}
                    >
                        <option value="">All statuses</option>
                        {STATUS_OPTIONS.map(status => (
                            <option key={status} value={status}>
                                {status.replaceAll("_", " ")}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    <span>Sort by</span>
                    <select
                        value={filters.sort}
                        onChange={event => updateFilter("sort", event.target.value)}
                    >
                        <option value="recent">Recently updated</option>
                        <option value="oldest">Oldest updated</option>
                    </select>
                </label>

                <button
                    type="button"
                    className="secondary-button public-reset"
                    onClick={resetFilters}
                >
                    Reset filters
                </button>
            </div>

            {error && <p className="error-message">{error}</p>}

            {loading ? (
                <p className="info-message">Loading published promises...</p>
            ) : promises.length === 0 ? (
                <div className="public-empty-state">
                    <strong>No published promises match these filters.</strong>
                    <p>Try clearing a filter or searching for a different keyword.</p>
                </div>
            ) : (
                <div className="public-promise-grid">
                    {promises.map(promise => (
                        <PublicPromiseCard
                            key={promise.id}
                            promise={promise}
                            onViewDetails={item =>
                                navigate(`/promises/${item.id}`)}
                            onViewEvidence={item =>
                                navigate(`/promises/${item.id}?tab=evidence`)}
                            onTrackProgress={item =>
                                navigate(`/promises/${item.id}?tab=tracking`)}
                        />
                    ))}
                </div>
            )}

            {(result.totalPages || 0) > 1 && (
                <div className="public-pagination">
                    <button
                        type="button"
                        className="secondary-button"
                        disabled={page <= 0}
                        onClick={() => setPage(value => Math.max(0, value - 1))}
                    >
                        Previous
                    </button>
                    <span>
                        Page {page + 1} of {result.totalPages}
                    </span>
                    <button
                        type="button"
                        className="secondary-button"
                        disabled={page + 1 >= result.totalPages}
                        onClick={() => setPage(value => value + 1)}
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
}

export default PublicPromiseList;
