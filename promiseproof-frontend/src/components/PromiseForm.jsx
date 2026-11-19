
import { useState } from "react";

const initialForm = {
    title: "",
    description: "",
    partyId: "",
    electionId: "",
    state: "",
    category: "",
    target: "",
    targetDate: "",
    geographicScope: ""
};

function PromiseForm({
    onSave,
    editingPromise,
    onCancel,
    parties,
    elections
}) {

    const [formData, setFormData] = useState(
        () =>
            editingPromise
                ? {
                    title: editingPromise.title || "",
                    description: editingPromise.description || "",
                    partyId: editingPromise.partyId
                        ? String(editingPromise.partyId)
                        : "",
                    electionId: editingPromise.electionId
                        ? String(editingPromise.electionId)
                        : "",
                    state: editingPromise.state || "",
                    category: editingPromise.category || "",
                    target: editingPromise.target || "",
                    targetDate: editingPromise.targetDate || "",
                    geographicScope:
                        editingPromise.geographicScope || ""
                }
                : initialForm
    );

    function handleChange(event) {

        const { name, value } = event.target;

        setFormData(previous => ({
            ...previous,
            [name]: value
        }));
    }

    async function handleSubmit(event) {

        event.preventDefault();

        const requestData = {
            ...formData,
            partyId: formData.partyId
                ? Number(formData.partyId)
                : null,
            electionId: formData.electionId
                ? Number(formData.electionId)
                : null
        };

        const saved = await onSave(requestData);

        if (saved) {
            setFormData(initialForm);
        }
    }

    return (

        <form className="promise-form" onSubmit={handleSubmit}>

            <h2>
                {editingPromise ? "Edit Promise" : "Add New Promise"}
            </h2>

            <div className="form-group">

                <label>Promise Title</label>

                <input
                    type="text"
                    name="title"
                    value={formData.title}
                    onChange={handleChange}
                    placeholder="Enter promise title"
                    maxLength={200}
                    required
                />

            </div>

            <div className="form-group">

                <label>Description</label>

                <textarea
                    name="description"
                    value={formData.description}
                    onChange={handleChange}
                    placeholder="Describe the promise"
                    rows="4"
                />

            </div>

            <div className="form-row">

                <div className="form-group">

                    <label>Category</label>

                    <input
                        type="text"
                        name="category"
                        value={formData.category}
                        onChange={handleChange}
                        placeholder="Example: Employment"
                        maxLength={80}
                    />

                </div>

                <div className="form-group">

                    <label>Target</label>

                    <input
                        type="text"
                        name="target"
                        value={formData.target}
                        onChange={handleChange}
                        placeholder="Example: 10,000 jobs"
                        maxLength={200}
                    />

                </div>

            </div>

            <div className="form-row">

                <div className="form-group">

                    <label>Target Date</label>

                    <input
                        type="date"
                        name="targetDate"
                        value={formData.targetDate}
                        onChange={handleChange}
                    />

                </div>

                <div className="form-group">

                    <label>Geographic Scope</label>

                    <input
                        type="text"
                        name="geographicScope"
                        value={formData.geographicScope}
                        onChange={handleChange}
                        placeholder="Example: Andhra Pradesh"
                        maxLength={120}
                    />

                </div>

            </div>

            <div className="form-row">

                <div className="form-group">

                    <label>Political Party</label>

                    <select
                        name="partyId"
                        value={formData.partyId}
                        onChange={handleChange}
                    >

                        <option value="">Select a party (optional)</option>

                        {parties.map(party => (
                            <option key={party.id} value={party.id}>
                                {party.name} ({party.abbreviation})
                            </option>
                        ))}

                    </select>

                    {parties.length === 0 && (
                        <small>
                            No parties available. Add a party below first.
                        </small>
                    )}

                </div>

                <div className="form-group">

                    <label>Election</label>

                    <select
                        name="electionId"
                        value={formData.electionId}
                        onChange={handleChange}
                    >

                        <option value="">Select an election (optional)</option>

                        {elections.map(election => (
                            <option key={election.id} value={election.id}>
                                {election.name} - {election.state} ({election.year})
                            </option>
                        ))}

                    </select>

                    {elections.length === 0 && (
                        <small>
                            No elections available. Add an election below first.
                        </small>
                    )}

                </div>

            </div>

            <div className="form-row">

                <div className="form-group">

                    <label>State</label>

                    <input
                        type="text"
                        name="state"
                        value={formData.state}
                        onChange={handleChange}
                        placeholder="Enter state"
                    />

                </div>

            </div>

            <p className="panel-note">
                Implementation status is not edited here. New promises start
                as <strong>Not yet assessed</strong>, and status changes only
                become public after an approved, evidence-backed status
                verification proposal.
            </p>

            <div className="form-actions">

                <button type="submit" className="primary-button">
                    {editingPromise ? "Update Promise" : "Save Promise"}
                </button>

                {editingPromise && (

                    <button
                        type="button"
                        className="cancel-button"
                        onClick={onCancel}
                    >
                        Cancel
                    </button>

                )}

            </div>

        </form>
    );
}

export default PromiseForm;