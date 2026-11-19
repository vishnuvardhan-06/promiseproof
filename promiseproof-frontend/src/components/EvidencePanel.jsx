
import { useCallback, useEffect, useState } from "react";

import {
    uploadEvidence,
    getEvidenceForPromise,
    getApprovedEvidenceForPromise,
    downloadEvidence,
    reviewEvidence,
    getEvidenceAudit
} from "../services/evidenceApi";

import EvidenceReviewForm from "./EvidenceReviewForm";

import "../styles/evidence.css";

function EvidencePanel({
    promise,
    canSubmit = false,
    canReview = false
}) {

    const [evidenceList, setEvidenceList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [auditFor, setAuditFor] = useState(null);
    const [auditItems, setAuditItems] = useState([]);
    const [auditLoading, setAuditLoading] = useState(false);
    const [auditError, setAuditError] = useState("");

    const [formData, setFormData] = useState({
        title: "",
        evidenceType: "GOVERNMENT_ORDER",
        description: "",
        sourceUrl: "",
        issuingDepartment: "",
        documentReference: "",
        documentDate: "",
        claimScope: "",
        file: null
    });

    const loadEvidence = useCallback(async () => {

        try {
            setLoading(true);
            setError("");

            // Reviewers and admins see the full queue (including pending
            // and rejected items). Everyone else sees approved evidence only.
            const data = canReview
                ? await getEvidenceForPromise(promise.id)
                : await getApprovedEvidenceForPromise(promise.id);

            setEvidenceList(data);

        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, [promise.id, canReview]);

    useEffect(() => {

        let cancelled = false;

        async function refresh() {

            await loadEvidence();

            if (cancelled) {
                return;
            }
        }

        refresh();

        return () => {
            cancelled = true;
        };
    }, [loadEvidence]);

    function handleChange(event) {

        const { name, value } = event.target;

        setFormData(previous => ({
            ...previous,
            [name]: value
        }));
    }

    function handleFileChange(event) {

        const file = event.target.files[0];

        setFormData(previous => ({
            ...previous,
            file
        }));
    }

    async function handleSubmit(event) {

        event.preventDefault();

        if (!formData.file) {
            setError("Please select an evidence file.");
            return;
        }

        try {

            setUploading(true);
            setError("");
            setSuccess("");

            await uploadEvidence({
                ...formData,
                promiseId: promise.id
            });

            setSuccess("Evidence uploaded successfully. Waiting for review.");

            setFormData({
                title: "",
                evidenceType: "GOVERNMENT_ORDER",
                description: "",
                sourceUrl: "",
                issuingDepartment: "",
                documentReference: "",
                documentDate: "",
                claimScope: "",
                file: null
            });

            event.target.reset();

            await loadEvidence();

        } catch (err) {
            setError(err.message);
        } finally {
            setUploading(false);
        }
    }

    async function handleDownload(item) {

        try {
            setError("");

            await downloadEvidence(
                item.id,
                item.originalFileName
            );

        } catch (err) {
            setError(err.message);
        }
    }

    async function handleReview(item, review) {

        try {

            setError("");
            setSuccess("");

            await reviewEvidence(item.id, review);

            setSuccess(
                "Review recorded. Document, source and claim decisions are " +
                "stored separately."
            );

            await loadEvidence();

        } catch (err) {
            setError(err.message);
        }
    }

    function formatFileSize(bytes) {

        if (!bytes) return "0 KB";

        return `${(bytes / 1024).toFixed(1)} KB`;
    }

    function formatDateTime(value) {

        if (!value) return "";

        return new Date(value).toLocaleString();
    }

    async function toggleAudit(item) {

        if (auditFor === item.id) {
            setAuditFor(null);
            return;
        }

        setAuditFor(item.id);
        setAuditItems([]);
        setAuditError("");

        try {
            setAuditLoading(true);
            setAuditItems(await getEvidenceAudit(item.id));
        } catch (err) {
            setAuditError(err.message);
        } finally {
            setAuditLoading(false);
        }
    }

    return (

        <div className="evidence-panel">

            <div className="evidence-heading">
                <h4>Evidence Management</h4>
                <span>{evidenceList.length} documents</span>
            </div>

            {canSubmit ? (

            <form
                className="evidence-form"
                onSubmit={handleSubmit}
            >

                <h5>Submit New Evidence</h5>

                <div className="evidence-form-group">

                    <label>Document Title</label>

                    <input
                        type="text"
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        placeholder="Example: Government Order No. 123"
                        maxLength={200}
                        required
                    />

                </div>

                <div className="evidence-form-group">

                    <label>Evidence Type</label>

                    <select
                        name="evidenceType"
                        value={formData.evidenceType}
                        onChange={handleChange}
                        required
                    >
                        <option value="GOVERNMENT_ORDER">
                            Government Order
                        </option>

                        <option value="BUDGET_DOCUMENT">
                            Budget Document
                        </option>

                        <option value="PROGRESS_REPORT">
                            Progress Report
                        </option>

                        <option value="COMPLETION_CERTIFICATE">
                            Completion Certificate
                        </option>

                        <option value="OFFICIAL_REPORT">
                            Official Report
                        </option>

                        <option value="OTHER">
                            Other
                        </option>
                    </select>

                </div>

                <div className="evidence-form-group">

                    <label>Description</label>

                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        placeholder="Explain what this document proves"
                        rows="3"
                    />

                </div>

                <div className="evidence-form-group">

                    <label>Official Source URL (optional)</label>

                    <input
                        type="url"
                        name="sourceUrl"
                        value={formData.sourceUrl}
                        onChange={handleChange}
                        placeholder="https://official-government-website.gov.in"
                    />

                </div>

                <div className="evidence-form-group">

                    <label>Issuing department</label>

                    <input
                        type="text"
                        name="issuingDepartment"
                        value={formData.issuingDepartment}
                        onChange={handleChange}
                        placeholder="Example: Department of Rural Development"
                        maxLength={200}
                    />

                </div>

                <div className="form-row">

                    <div className="evidence-form-group">

                        <label>Document reference number</label>

                        <input
                            type="text"
                            name="documentReference"
                            value={formData.documentReference}
                            onChange={handleChange}
                            placeholder="Example: G.O. Ms. No. 123"
                            maxLength={120}
                        />

                    </div>

                    <div className="evidence-form-group">

                        <label>Document publication date</label>

                        <input
                            type="date"
                            name="documentDate"
                            value={formData.documentDate}
                            onChange={handleChange}
                        />

                    </div>

                </div>

                <div className="evidence-form-group">

                    <label>What part of the promise does it support?</label>

                    <textarea
                        name="claimScope"
                        value={formData.claimScope}
                        onChange={handleChange}
                        rows="2"
                        placeholder="Example: Confirms budget allocation only, not completion"
                    />

                </div>

                <div className="evidence-form-group">

                    <label>Upload Document</label>

                    <input
                        type="file"
                        accept=".pdf,.jpg,.jpeg,.png"
                        onChange={handleFileChange}
                        required
                    />

                    <small>
                        Attach a clear copy of the official document. Maximum 10 MB.
                    </small>

                    {formData.file && (
                        <small>
                            Selected: {formData.file.name}
                        </small>
                    )}

                </div>

                <button
                    type="submit"
                    className="evidence-upload-button"
                    disabled={uploading}
                >
                    {uploading ? "Uploading..." : "Upload Evidence"}
                </button>

            </form>

            ) : (

                <p className="evidence-note">
                    Sign in as a citizen, researcher, reviewer or
                    administrator to submit supporting evidence.
                </p>

            )}

            {error && (
                <p className="evidence-error">{error}</p>
            )}

            {success && (
                <p className="evidence-success">{success}</p>
            )}

            <div className="evidence-list">

                <h5>Submitted Documents</h5>

                {loading ? (

                    <p>Loading evidence...</p>

                ) : evidenceList.length === 0 ? (

                    <p className="evidence-empty">
                        {canReview
                            ? "No evidence submitted for this promise yet."
                            : "No approved evidence has been published for this promise yet."}
                    </p>

                ) : (

                    evidenceList.map(item => (

                        <div
                            className="evidence-item"
                            key={item.id}
                        >

                            <div className="evidence-item-top">

                                <div>
                                    <strong>{item.title}</strong>

                                    <p>{item.evidenceType.replaceAll("_", " ")}</p>
                                </div>

                                <span className={`evidence-status ${item.status.toLowerCase()}`}>
                                    {item.status}
                                </span>

                            </div>

                            {item.description && (
                                <p>{item.description}</p>
                            )}

                            <dl className="evidence-meta">
                                {item.issuingDepartment && (
                                    <div>
                                        <dt>Issuing department</dt>
                                        <dd>{item.issuingDepartment}</dd>
                                    </div>
                                )}

                                {item.documentReference && (
                                    <div>
                                        <dt>Document reference</dt>
                                        <dd>{item.documentReference}</dd>
                                    </div>
                                )}

                                {item.documentDate && (
                                    <div>
                                        <dt>Document date</dt>
                                        <dd>{item.documentDate}</dd>
                                    </div>
                                )}

                                {item.claimScope && (
                                    <div>
                                        <dt>Claim scope</dt>
                                        <dd>{item.claimScope}</dd>
                                    </div>
                                )}

                                {item.sha256Hash && (
                                    <div>
                                        <dt>SHA-256</dt>
                                        <dd className="hash-value">
                                            {item.sha256Hash}
                                        </dd>
                                    </div>
                                )}
                            </dl>

                            {(item.documentReviewStatus ||
                                item.sourceReviewStatus ||
                                item.claimReviewStatus) && (
                                <p className="review-dimension-summary">
                                    <span>
                                        Document: {item.documentReviewStatus}
                                    </span>
                                    <span>
                                        Source: {item.sourceReviewStatus}
                                    </span>
                                    <span>
                                        Claim: {item.claimReviewStatus}
                                    </span>
                                </p>
                            )}

                            <p className="evidence-file-name">
                                {item.originalFileName}
                                {" • "}
                                {formatFileSize(item.fileSize)}
                            </p>

                            {item.sourceUrl && (
                                <a
                                    href={item.sourceUrl}
                                    target="_blank"
                                    rel="noreferrer"
                                >
                                    View official source
                                </a>
                            )}

                            {item.reviewComment && (
                                <p className="evidence-review-comment">
                                    Review comment: {item.reviewComment}
                                </p>
                            )}

                            {item.verificationNotes && (
                                <p className="evidence-verification-notes">
                                    Verification notes: {item.verificationNotes}
                                </p>
                            )}

                            <div className="evidence-actions">

                                <button
                                    type="button"
                                    onClick={() => handleDownload(item)}
                                >
                                    Download
                                </button>

                                {canReview && item.status === "PENDING" && (
                                    <EvidenceReviewForm
                                        onReview={review =>
                                            handleReview(item, review)}
                                    />
                                )}

                                {canReview && (
                                    <button
                                        type="button"
                                        className="history-button"
                                        onClick={() => toggleAudit(item)}
                                    >
                                        {auditFor === item.id
                                            ? "Hide history"
                                            : "History"}
                                    </button>
                                )}

                            </div>

                            {item.reviewedAt && (
                                <p className="evidence-reviewed-at">
                                    Reviewed: {formatDateTime(item.reviewedAt)}
                                </p>
                            )}

                            {canReview && auditFor === item.id && (
                                <div className="audit-timeline">

                                    <h6>Audit history</h6>

                                    {auditLoading ? (
                                        <p>Loading history...</p>
                                    ) : auditError ? (
                                        <p className="evidence-error">
                                            {auditError}
                                        </p>
                                    ) : auditItems.length === 0 ? (
                                        <p className="evidence-empty">
                                            No audit entries recorded.
                                        </p>
                                    ) : (
                                        <ol className="audit-list">
                                            {auditItems.map(entry => (
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
                                                        {entry.actorRole
                                                            ? ` • ${entry.actorRole}`
                                                            : ""}
                                                    </p>

                                                    {entry.fromStatus && (
                                                        <p className="audit-transition">
                                                            {entry.fromStatus} → {entry.toStatus}
                                                        </p>
                                                    )}

                                                    {entry.comment && (
                                                        <p className="audit-comment">
                                                            {entry.comment}
                                                        </p>
                                                    )}
                                                </li>
                                            ))}
                                        </ol>
                                    )}

                                </div>
                            )}

                        </div>

                    ))

                )}

            </div>

        </div>
    );
}

export default EvidencePanel;
