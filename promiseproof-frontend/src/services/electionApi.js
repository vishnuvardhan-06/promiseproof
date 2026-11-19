
import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/elections`;

export async function getElections() {
    const response = await apiFetch(API_URL);
    return handleResponse(response);
}

export async function createElection(election) {
    const response = await apiFetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(election)
    });

    return handleResponse(response);
}

export async function updateElection(id, election) {
    const response = await apiFetch(`${API_URL}/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(election)
    });

    return handleResponse(response);
}

export async function deleteElection(id) {
    const response = await apiFetch(`${API_URL}/${id}`, {
        method: "DELETE"
    });

    return handleResponse(response);
}