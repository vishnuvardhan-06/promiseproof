
import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/evidence`;

// Upload evidence using multipart/form-data.
export async function uploadEvidence(evidenceData) {

    const formData = new FormData();

    formData.append("promiseId", evidenceData.promiseId);
    formData.append("title", evidenceData.title);
    formData.append("evidenceType", evidenceData.evidenceType);
    formData.append("description", evidenceData.description);
    formData.append("sourceUrl", evidenceData.sourceUrl);

    if (evidenceData.issuingDepartment) {
        formData.append(
            "issuingDepartment",
            evidenceData.issuingDepartment
        );
    }

    if (evidenceData.documentReference) {
        formData.append(
            "documentReference",
            evidenceData.documentReference
        );
    }

    if (evidenceData.documentDate) {
        formData.append("documentDate", evidenceData.documentDate);
    }

    if (evidenceData.claimScope) {
        formData.append("claimScope", evidenceData.claimScope);
    }

    formData.append("file", evidenceData.file);

    const response = await apiFetch(`${API_URL}`, {
        method: "POST",
        body: formData
    });

    return handleResponse(response);
}

// Get evidence for a particular Promise.
export async function getEvidenceForPromise(promiseId) {

    const response = await apiFetch(
        `${API_URL}/promise/${promiseId}`
    );

    return handleResponse(response);
}

// Public view: only evidence a reviewer has approved.
export async function getApprovedEvidenceForPromise(promiseId) {

    const response = await apiFetch(
        `${API_URL}/promise/${promiseId}/approved`
    );

    return handleResponse(response);
}

// Evidence submitted by the signed-in user.
export async function getMyEvidence() {

    const response = await apiFetch(`${API_URL}/mine`);

    return handleResponse(response);
}

// Review queue across every promise (reviewers and administrators).
export async function getPendingEvidence() {

    const response = await apiFetch(`${API_URL}/pending`);

    return handleResponse(response);
}

// Download evidence.
export async function downloadEvidence(id, originalFileName) {

    const response = await apiFetch(
        `${API_URL}/${id}/download`
    );

    if (!response.ok) {
        throw new Error("Could not download evidence");
    }

    const blob = await response.blob();

    const url = window.URL.createObjectURL(blob);

    const link = document.createElement("a");

    link.href = url;
    link.download = originalFileName;

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.URL.revokeObjectURL(url);
}

// Audit history for one evidence document (reviewers and admins).
export async function getEvidenceAudit(id) {

    const response = await apiFetch(
        `${API_URL}/${id}/audit`
    );

    return handleResponse(response);
}

// Three independent review decisions. Accepts either an object:
//   { documentReview, sourceReview, claimReview, reviewComment,
//     verificationNotes }
// or the legacy (status, comment) arguments.
export async function reviewEvidence(id, review, legacyComment) {

    const payload = typeof review === "string"
        ? {
            status: review,
            reviewComment: legacyComment
        }
        : review;

    const response = await apiFetch(
        `${API_URL}/${id}/review`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        }
    );

    return handleResponse(response);
}

// Return a rejected document to the review queue.
export async function resubmitEvidence(id) {

    const response = await apiFetch(
        `${API_URL}/${id}/resubmit`,
        { method: "POST" }
    );

    return handleResponse(response);
}
