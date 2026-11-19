
import { useCallback, useEffect, useState } from "react";

import {
    getElections,
    createElection,
    updateElection,
    deleteElection
} from "../services/electionApi";

import "./ElectionPage.css";

const emptyForm = {
    name: "",
    electionType: "STATE_ASSEMBLY",
    state: "",
    year: "",
    electionDate: "",
    description: ""
};

function ElectionPage({ canManage = false }) {

    const [elections, setElections] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [search, setSearch] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const loadElections = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const data = await getElections();
            setElections(data);

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await loadElections();

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadElections]);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm(previous => ({
            ...previous,
            [name]: value
        }));
    }

    async function handleSubmit(event) {
        event.preventDefault();

        setMessage("");
        setError("");

        const electionData = {
            ...form,
            year: Number(form.year),
            electionDate: form.electionDate || null
        };

        try {
            if (editingId !== null) {
                await updateElection(editingId, electionData);
                setMessage("Election updated successfully!");
            } else {
                await createElection(electionData);
                setMessage("Election registered successfully!");
            }

            setForm(emptyForm);
            setEditingId(null);

            await loadElections();

        } catch (err) {
            setError(err.message);
        }
    }

    function handleEdit(election) {
        setEditingId(election.id);

        setForm({
            name: election.name,
            electionType: election.electionType,
            state: election.state,
            year: String(election.year),
            electionDate: election.electionDate || "",
            description: election.description || ""
        });

        document.getElementById("election-form")
            ?.scrollIntoView({ behavior: "smooth" });
    }

    async function handleDelete(id) {
        const confirmed = window.confirm(
            "Are you sure you want to delete this election?"
        );

        if (!confirmed) return;

        try {
            setError("");
            setMessage("");

            await deleteElection(id);

            setMessage("Election deleted successfully!");

            if (editingId === id) {
                setEditingId(null);
                setForm(emptyForm);
            }

            await loadElections();

        } catch (err) {
            setError(err.message);
        }
    }

    function cancelEdit() {
        setEditingId(null);
        setForm(emptyForm);
        setMessage("");
        setError("");
    }

    const filteredElections = elections.filter(election => {
        const searchText = search.toLowerCase();

        return (
            election.name.toLowerCase().includes(searchText) ||
            election.state.toLowerCase().includes(searchText) ||
            String(election.year).includes(searchText) ||
            election.electionType.toLowerCase().includes(searchText)
        );
    });

    return (
        <section className="election-section">

            <div className="election-heading">
                <div>
                    <p className="election-eyebrow">
                        ELECTION DATABASE
                    </p>

                    <h2>Election Management</h2>

                    <p>
                        Organize elections and track public commitments
                        associated with each election.
                    </p>
                </div>

                <div className="election-count">
                    <span>Total Elections</span>
                    <strong>{elections.length}</strong>
                </div>
            </div>

            <div
                className="election-layout"
                style={canManage ? undefined : { gridTemplateColumns: "1fr" }}
            >

                <div
                    className="election-form-card"
                    id="election-form"
                    style={canManage ? undefined : { display: "none" }}
                >

                    <h3>
                        {editingId !== null
                            ? "Update Election"
                            : "Register New Election"}
                    </h3>

                    {message && (
                        <div className="election-success">
                            {message}
                        </div>
                    )}

                    {error && (
                        <div className="election-error">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>

                        <label>Election Name</label>
                        <input
                            type="text"
                            name="name"
                            placeholder="Enter election name"
                            value={form.name}
                            onChange={handleChange}
                            required
                        />

                        <label>Election Type</label>
                        <select
                            name="electionType"
                            value={form.electionType}
                            onChange={handleChange}
                            required
                        >
                            <option value="LOK_SABHA">
                                Lok Sabha
                            </option>

                            <option value="STATE_ASSEMBLY">
                                State Assembly
                            </option>

                            <option value="LOCAL_BODY">
                                Local Body
                            </option>
                        </select>

                        <label>State / Region</label>
                        <input
                            type="text"
                            name="state"
                            placeholder="Example: Andhra Pradesh"
                            value={form.state}
                            onChange={handleChange}
                            required
                        />

                        <label>Election Year</label>
                        <input
                            type="number"
                            name="year"
                            placeholder="Example: 2024"
                            min="1900"
                            max="2100"
                            value={form.year}
                            onChange={handleChange}
                            required
                        />

                        <label>Election Date (Optional)</label>
                        <input
                            type="date"
                            name="electionDate"
                            value={form.electionDate}
                            onChange={handleChange}
                        />

                        <label>Description</label>
                        <textarea
                            name="description"
                            placeholder="Enter election details"
                            value={form.description}
                            onChange={handleChange}
                            rows="4"
                        />

                        <button type="submit">
                            {editingId !== null
                                ? "Update Election"
                                : "Register Election"}
                        </button>

                        {editingId !== null && (
                            <button
                                type="button"
                                className="election-cancel"
                                onClick={cancelEdit}
                            >
                                Cancel Edit
                            </button>
                        )}

                    </form>
                </div>

                <div className="election-list-card">

                    <div className="election-list-heading">
                        <h3>Registered Elections</h3>

                        <span>{filteredElections.length} found</span>
                    </div>

                    <input
                        className="election-search"
                        type="text"
                        placeholder="Search by election, state, year or type..."
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                    />

                    {loading ? (
                        <p className="election-empty">
                            Loading elections...
                        </p>
                    ) : filteredElections.length === 0 ? (
                        <p className="election-empty">
                            {canManage
                                ? "No elections found. Register your first election."
                                : "No elections have been registered yet."}
                        </p>
                    ) : (

                        <div className="election-list">

                            {filteredElections.map(election => (

                                <article
                                    className="election-card"
                                    key={election.id}
                                >

                                    <div className="election-card-top">
                                        <div className="election-avatar">
                                            {election.year}
                                        </div>

                                        <span className="election-type">
                                            {election.electionType.replaceAll(
                                                "_", " "
                                            )}
                                        </span>
                                    </div>

                                    <h4>{election.name}</h4>

                                    <p className="election-state">
                                        {election.state}
                                    </p>

                                    {election.electionDate && (
                                        <p className="election-date">
                                            Election date: {election.electionDate}
                                        </p>
                                    )}

                                    {election.description && (
                                        <p className="election-description">
                                            {election.description}
                                        </p>
                                    )}

                                    {canManage && (
                                        <div className="election-actions">

                                            <button
                                                type="button"
                                                onClick={() => handleEdit(election)}
                                            >
                                                Edit
                                            </button>

                                            <button
                                                type="button"
                                                className="election-delete"
                                                onClick={() => handleDelete(election.id)}
                                            >
                                                Delete
                                            </button>

                                        </div>
                                    )}

                                </article>

                            ))}

                        </div>
                    )}

                </div>
            </div>
        </section>
    );
}

export default ElectionPage;