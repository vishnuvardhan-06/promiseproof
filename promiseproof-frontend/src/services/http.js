// Shared HTTP helpers for the PromiseProof API.
// Local development falls back to the Spring Boot port. Production builds can
// point this at a same-origin reverse proxy such as "/api".
export const API_BASE = (
    import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api"
).replace(/\/+$/, "");

const CSRF_HEADER = "X-XSRF-TOKEN";
const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

let csrfToken = null;

async function loadCsrfToken(force = false) {

    if (!force && csrfToken) {
        return csrfToken;
    }

    // Ask the server for a token. The token returned by this endpoint is the
    // masked value the server expects back in X-XSRF-TOKEN; the raw cookie
    // value must not be echoed directly.
    const response = await fetch(`${API_BASE}/auth/csrf`, {
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error(
            "Could not start a secure session. Please refresh the page."
        );
    }

    const data = await response.json();

    csrfToken = data.token;

    return csrfToken;
}

function withCsrf(options, token) {

    return {
        ...options,
        headers: {
            ...(options.headers || {}),
            [CSRF_HEADER]: token
        }
    };
}

// Fetch wrapper that always sends the session cookie and attaches the CSRF
// header to state-changing requests.
export async function apiFetch(url, options = {}) {

    const method = (options.method || "GET").toUpperCase();
    const request = {
        credentials: "include",
        ...options
    };

    if (!UNSAFE_METHODS.has(method)) {
        return fetch(url, request);
    }

    let token = await loadCsrfToken();

    let response = await fetch(url, withCsrf(request, token));

    if (response.status === 403) {

        // The token can go stale after a session change; retry once with a
        // freshly issued token before surfacing the error to the user.
        token = await loadCsrfToken(true);

        response = await fetch(url, withCsrf(request, token));
    }

    return response;
}

// Turn a fetch Response into JSON, or throw a readable Error.
export async function handleResponse(response) {

    if (!response.ok) {

        const text = await response.text();

        let message = text;

        try {
            const data = JSON.parse(text);
            message = data.message || data.error || text;
        } catch {
            // Response was not JSON; keep the plain text.
        }

        throw new Error(
            message || `Request failed with status ${response.status}`
        );
    }

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();

    return text ? JSON.parse(text) : null;
}
