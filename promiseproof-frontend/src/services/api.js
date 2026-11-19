import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = API_BASE;

// Get all promises
export async function getPromises() {

    const response = await apiFetch(`${API_URL}/promises`);

    return handleResponse(response);
}

// Search, filter and paginate promises.
export async function searchPromises({
    q = "",
    status = "",
    partyId = "",
    electionId = "",
    state = "",
    withApprovedEvidence = false,
    page = 0,
    size = 9
} = {}) {

    const params = new URLSearchParams();

    if (q.trim()) params.set("q", q.trim());
    if (status) params.set("status", status);
    if (partyId) params.set("partyId", partyId);
    if (electionId) params.set("electionId", electionId);
    if (state.trim()) params.set("state", state.trim());
    if (withApprovedEvidence) params.set("withApprovedEvidence", "true");

    params.set("page", String(page));
    params.set("size", String(size));

    const response = await apiFetch(
        `${API_URL}/promises/search?${params.toString()}`
    );

    return handleResponse(response);
}

/**
 * Public citizen listing. The backend enforces publication visibility, so
 * drafts and unpublished promises can never be returned here.
 */
export async function getPublicPromises({
    q = "",
    status = "",
    partyId = "",
    electionId = "",
    state = "",
    category = "",
    withApprovedEvidence = false,
    sort = "recent",
    page = 0,
    size = 9
} = {}) {

    const params = new URLSearchParams();

    if (q.trim()) params.set("q", q.trim());
    if (status) params.set("status", status);
    if (partyId) params.set("partyId", partyId);
    if (electionId) params.set("electionId", electionId);
    if (state.trim()) params.set("state", state.trim());
    if (category.trim()) params.set("category", category.trim());
    if (withApprovedEvidence) params.set("withApprovedEvidence", "true");
    params.set("sort", sort);
    params.set("page", String(page));
    params.set("size", String(size));

    const response = await apiFetch(
        `${API_URL}/promises/public?${params.toString()}`
    );

    return handleResponse(response);
}

// Public detail of a published promise (creator identity omitted by backend).
export async function getPromise(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}`);

    return handleResponse(response);
}

// Public, independently reviewed status history for a published promise.
export async function getPromiseHistory(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}/history`);

    return handleResponse(response);
}

// Promises created by the signed-in user.
export async function getMyPromises() {

    const response = await apiFetch(`${API_URL}/promises/mine`);

    return handleResponse(response);
}

// Ask the server to suggest promises from pasted manifesto text.
export async function extractPromises(text) {

    const response = await apiFetch(`${API_URL}/promises/extract`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ text })
    });

    return handleResponse(response);
}

// Create a promise
export async function createPromise(promise) {

    const response = await apiFetch(`${API_URL}/promises`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(promise)
    });

    return handleResponse(response);
}

// Update a promise
export async function updatePromise(id, promise) {

    const response = await apiFetch(`${API_URL}/promises/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(promise)
    });

    return handleResponse(response);
}

// Delete a promise
export async function deletePromise(id) {

    const response = await apiFetch(`${API_URL}/promises/${id}`, {
        method: "DELETE"
    });

    return handleResponse(response);
}

// Independent publication workflow -------------------------------

// Reviewer/administrator publication queue.
export async function getPublicationQueue() {

    const response = await apiFetch(
        `${API_URL}/promises/publication-queue`
    );

    return handleResponse(response);
}

// A researcher submits a draft for independent publication review.
export async function submitForPublication(id) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/submit-for-publication`,
        { method: "POST" }
    );

    return handleResponse(response);
}

// Reviewer/administrator publication decision.
export async function decidePublication(id, decision, comment) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/publication`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ decision, comment })
        }
    );

    return handleResponse(response);
}

// Original source tracking ---------------------------------------

export async function getPromiseSources(id) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources`
    );

    return handleResponse(response);
}

export async function createPromiseSource(id, source) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(source)
        }
    );

    return handleResponse(response);
}

export async function updatePromiseSource(id, sourceId, source) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources/${sourceId}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(source)
        }
    );

    return handleResponse(response);
}

export async function deletePromiseSource(id, sourceId) {

    const response = await apiFetch(
        `${API_URL}/promises/${id}/sources/${sourceId}`,
        { method: "DELETE" }
    );

    return handleResponse(response);
}