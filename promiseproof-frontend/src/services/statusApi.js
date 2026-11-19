import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/status-proposals`;

// A researcher proposes a new promise status backed by approved evidence.
export async function proposeStatus(promiseId, payload) {

    const response = await apiFetch(`${API_URL}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            promiseId,
            proposedStatus: payload.proposedStatus,
            reason: payload.reason,
            evidenceId: payload.evidenceId
        })
    });

    return handleResponse(response);
}

// Pending status verification proposals (reviewers and administrators).
export async function getPendingStatusProposals() {

    const response = await apiFetch(`${API_URL}/pending`);

    return handleResponse(response);
}

// Approve or reject a status verification proposal.
export async function decideStatusProposal(id, decision, comment) {

    const response = await apiFetch(`${API_URL}/${id}/decision`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            decision,
            comment
        })
    });

    return handleResponse(response);
}
