
import { useState } from "react";

import {
    createPromiseSource,
    deletePromiseSource
} from "../services/api";

const SOURCE_TYPES = [
    "MANIFESTO",
    "PARTY_DOCUMENT",
    "GOVERNMENT_ANNOUNCEMENT",
    "IMPLEMENTATION_REPORT",
    "OTHER"
];

const emptySource = {
    manifestoName: "",
    sourceUrl: "",
    pageNumber: "",
    exactQuotation: "",
    sourceType: "MANIFESTO",
    publishedDate: ""
};

function PromiseSourceManager({ promise, onChanged }) {

    const sources = promise.sources || [];

    const [form, setForm] = useState(emptySource);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    function handleChange(event) {

        const { name, value } = event.target;

        setForm(previous => ({
            ...previous,
            [name]: value
        }));
    }

    async function handleSubmit(event) {

        event.preventDefault();

        try {

            setBusy(true);
            setError("");
            setSuccess("");

            await createPromiseSource(promise.id, {
                ...form,
                pageNumber: form.pageNumber
                    ? Number(form.pageNumber)
                    : null,
                publishedDate: form.publishedDate || null
            });

            setForm(emptySource);
            setSuccess("Source added. The promise is back in draft until it is reviewed again.");

            if (onChanged) {
                await onChanged();
            }

        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    async function handleDelete(sourceId) {

        if (!window.confirm("Remove this original source?")) {
            return;
        }

        try {

            setBusy(true);
            setError("");
            setSuccess("");

            await deletePromiseSource(promise.id, sourceId);

            setSuccess("Source removed.");

            if (onChanged) {
                await onChanged();
            }

        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <details className="source-manager">

            <summary>
                Original sources ({sources.length})
            </summary>

            {sources.length === 0 ? (
                <p className="source-empty">
                    Every published promise needs at least one original
                    source with the exact wording.
                </p>
            ) : (
                <ul className="source-list">
                    {sources.map(source => (
                        <li key={source.id}>
                            <div className="source-line">
                                <strong>{source.manifestoName}</strong>
                                <span>
                                    {String(source.sourceType || "MANIFESTO")
                                        .replaceAll("_", " ")}
                                    {source.pageNumber
                                        ? ` • page ${source.pageNumber}`
                                        : ""}
                                </span>
                            </div>

                            <blockquote>
                                {source.exactQuotation}
                            </blockquote>

                            {source.sourceUrl && (
                                <a
                                    href={source.sourceUrl}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    Open source document
                                </a>
                            )}

                            <button
                                type="button"
                                className="delete-button"
                                disabled={busy}
                                onClick={() => handleDelete(source.id)}
                            >
                                Remove
                            </button>
                        </li>
                    ))}
                </ul>
            )}

            {error && <p className="error-message">{error}</p>}
            {success && <p className="success-message">{success}</p>}

            <form className="source-form" onSubmit={handleSubmit}>

                <div className="form-group">
                    <label>Manifesto or document name</label>
                    <input
                        type="text"
                        name="manifestoName"
                        value={form.manifestoName}
                        onChange={handleChange}
                        placeholder="Example: Party manifesto 2024"
                        maxLength={200}
                        required
                    />
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label>Source URL</label>
                        <input
                            type="url"
                            name="sourceUrl"
                            value={form.sourceUrl}
                            onChange={handleChange}
                            placeholder="https://..."
                        />
                    </div>

                    <div className="form-group">
                        <label>Page number</label>
                        <input
                            type="number"
                            name="pageNumber"
                            min="1"
                            value={form.pageNumber}
                            onChange={handleChange}
                        />
                    </div>
                </div>

                <div className="form-row">
                    <div className="form-group">
                        <label>Source type</label>
                        <select
                            name="sourceType"
                            value={form.sourceType}
                            onChange={handleChange}
                        >
                            {SOURCE_TYPES.map(type => (
                                <option key={type} value={type}>
                                    {type.replaceAll("_", " ")}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Publication date</label>
                        <input
                            type="date"
                            name="publishedDate"
                            value={form.publishedDate}
                            onChange={handleChange}
                        />
                    </div>
                </div>

                <div className="form-group">
                    <label>Exact original quotation</label>
                    <textarea
                        name="exactQuotation"
                        value={form.exactQuotation}
                        onChange={handleChange}
                        rows="3"
                        placeholder="Copy the exact wording from the document"
                        required
                    />
                </div>

                <button
                    type="submit"
                    className="secondary-button"
                    disabled={busy}
                >
                    {busy ? "Saving..." : "Add original source"}
                </button>

            </form>

        </details>
    );
}

export default PromiseSourceManager;
