import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/users`;

export async function getUsers() {

    const response = await apiFetch(API_URL);

    return handleResponse(response);
}

export async function updateUser(id, changes) {

    const response = await apiFetch(`${API_URL}/${id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(changes)
    });

    return handleResponse(response);
}
