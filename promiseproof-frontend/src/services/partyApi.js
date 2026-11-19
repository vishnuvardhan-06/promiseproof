import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/parties`;

export async function getParties() {

    const response = await apiFetch(API_URL);

    return handleResponse(response);
}

export async function createParty(party) {

    const response = await apiFetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(party)
    });

    return handleResponse(response);
}

export async function updateParty(id, party) {

    const response = await apiFetch(`${API_URL}/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(party)
    });

    return handleResponse(response);
}

export async function deleteParty(id) {

    const response = await apiFetch(`${API_URL}/${id}`, {
        method: "DELETE"
    });

    return handleResponse(response);
}