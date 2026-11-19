
import { useState } from "react";

function EvidenceReviewForm({ onReview, busy = false }) {

    const [documentReview, setDocumentReview] = useState("ACCEPTED");
    const [sourceReview, setSourceReview] = useState("VERIFIED");
    const [claimReview, setClaimReview] = useState("SUPPORTED");
    const [reviewComment, setReviewComment] = useState("");
    const [verificationNotes, setVerificationNotes] = useState("");
    const [error, setError] = useState("");

    async function handleSubmit(event) {

        event.preventDefault();

        if (documentReview === "REJECTED" &&
                !reviewComment.trim()) {
            setError("Please explain why the document is rejected.");
            return;
        }

        setError("");

        await onReview({
            documentReview,
            sourceReview,
            claimReview,
            reviewComment,
            verificationNotes
        });
    }

    return (
        <form className="evidence-review-form" onSubmit={handleSubmit}>

            <div className="review-dimensions">

                <label>
                    Document review
                    <select
                        value={documentReview}
                        onChange={event =>
                            setDocumentReview(event.target.value)}
                    >
                        <option value="ACCEPTED">
                            Accept for publication
                        </option>
                        <option value="REJECTED">Reject document</option>
                    </select>
                </label>

                <label>
                    Source review
                    <select
                        value={sourceReview}
                        onChange={event =>
                            setSourceReview(event.target.value)}
                    >
                        <option value="VERIFIED">
                            Traceable to issuer
                        </option>
                        <option value="DISPUTED">
                            Source disputed
                        </option>
                    </select>
                </label>

                <label>
                    Claim review
                    <select
                        value={claimReview}
                        onChange={event =>
                            setClaimReview(event.target.value)}
                    >
                        <option value="SUPPORTED">
                            Supports the claim
                        </option>
                        <option value="PARTIAL">
                            Partially supports
                        </option>
                        <option value="NOT_SUPPORTED">
                            Does not support
                        </option>
                    </select>
                </label>

            </div>

            <textarea
                rows="2"
                value={reviewComment}
                onChange={event =>
                    setReviewComment(event.target.value)}
                placeholder="Review comment (required for rejection)"
            />

            <textarea
                rows="2"
                value={verificationNotes}
                onChange={event =>
                    setVerificationNotes(event.target.value)}
                placeholder="Verification notes: what exactly does this document prove?"
            />

            {error && <p className="evidence-error">{error}</p>}

            <button
                type="submit"
                className="approve-button"
                disabled={busy}
            >
                {busy ? "Saving review..." : "Record review"}
            </button>

        </form>
    );
}

export default EvidenceReviewForm;
