import { API_BASE, apiFetch, handleResponse } from "./http";

const API_URL = `${API_BASE}/stats`;

// Public dashboard statistics.
export async function getStatsOverview() {

    const response = await apiFetch(`${API_URL}/overview`);

    return handleResponse(response);
}

// Public dashboard statistics (published promises only).
export async function getPublicStats() {

    const response = await apiFetch(`${API_URL}/public`);

    return handleResponse(response);
}

// Platform-wide evidence audit trail (administrators only).
export async function getRecentAudit(limit = 50) {

    const response = await apiFetch(
        `${API_BASE}/audit/evidence/recent?limit=${limit}`
    );

    return handleResponse(response);
}
