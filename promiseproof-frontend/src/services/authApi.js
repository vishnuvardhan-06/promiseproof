import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/auth`;

export async function login(email, password) {

    const response = await apiFetch(`${API_URL}/login`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ email, password })
    });

    return handleResponse(response);
}

export async function register({ name, email, password }) {

    const response = await apiFetch(`${API_URL}/register`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ name, email, password })
    });

    return handleResponse(response);
}

export async function logout() {

    const response = await apiFetch(`${API_URL}/logout`, {
        method: "POST"
    });

    return handleResponse(response);
}

export async function getMe() {

    const response = await apiFetch(`${API_URL}/me`);

    return handleResponse(response);
}
