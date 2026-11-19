import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/notifications`;

export async function getNotifications() {

    const response = await apiFetch(API_URL);

    return handleResponse(response);
}

export async function markNotificationRead(id) {

    const response = await apiFetch(`${API_URL}/${id}/read`, {
        method: "POST"
    });

    return handleResponse(response);
}

export async function markAllNotificationsRead() {

    const response = await apiFetch(`${API_URL}/read-all`, {
        method: "POST"
    });

    return handleResponse(response);
}
