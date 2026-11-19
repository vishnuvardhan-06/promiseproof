import { useCallback, useEffect, useState } from "react";

import {
    getParties,
    createParty,
    updateParty,
    deleteParty
} from "../services/partyApi";

import "./PartyPage.css";

const emptyForm = {
    name: "",
    abbreviation: "",
    description: "",
    state: "",
    officialWebsite: ""
};

function PartyPage({ canManage = false }) {

    const [parties, setParties] = useState([]);

    const [formData, setFormData] = useState(emptyForm);

    const [editingId, setEditingId] = useState(null);

    const [search, setSearch] = useState("");

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [success, setSuccess] = useState("");

    const loadParties = useCallback(async () => {

        try {

            setLoading(true);
            setError("");

            const data = await getParties();

            setParties(data);

        } catch (err) {

            setError(err.message);

        } finally {

            setLoading(false);

        }
    }, []);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await loadParties();

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadParties]);

    // Update form fields
    function handleChange(event) {

        const { name, value } = event.target;

        setFormData(previous => ({
            ...previous,
            [name]: value
        }));
    }

    // Save or update party
    async function handleSubmit(event) {

        event.preventDefault();

        try {

            setError("");
            setSuccess("");

            if (editingId !== null) {

                await updateParty(editingId, formData);

                setSuccess("Party updated successfully!");

            } else {

                await createParty(formData);

                setSuccess("Party added successfully!");

            }

            setFormData(emptyForm);
            setEditingId(null);

            await loadParties();

        } catch (err) {

            setError(err.message);

        }
    }

    // Edit party
    function handleEdit(party) {

        setEditingId(party.id);

        setFormData({
            name: party.name || "",
            abbreviation: party.abbreviation || "",
            description: party.description || "",
            state: party.state || "",
            officialWebsite: party.officialWebsite || ""
        });

        window.scrollTo({
            top: document.getElementById("party-form").offsetTop - 30,
            behavior: "smooth"
        });
    }

    // Cancel editing
    function handleCancel() {

        setEditingId(null);
        setFormData(emptyForm);

    }

    // Delete party
    async function handleDelete(id) {

        const confirmed = window.confirm(
            "Are you sure you want to delete this party?"
        );

        if (!confirmed) {
            return;
        }

        try {

            setError("");
            setSuccess("");

            await deleteParty(id);

            setSuccess("Party deleted successfully!");

            await loadParties();

        } catch (err) {

            setError(err.message);

        }
    }

    // Search parties
    const filteredParties = parties.filter(party => {

        const searchText = search.toLowerCase();

        return (
            party.name.toLowerCase().includes(searchText) ||
            party.abbreviation.toLowerCase().includes(searchText) ||
            (party.state || "").toLowerCase().includes(searchText)
        );
    });

    return (

        <section className="party-page">

            <div className="party-heading">

                <div>
                    <p className="party-eyebrow">
                        PARTY DIRECTORY
                    </p>

                    <h2>Political Parties</h2>

                    <p>
                        Manage political party information and records.
                    </p>
                </div>

                <div className="party-total">
                    <strong>{parties.length}</strong>
                    <span>Registered Parties</span>
                </div>

            </div>

            <div
                className="party-layout"
                style={canManage ? undefined : { gridTemplateColumns: "1fr" }}
            >

                <form
                    id="party-form"
                    className="party-form"
                    onSubmit={handleSubmit}
                    style={canManage ? undefined : { display: "none" }}
                >

                    <h3>
                        {editingId !== null
                            ? "Edit Party"
                            : "Register a Party"}
                    </h3>

                    <div className="party-field">

                        <label>Party Name *</label>

                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Enter full party name"
                            maxLength={150}
                            required
                        />

                    </div>

                    <div className="party-field">

                        <label>Abbreviation *</label>

                        <input
                            type="text"
                            name="abbreviation"
                            value={formData.abbreviation}
                            onChange={handleChange}
                            placeholder="Example: EPP"
                            maxLength={30}
                            required
                        />

                    </div>

                    <div className="party-field">

                        <label>Description</label>

                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Enter party description"
                            rows="4"
                        />

                    </div>

                    <div className="party-field">

                        <label>State</label>

                        <input
                            type="text"
                            name="state"
                            value={formData.state}
                            onChange={handleChange}
                            placeholder="Enter state"
                        />

                    </div>

                    <div className="party-field">

                        <label>Official Website</label>

                        <input
                            type="url"
                            name="officialWebsite"
                            value={formData.officialWebsite}
                            onChange={handleChange}
                            placeholder="https://example.com"
                        />

                    </div>

                    <div className="party-form-actions">

                        <button type="submit">
                            {editingId !== null
                                ? "Update Party"
                                : "Register Party"}
                        </button>

                        {editingId !== null && (

                            <button
                                type="button"
                                className="party-cancel"
                                onClick={handleCancel}
                            >
                                Cancel
                            </button>

                        )}

                    </div>

                </form>

                <div className="party-records">

                    <div className="party-search">

                        <input
                            type="search"
                            placeholder="Search by name, abbreviation or state..."
                            value={search}
                            onChange={event => setSearch(event.target.value)}
                        />

                    </div>

                    {error && (
                        <p className="party-error">{error}</p>
                    )}

                    {success && (
                        <p className="party-success">{success}</p>
                    )}

                    {loading ? (

                        <p>Loading parties...</p>

                    ) : filteredParties.length === 0 ? (

                        <div className="party-empty">
                            <h3>No parties found</h3>
                            <p>
                                {canManage
                                    ? "Add a party or change your search."
                                    : "No political parties have been registered yet."}
                            </p>
                        </div>

                    ) : (

                        <div className="party-list">

                            {filteredParties.map(party => (

                                <article
                                    className="party-card"
                                    key={party.id}
                                >

                                    <div className="party-avatar">
                                        {party.abbreviation.slice(0, 2).toUpperCase()}
                                    </div>

                                    <div className="party-card-content">

                                        <h3>{party.name}</h3>

                                        <span className="party-abbreviation">
                                            {party.abbreviation}
                                        </span>

                                        <p>
                                            {party.description || "No description provided."}
                                        </p>

                                        <small>
                                            {party.state || "State not specified"}
                                        </small>

                                        {party.officialWebsite && (

                                            <a
                                                href={party.officialWebsite}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                            >
                                                Official website
                                            </a>

                                        )}

                                        {canManage && (
                                            <div className="party-card-actions">

                                                <button
                                                    type="button"
                                                    onClick={() => handleEdit(party)}
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    type="button"
                                                    onClick={() => handleDelete(party.id)}
                                                >
                                                    Delete
                                                </button>

                                            </div>
                                        )}

                                    </div>

                                </article>

                            ))}

                        </div>

                    )}

                </div>

            </div>

        </section>
    );
}

export default PartyPage;