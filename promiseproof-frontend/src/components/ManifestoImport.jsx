import { useState } from "react";

import { createPromise, extractPromises } from "../services/api";

function ManifestoImport({ parties, elections, onCreated }) {

    const [text, setText] = useState("");
    const [partyId, setPartyId] = useState("");
    const [electionId, setElectionId] = useState("");
    const [state, setState] = useState("");

    const [suggestions, setSuggestions] = useState([]);
    const [selected, setSelected] = useState([]);

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    async function handleExtract(event) {

        event.preventDefault();

        try {

            setLoading(true);
            setError("");
            setSuccess("");

            const results = await extractPromises(text);

            setSuggestions(results);
            setSelected(results.map(() => true));

            if (results.length === 0) {
                setError(
                    "No promise-like statements were found. Try pasting " +
                    "bulleted commitments or sentences such as " +
                    "\"We will ...\"."
                );
            }

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function toggle(index) {

        setSelected(previous =>
            previous.map((value, position) =>
                position === index ? !value : value
            )
        );
    }

    function selectAll(value) {
        setSelected(suggestions.map(() => value));
    }

    async function handleSave() {

        const chosen = suggestions.filter((ignored, index) => selected[index]);

        if (chosen.length === 0) {
            return;
        }

        try {

            setSaving(true);
            setError("");
            setSuccess("");

            let created = 0;

            for (const suggestion of chosen) {

                await createPromise({
                    title: suggestion.title,
                    description: suggestion.description,
                    partyId: partyId ? Number(partyId) : null,
                    electionId: electionId ? Number(electionId) : null,
                    state: state.trim()
                });

                created++;
            }

            setSuccess(
                `${created} promise${created === 1 ? "" : "s"} added. ` +
                "Review the wording on your dashboard."
            );

            setSuggestions([]);
            setSelected([]);
            setText("");

            if (onCreated) {
                onCreated();
            }

        } catch (err) {
            setError(err.message);
        } finally {
            setSaving(false);
        }
    }

    const selectedCount = selected.filter(Boolean).length;

    return (

        <section className="panel manifesto-panel">

            <div className="panel-heading">
                <div>
                    <p className="eyebrow">ASSISTED EXTRACTION</p>
                    <h3>Manifesto import</h3>
                </div>
                <span className="panel-tag">Human review required</span>
            </div>

            <p className="panel-note">
                Paste manifesto text you are allowed to use. The server
                proposes candidate sentences with a simple text heuristic —
                it is not an authoritative reading of the document. Edit or
                discard anything that misrepresents the source before saving.
            </p>

            <form onSubmit={handleExtract} className="manifesto-form">

                <textarea
                    value={text}
                    onChange={event => setText(event.target.value)}
                    rows={6}
                    maxLength={50000}
                    placeholder={"We will build 100 hospitals by 2030.\nProvide free bus travel for women."}
                    required
                />

                <div className="manifesto-options">

                    <div className="form-group">
                        <label>Party</label>
                        <select
                            value={partyId}
                            onChange={event => setPartyId(event.target.value)}
                        >
                            <option value="">Unattributed</option>
                            {parties.map(party => (
                                <option key={party.id} value={party.id}>
                                    {party.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Election</label>
                        <select
                            value={electionId}
                            onChange={event => setElectionId(event.target.value)}
                        >
                            <option value="">Not linked</option>
                            {elections.map(election => (
                                <option key={election.id} value={election.id}>
                                    {election.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>State</label>
                        <input
                            type="text"
                            value={state}
                            onChange={event => setState(event.target.value)}
                            placeholder="Optional"
                            maxLength={100}
                        />
                    </div>

                </div>

                <button
                    type="submit"
                    className="primary-button"
                    disabled={loading}
                >
                    {loading ? "Extracting..." : "Extract suggestions"}
                </button>

            </form>

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            {suggestions.length > 0 && (

                <div className="manifesto-results">

                    <div className="manifesto-results-heading">
                        <strong>
                            {selectedCount} of {suggestions.length} selected
                        </strong>
                        <div>
                            <button
                                type="button"
                                className="link-button"
                                onClick={() => selectAll(true)}
                            >
                                Select all
                            </button>
                            <button
                                type="button"
                                className="link-button"
                                onClick={() => selectAll(false)}
                            >
                                Clear
                            </button>
                        </div>
                    </div>

                    <ul className="suggestion-list">
                        {suggestions.map((suggestion, index) => (
                            <li key={suggestion.title + index}>
                                <label>
                                    <input
                                        type="checkbox"
                                        checked={Boolean(selected[index])}
                                        onChange={() => toggle(index)}
                                    />
                                    <span>
                                        <strong>{suggestion.title}</strong>
                                        {suggestion.description &&
                                            suggestion.description !== suggestion.title && (
                                                <small>{suggestion.description}</small>
                                            )}
                                    </span>
                                </label>
                            </li>
                        ))}
                    </ul>

                    <button
                        type="button"
                        className="primary-button"
                        onClick={handleSave}
                        disabled={saving || selectedCount === 0}
                    >
                        {saving
                            ? "Saving..."
                            : `Add ${selectedCount} selected promise${selectedCount === 1 ? "" : "s"}`}
                    </button>

                </div>
            )}

        </section>
    );
}

export default ManifestoImport;
